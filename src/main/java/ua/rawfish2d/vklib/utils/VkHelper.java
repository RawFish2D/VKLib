package ua.rawfish2d.vklib.utils;

import lombok.NonNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;
import org.lwjgl.vulkan.*;
import ua.rawfish2d.vklib.VkBuffer;
import ua.rawfish2d.vklib.init.enums.TextureFiltering;
import ua.rawfish2d.vklib.init.enums.TextureWrap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdBeginRenderingKHR;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdEndRenderingKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSynchronization2.vkCmdPipelineBarrier2KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_ACCESS_NONE;

public class VkHelper {

	public static int findMemoryType(VkPhysicalDevice vkPhysicalDevice, int typeBits, int properties, MemoryStack stack) {
		final VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
		vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, memProperties);

		System.out.printf("🔷 Searching for memory:" +
				"\n\tbits: %d %s" +
				"\n\ttype: %d %s\n", typeBits, VkTranslate.vkTranslateMemoryProperty(typeBits), properties, VkTranslate.vkTranslateMemoryProperty(properties));
		int memoryIndex = 0;
		int flags = 0;
		for (int index = 0; index < memProperties.memoryTypeCount(); index++) {
			final VkMemoryType memoryType = memProperties.memoryTypes().get(index);
			final int propertyFlags = memoryType.propertyFlags();

			if ((typeBits & 1) == 1) {
				if ((propertyFlags & properties) == properties) {
					memoryIndex = memoryType.heapIndex();
					flags = propertyFlags;
					break;
				}
			}
//			if ((typeBits & (1 << index)) >= 1 && (memoryPropertyFlags & properties) == properties) {
//				memoryIndex = memoryType.heapIndex();
//				flags = memoryPropertyFlags;
//			}
			typeBits = typeBits >> 1;
		}
		System.out.printf("❇️ Chose memory type index: %d with flags: %d %s\n", memoryIndex, flags, VkTranslate.vkTranslateMemoryProperty(flags));
		return memoryIndex;
//		throw new RuntimeException("Failed to find suitable memory type!");
	}

	public static void printMemoryTypes(VkPhysicalDevice vkPhysicalDevice) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
			vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, memProperties);

			System.out.printf("🔷 Memory types:\n");
			for (int index = 0; index < memProperties.memoryTypeCount(); index++) {
				final VkMemoryType memoryType = memProperties.memoryTypes().get(index);
				final int propertyFlags = memoryType.propertyFlags();

				System.out.printf("\t[%d] index: %d flags: %d %s\n", index, memoryType.heapIndex(), propertyFlags, VkTranslate.vkTranslateMemoryProperty(propertyFlags));
			}

			System.out.printf("🔷 Memory heaps:\n");
			for (int index = 0; index < memProperties.memoryHeapCount(); index++) {
				final VkMemoryHeap memoryHeap = memProperties.memoryHeaps().get(index);
				final int flags = memoryHeap.flags();

				System.out.printf("\t[%d] size: %d | flags: %d %s\n", index, memoryHeap.size(), flags, VkTranslate.vkTranslateMemoryHeapFlags(flags));
			}
		}
	}

	public static long createTextureImageView(VkDevice vkLogicalDevice, long vkTextureImage, int vkFormat) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkImageViewCreateInfo vkImageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
					.sType$Default()
					.image(vkTextureImage)
					.viewType(VK_IMAGE_VIEW_TYPE_2D)
					.format(vkFormat);
			vkImageViewCreateInfo.subresourceRange()
					.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
					.baseMipLevel(0)
					.levelCount(1)
					.baseArrayLayer(0)
					.layerCount(1);

			final LongBuffer pTextureImageView = stack.mallocLong(1);
			final int result = vkCreateImageView(vkLogicalDevice, vkImageViewCreateInfo, null, pTextureImageView);
			if (result != VK_SUCCESS) {
				throw new RuntimeException("Failed to create texture image view! Error: " + VkTranslate.translateVulkanResult(result));
			}
			return pTextureImageView.get(0);
		}
	}

	public static VkClearValue vkGetClearValue(MemoryStack stack, float r, float g, float b, float a) {
		final VkClearValue clearColor = VkClearValue.calloc(stack);
		clearColor.color().float32()
				.put(0, r)
				.put(1, g)
				.put(2, b)
				.put(3, a);
		return clearColor;
	}

	public static long createCommandPool(VkDevice vkLogicalDevice, int queueIndex) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
					.sType$Default()
					.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
					.queueFamilyIndex(queueIndex);

			final LongBuffer pCommandPool = stack.mallocLong(1);
			if (vkCreateCommandPool(vkLogicalDevice, poolInfo, null, pCommandPool) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create command pool!");
			}
			return pCommandPool.get(0);
		}
	}

	public static VkCommandBuffer createCommandBuffer(VkDevice vkLogicalDevice, long vkCommandPool) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
					.sType$Default()
					.commandPool(vkCommandPool)
					.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
					.commandBufferCount(1);

			final PointerBuffer pCommandBuffer = stack.mallocPointer(1);
			if (vkAllocateCommandBuffers(vkLogicalDevice, allocInfo, pCommandBuffer) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocate command buffers!");
			}
			return new VkCommandBuffer(pCommandBuffer.get(0), vkLogicalDevice);
		}
	}

	public static void beginCommandBuffer(VkCommandBuffer commandBuffer) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
					.sType$Default()
					.flags(0) // VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT
					.pInheritanceInfo(null);

			vkBeginCommandBuffer(commandBuffer, beginInfo);
		}
	}

	public static void endCommandBuffer(VkCommandBuffer commandBuffer) {
		vkEndCommandBuffer(commandBuffer);
	}

	public static void beginRendering(MemoryStack stack, VkCommandBuffer commandBuffer, long vkSwapChainImage, long vkSwapChainImageView, VkClearValue clearValue, int extentWidth, int extentHeight) {
		final VkImageMemoryBarrier.Buffer vkImageMemoryBarriers = VkImageMemoryBarrier.calloc(1, stack);
		final VkImageMemoryBarrier vkImageMemoryBarrier = VkImageMemoryBarrier.calloc(stack)
				.sType$Default()
				.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED) // new
				.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED) // new
				.srcAccessMask(VK_ACCESS_NONE)
				.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
				.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
				.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
				.image(vkSwapChainImage);
		vkImageMemoryBarrier.subresourceRange()
				.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
				.baseMipLevel(0)
				.levelCount(1)
				.baseArrayLayer(0)
				.layerCount(1);
		vkImageMemoryBarriers.put(0, vkImageMemoryBarrier);

		vkCmdPipelineBarrier(
				commandBuffer,
				VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, // srcStageMask
				VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, // dstStageMask
				0,
				null,
				null,
				vkImageMemoryBarriers);

		final VkRenderingAttachmentInfoKHR.Buffer vkRenderingAttachmentInfoBuffer = VkRenderingAttachmentInfoKHR.calloc(1, stack);
		vkRenderingAttachmentInfoBuffer.put(0, VkRenderingAttachmentInfoKHR.calloc(stack)
				.sType$Default()
				.imageView(vkSwapChainImageView)
				.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
				.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR) // clear framebuffer before using it
				.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
				.clearValue(clearValue));

		final VkRenderingInfoKHR vkRenderingInfo = VkRenderingInfoKHR.calloc(stack)
				.sType$Default()
				.layerCount(1)
				.pColorAttachments(vkRenderingAttachmentInfoBuffer);
		vkRenderingInfo.renderArea().offset().set(0, 0);
		vkRenderingInfo.renderArea().extent().width(extentWidth).height(extentHeight);

		vkCmdBeginRenderingKHR(commandBuffer, vkRenderingInfo);
	}

	public static void endRendering(MemoryStack stack, VkCommandBuffer commandBuffer, long vkSwapChainImage) {
		vkCmdEndRenderingKHR(commandBuffer);

		final VkImageMemoryBarrier.Buffer vkImageMemoryBarriers = VkImageMemoryBarrier.calloc(1, stack);
		final VkImageMemoryBarrier vkImageMemoryBarrier = VkImageMemoryBarrier.calloc()
				.sType$Default()
				.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED) // new
				.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED) // new
				.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
				.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
				.newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
				.image(vkSwapChainImage);
		vkImageMemoryBarrier.subresourceRange()
				.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
				.baseMipLevel(0)
				.levelCount(1)
				.baseArrayLayer(0)
				.layerCount(1);
		vkImageMemoryBarriers.put(0, vkImageMemoryBarrier);

		vkCmdPipelineBarrier(
				commandBuffer,
				VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, // srcStageMask
				VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, // dstStageMask
				0,
				null,
				null,
				vkImageMemoryBarriers);
	}

	public static void setViewport(MemoryStack stack, VkCommandBuffer commandBuffer, int width, int height) {
		final VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
		viewport.x(0.0f).y(0.0f);
		viewport.width(width).height(height);
		viewport.minDepth(0f).maxDepth(1f);
		vkCmdSetViewport(commandBuffer, 0, viewport);
	}

	public static void setScissor(MemoryStack stack, VkCommandBuffer commandBuffer, int width, int height) {
		final VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
		scissor.offset().set(0, 0);
		scissor.extent().width(width).height(height);
		vkCmdSetScissor(commandBuffer, 0, scissor);
	}

	public static ByteBuffer glslToSpirv(String classPath, int vulkanStage) throws IOException {
		// ByteBuffer src = IOUtils.ioResourceToByteBuffer(classPath, 1024);
		ByteBuffer src = IOUtils.ioFileToByteBuffer(classPath, 4096);
		long compiler = shaderc_compiler_initialize();
		long options = shaderc_compile_options_initialize();
		ShadercIncludeResolve resolver;
		ShadercIncludeResultRelease releaser;
		shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_3);
		shaderc_compile_options_set_target_spirv(options, shaderc_spirv_version_1_3);
		shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);
		shaderc_compile_options_set_include_callbacks(options, resolver = new ShadercIncludeResolve() {
			public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
				ShadercIncludeResult res = ShadercIncludeResult.calloc();
				try {
					String src = classPath.substring(0, classPath.lastIndexOf('/')) + "/" + memUTF8(requested_source);
					res.content(IOUtils.ioResourceToByteBuffer(src, 1024));
					res.source_name(memUTF8(src));
					return res.address();
				} catch (IOException e) {
					throw new AssertionError("Failed to resolve include: " + src);
				}
			}
		}, releaser = new ShadercIncludeResultRelease() {
			public void invoke(long user_data, long include_result) {
				ShadercIncludeResult result = ShadercIncludeResult.create(include_result);
				MemoryUtil.memFree(result.source_name());
				result.free();
			}
		}, 0L);
		long res;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			res = shaderc_compile_into_spv(compiler, src, VkTranslate.vulkanStageToShadercKind(vulkanStage), stack.UTF8(classPath), stack.UTF8("main"), options);
			if (res == 0L)
				throw new AssertionError("Internal error during compilation!");
		}
		if (shaderc_result_get_compilation_status(res) != shaderc_compilation_status_success) {
			throw new AssertionError("Shader compilation failed: " + shaderc_result_get_error_message(res));
		}
		int size = (int) shaderc_result_get_length(res);
		ByteBuffer resultBytes = createByteBuffer(size);
		resultBytes.put(shaderc_result_get_bytes(res));
		resultBytes.flip();
		shaderc_result_release(res);
		shaderc_compiler_release(compiler);
		releaser.free();
		resolver.free();
		return resultBytes;
	}

	public static void createImage(VkDevice vkLogicalDevice, VkPhysicalDevice vkPhysicalDevice, int format, int width, int height, @NonNull LongBuffer out_vkTextureImage, @NonNull LongBuffer out_vkTextureImageMemory) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
					.sType$Default()
					.imageType(VK_IMAGE_TYPE_2D)
					.mipLevels(1)
					.arrayLayers(1)
					.format(format)
					.tiling(VK_IMAGE_TILING_OPTIMAL)
					.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
					.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
					.sharingMode(VK_SHARING_MODE_EXCLUSIVE) // image will only be used by one queue family
					.samples(VK_SAMPLE_COUNT_1_BIT)
					.flags(0);

			imageInfo.extent().width(width);
			imageInfo.extent().height(height);
			imageInfo.extent().depth(1);

			System.out.printf("Creating image with format: %s\n", VkTranslate.translateSurfaceFormatBit(format));
			final LongBuffer pImage = stack.mallocLong(1);
			final int result = vkCreateImage(vkLogicalDevice, imageInfo, null, pImage);
			if (result != VK_SUCCESS) {
				throw new RuntimeException("Failed to create an image! Error: " + VkTranslate.translateVulkanResult(result));
			}
			out_vkTextureImage.put(0, pImage.get(0));
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
			vkGetImageMemoryRequirements(vkLogicalDevice, out_vkTextureImage.get(0), memRequirements);

			final int memoryType = findMemoryType(vkPhysicalDevice, memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, stack);
			final VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
					.sType$Default()
					.allocationSize(memRequirements.size())
					.memoryTypeIndex(memoryType);

			System.out.printf("🔷 Allocating %d bytes (%s) of memory\n", memRequirements.size(), humanReadableByteCountBin(memRequirements.size()));
			final LongBuffer pTextureImageMemory = stack.mallocLong(1);
			final int result = vkAllocateMemory(vkLogicalDevice, allocInfo, null, pTextureImageMemory);
			if (result != VK_SUCCESS) {
				throw new RuntimeException("failed to allocate image memory! Error: " + VkTranslate.translateVulkanResult(result));
			}
			out_vkTextureImageMemory.put(0, pTextureImageMemory.get(0));
			vkBindImageMemory(vkLogicalDevice, out_vkTextureImage.get(0), out_vkTextureImageMemory.get(0), 0);
		}
	}

	public static String humanReadableByteCountBin(long bytes) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			return bytes + " B";
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		return String.format(Locale.US, "%.1f %ciB", value / 1024.0, ci.current());
	}

	public static VkCommandBuffer beginSingleTimeCommands(VkDevice vkLogicalDevice, long vkCommandPool) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
					.sType$Default()
					.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
					.commandPool(vkCommandPool)
					.commandBufferCount(1);

			final PointerBuffer pCommandBuffer = stack.mallocPointer(1);
			vkAllocateCommandBuffers(vkLogicalDevice, allocInfo, pCommandBuffer);
			final VkCommandBuffer vkCommandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), vkLogicalDevice);

			final VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
					.sType$Default()
					.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

			vkBeginCommandBuffer(vkCommandBuffer, beginInfo);

			return vkCommandBuffer;
		}
	}

	public static void endSingleTimeCommands(VkDevice vkLogicalDevice, long vkCommandPool, VkCommandBuffer commandBuffer, VkQueue graphicsQueue) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			vkEndCommandBuffer(commandBuffer);
			final PointerBuffer pCommandBuffers = stack.pointers(commandBuffer);
			final VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
					.sType$Default()
					.pCommandBuffers(pCommandBuffers);

			vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
			vkQueueWaitIdle(graphicsQueue);
			vkFreeCommandBuffers(vkLogicalDevice, vkCommandPool, commandBuffer);
		}
	}

	public static void transitionImageLayout(VkDevice vkLogicalDevice, long vkCommandPool, VkQueue graphicsQueue, long vkImage, int vkOldLayout, int vkNewLayout) {
		final VkCommandBuffer commandBuffer = beginSingleTimeCommands(vkLogicalDevice, vkCommandPool);

		try (MemoryStack stack = MemoryStack.stackPush()) {

			int sourceStage;
			int destinationStage;
			int srcAccessMask;
			int dstAccessMask;

			if (vkOldLayout == VK_IMAGE_LAYOUT_UNDEFINED && vkNewLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
				srcAccessMask = 0;
				dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;

				sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
			} else if (vkOldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && vkNewLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
				srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
				dstAccessMask = VK_ACCESS_SHADER_READ_BIT;

				sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
				destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
			} else {
				throw new RuntimeException("Unsupported layout transition!");
			}

			final VkImageMemoryBarrier.Buffer vkImageMemoryBarrier = VkImageMemoryBarrier.calloc(1, stack)
					.sType$Default()
					// VK_IMAGE_LAYOUT_UNDEFINED if you don't care about existing data in texture
					.oldLayout(vkOldLayout)
					.newLayout(vkNewLayout)
					// should be actual indexes if you transition queue ownership, but not this time
					.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
					.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
					.image(vkImage)
					.srcAccessMask(srcAccessMask)
					.dstAccessMask(dstAccessMask);
			vkImageMemoryBarrier.subresourceRange()
					.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
					.baseMipLevel(0)
					.levelCount(1)
					.baseArrayLayer(0)
					.layerCount(1);

			vkCmdPipelineBarrier(
					commandBuffer,
					// specifies in which pipeline stage the operations occur that should happen before the barrier
					sourceStage,
					// specifies the pipeline stage in which operations will wait on the barrier
					destinationStage,
					// 0 or VK_DEPENDENCY_BY_REGION_BIT
					0,
					null,
					null,
					vkImageMemoryBarrier);
		}

		endSingleTimeCommands(vkLogicalDevice, vkCommandPool, commandBuffer, graphicsQueue);
	}

	public static void copyBufferToImage(VkDevice vkLogicalDevice, long vkCommandPool, VkQueue graphicsQueue, VkBuffer buffer, long image, int width, int height) {
		final VkCommandBuffer commandBuffer = beginSingleTimeCommands(vkLogicalDevice, vkCommandPool);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkBufferImageCopy.Buffer vkBufferImageCopy = VkBufferImageCopy.calloc(1, stack)
					.bufferOffset(0)
					.bufferRowLength(0)
					.bufferImageHeight(0);
			vkBufferImageCopy.imageOffset().set(0, 0, 0);
			vkBufferImageCopy.imageExtent().set(width, height, 1);
			vkBufferImageCopy.imageSubresource()
					.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
					.mipLevel(0)
					.baseArrayLayer(0)
					.layerCount(1);

			vkCmdCopyBufferToImage(
					commandBuffer,
					buffer.getHandle(),
					image,
					VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
					vkBufferImageCopy);
		}

		endSingleTimeCommands(vkLogicalDevice, vkCommandPool, commandBuffer, graphicsQueue);
	}

	public static VkSamplerCreateInfo makeSamplerCreateInfo(MemoryStack stack, float maxAnisotropy, TextureFiltering minFilter, TextureFiltering magFilter, TextureFiltering mipmapFilter, TextureWrap textureWrapU, TextureWrap textureWrapV) {
		final boolean anisotropyEnabled = maxAnisotropy != 0f;
		return VkSamplerCreateInfo.calloc(stack)
				.sType$Default()
				.minFilter(minFilter.get())
				.magFilter(magFilter.get())
				.addressModeU(textureWrapU.get())
				.addressModeV(textureWrapV.get())
				.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
				.anisotropyEnable(anisotropyEnabled)
				.maxAnisotropy(maxAnisotropy)
				.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
				.unnormalizedCoordinates(false)
				// stuff for shadow maps
				.compareEnable(false)
				.compareOp(VK_COMPARE_OP_ALWAYS)
				//
				.mipmapMode(mipmapFilter.get())
				.mipLodBias(0f)
				.minLod(0f)
				.maxLod(0f);
	}

//	public static void bufferMemoryBarrier1(MemoryStack stack, VkCommandBuffer commandBuffer, long vkBuffer, long bufferSize, int srcAccess, int srcStage, int dstAccess, int dstStage) {
//		final VkBufferMemoryBarrier.Buffer bufferMemoryBarriers = VkBufferMemoryBarrier.calloc(1, stack).sType$Default();
//		final VkBufferMemoryBarrier vkBufferMemoryBarrier = VkBufferMemoryBarrier.calloc(stack)
//				.sType$Default()
//				.pNext(0)
//				.srcAccessMask(srcAccess)
//				.dstAccessMask(dstAccess)
//				.buffer(vkBuffer)
//				.offset(0)
//				.size(bufferSize);
//		bufferMemoryBarriers.put(0, vkBufferMemoryBarrier);
//
//		vkCmdPipelineBarrier(commandBuffer,
//				srcStage,
//				dstStage,
//				0,
//				null,
//				bufferMemoryBarriers,
//				null);
//	}

	public static void bufferMemoryBarrier2(VkCommandBuffer commandBuffer, long vkBuffer, long bufferSize, int srcAccess, int srcStage, int dstAccess, int dstStage) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkBufferMemoryBarrier2KHR.Buffer bufferMemoryBarriers = VkBufferMemoryBarrier2KHR.calloc(1, stack).sType$Default();
			final VkBufferMemoryBarrier2KHR vkBufferMemoryBarrier = VkBufferMemoryBarrier2KHR.calloc(stack)
					.sType$Default()
					.pNext(0)
					.srcAccessMask(srcAccess)
					.srcStageMask(srcStage)
					.dstAccessMask(dstAccess)
					.dstStageMask(dstStage)
					.buffer(vkBuffer)
					.offset(0)
					.size(bufferSize)
					.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
					.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			bufferMemoryBarriers.put(0, vkBufferMemoryBarrier);

			final VkDependencyInfoKHR dependencyInfo = VkDependencyInfoKHR.calloc(stack)
					.sType$Default()
					.dependencyFlags(0)
					.pMemoryBarriers(null)
					.pBufferMemoryBarriers(bufferMemoryBarriers)
					.pImageMemoryBarriers(null);

			vkCmdPipelineBarrier2KHR(commandBuffer, dependencyInfo);
		}
	}

	public static PointerBuffer stringsToPointerBuffer(MemoryStack stack, List<String> list) {
		final int pointerBufferSize = list.size();
//		if (pointerBufferSize == 0) {
//			return null;
//		}
		final PointerBuffer pointerBuffer = stack.mallocPointer(pointerBufferSize);
		IntStream.range(0, pointerBufferSize).forEach(index -> {
			final String str = list.get(index);
			pointerBuffer.put(index, stack.UTF8(str));
		});
		System.out.printf("size: %d | capacity: %d | remaining: %d\n", pointerBufferSize, pointerBuffer.capacity(), pointerBuffer.remaining());
		return pointerBuffer;
	}
}
