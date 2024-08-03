package ua.rawfish2d.vklib.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_BT709_NONLINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdBeginRenderingKHR;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdEndRenderingKHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;
import static org.lwjgl.vulkan.VK13.VK_ACCESS_NONE;

public class VkHelper {
	/**
	 * Translates a Vulkan {@code VkResult} value to a String describing the result.
	 *
	 * @param result the {@code VkResult} value
	 * @return the result description
	 */
	public static String translateVulkanResult(int result) {
		return switch (result) {
			// Success codes
			case VK_SUCCESS -> "Command successfully completed.";
			case VK_NOT_READY -> "A fence or query has not yet completed.";
			case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
			case VK_EVENT_SET -> "An event is signaled.";
			case VK_EVENT_RESET -> "An event is unsignaled.";
			case VK_INCOMPLETE -> "A return array was too small for the result.";
			case VK_SUBOPTIMAL_KHR ->
					"A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

			// Error codes
			case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
			case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
			case VK_ERROR_INITIALIZATION_FAILED ->
					"Initialization of an object could not be completed for implementation-specific reasons.";
			case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
			case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
			case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
			case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
			case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
			case VK_ERROR_INCOMPATIBLE_DRIVER ->
					"The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
			case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
			case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
			case VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL";
			case VK_ERROR_UNKNOWN -> "Unknown error.";
			case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
			case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR ->
					"The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
			case VK_ERROR_OUT_OF_DATE_KHR ->
					"A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
							+ "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue" + "presenting to the surface.";
			case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR ->
					"The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an" + " image.";
			case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
			case VK_ERROR_OUT_OF_POOL_MEMORY -> "VK_ERROR_OUT_OF_POOL_MEMORY";
			default -> String.format("%s [%d]", "Unknown", result);
		};
	}

	public static String translateQueueBit(int bits) {
		final List<String> stringList = new ArrayList<>();
		if ((bits & VK_QUEUE_GRAPHICS_BIT) == 1) {
			stringList.add("VK_QUEUE_GRAPHICS_BIT");
		}
		if ((bits & VK_QUEUE_COMPUTE_BIT) == 2) {
			stringList.add("VK_QUEUE_COMPUTE_BIT");
		}
		if ((bits & VK_QUEUE_TRANSFER_BIT) == 4) {
			stringList.add("VK_QUEUE_TRANSFER_BIT");
		}
		if ((bits & VK_QUEUE_SPARSE_BINDING_BIT) == 8) {
			stringList.add("VK_QUEUE_SPARSE_BINDING_BIT");
		}
		if ((bits & VK_QUEUE_FAMILY_IGNORED) == -1) {
			stringList.add("VK_QUEUE_FAMILY_IGNORED");
		}
		if ((bits & VK_QUEUE_FAMILY_EXTERNAL) == -2) {
			stringList.add("VK_QUEUE_FAMILY_EXTERNAL");
		}
		final StringBuilder stringBuilder = new StringBuilder();
		for (int a = 0; a < stringList.size(); ++a) {
			stringBuilder.append(stringList.get(a));
			if (a != stringList.size() - 1) {
				stringBuilder.append(", ");
			}
		}
		return stringBuilder.toString();
	}

	public static String translatePhysicalDeviceType(int deviceType) {
		return switch (deviceType) {
			case VK_PHYSICAL_DEVICE_TYPE_OTHER -> "VK_PHYSICAL_DEVICE_TYPE_OTHER";
			case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU";
			case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU";
			case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU";
			case VK_PHYSICAL_DEVICE_TYPE_CPU -> "VK_PHYSICAL_DEVICE_TYPE_CPU";
			default -> "UNKNOWN";
		};
	}

	public static String translateColorSpace(int bits) {
		return switch (bits) {
			case VK_COLOR_SPACE_SRGB_NONLINEAR_KHR -> "VK_COLOR_SPACE_SRGB_NONLINEAR_KHR";
			case VK_COLOR_SPACE_BT709_NONLINEAR_EXT -> "VK_COLOR_SPACE_BT709_NONLINEAR_EXT"; // EXT
			case VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT -> "VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT"; // EXT
			default -> "unknown";
		};
	}

	public static String translateSurfaceFormatBit(int bits) {
		return switch (bits) {
			case VK_FORMAT_R4G4_UNORM_PACK8 -> "VK_FORMAT_R4G4_UNORM_PACK8";
			case VK_FORMAT_R4G4B4A4_UNORM_PACK16 -> "VK_FORMAT_R4G4B4A4_UNORM_PACK16";
			case VK_FORMAT_B4G4R4A4_UNORM_PACK16 -> "VK_FORMAT_B4G4R4A4_UNORM_PACK16";
			case VK_FORMAT_R5G6B5_UNORM_PACK16 -> "VK_FORMAT_R5G6B5_UNORM_PACK16";
			case VK_FORMAT_B5G6R5_UNORM_PACK16 -> "VK_FORMAT_B5G6R5_UNORM_PACK16";
			case VK_FORMAT_R5G5B5A1_UNORM_PACK16 -> "VK_FORMAT_R5G5B5A1_UNORM_PACK16";
			case VK_FORMAT_B5G5R5A1_UNORM_PACK16 -> "VK_FORMAT_B5G5R5A1_UNORM_PACK16";
			case VK_FORMAT_A1R5G5B5_UNORM_PACK16 -> "VK_FORMAT_A1R5G5B5_UNORM_PACK16";

			case VK_FORMAT_R8_UNORM -> "VK_FORMAT_R8_UNORM";
			case VK_FORMAT_R8_SNORM -> "VK_FORMAT_R8_SNORM";
			case VK_FORMAT_R8_USCALED -> "VK_FORMAT_R8_USCALED";
			case VK_FORMAT_R8_SSCALED -> "VK_FORMAT_R8_SSCALED";
			case VK_FORMAT_R8_UINT -> "VK_FORMAT_R8_UINT";
			case VK_FORMAT_R8_SINT -> "VK_FORMAT_R8_SINT";
			case VK_FORMAT_R8_SRGB -> "VK_FORMAT_R8_SRGB";

			case VK_FORMAT_R8G8_UNORM -> "VK_FORMAT_R8G8_UNORM";
			case VK_FORMAT_R8G8_SNORM -> "VK_FORMAT_R8G8_SNORM";
			case VK_FORMAT_R8G8_USCALED -> "VK_FORMAT_R8G8_USCALED";
			case VK_FORMAT_R8G8_SSCALED -> "VK_FORMAT_R8G8_SSCALED";
			case VK_FORMAT_R8G8_UINT -> "VK_FORMAT_R8G8_UINT";
			case VK_FORMAT_R8G8_SINT -> "VK_FORMAT_R8G8_SINT";
			case VK_FORMAT_R8G8_SRGB -> "VK_FORMAT_R8G8_SRGB";

			case VK_FORMAT_R8G8B8_UNORM -> "VK_FORMAT_R8G8B8_UNORM";
			case VK_FORMAT_R8G8B8_SNORM -> "VK_FORMAT_R8G8B8_SNORM";
			case VK_FORMAT_R8G8B8_USCALED -> "VK_FORMAT_R8G8B8_USCALED";
			case VK_FORMAT_R8G8B8_SSCALED -> "VK_FORMAT_R8G8B8_SSCALED";
			case VK_FORMAT_R8G8B8_UINT -> "VK_FORMAT_R8G8B8_UINT";
			case VK_FORMAT_R8G8B8_SINT -> "VK_FORMAT_R8G8B8_SINT";
			case VK_FORMAT_R8G8B8_SRGB -> "VK_FORMAT_R8G8B8_SRGB";

			case VK_FORMAT_B8G8R8_UNORM -> "VK_FORMAT_B8G8R8_UNORM";
			case VK_FORMAT_B8G8R8_SNORM -> "VK_FORMAT_B8G8R8_SNORM";
			case VK_FORMAT_B8G8R8_USCALED -> "VK_FORMAT_B8G8R8_USCALED";
			case VK_FORMAT_B8G8R8_SSCALED -> "VK_FORMAT_B8G8R8_SSCALED";
			case VK_FORMAT_B8G8R8_UINT -> "VK_FORMAT_B8G8R8_UINT";
			case VK_FORMAT_B8G8R8_SINT -> "VK_FORMAT_B8G8R8_SINT";
			case VK_FORMAT_B8G8R8_SRGB -> "VK_FORMAT_B8G8R8_SRGB";

			case VK_FORMAT_R8G8B8A8_UNORM -> "VK_FORMAT_R8G8B8A8_UNORM";
			case VK_FORMAT_R8G8B8A8_SNORM -> "VK_FORMAT_R8G8B8A8_SNORM";
			case VK_FORMAT_R8G8B8A8_USCALED -> "VK_FORMAT_R8G8B8A8_USCALED";
			case VK_FORMAT_R8G8B8A8_SSCALED -> "VK_FORMAT_R8G8B8A8_SSCALED";
			case VK_FORMAT_R8G8B8A8_UINT -> "VK_FORMAT_R8G8B8A8_UINT";
			case VK_FORMAT_R8G8B8A8_SINT -> "VK_FORMAT_R8G8B8A8_SINT";
			case VK_FORMAT_R8G8B8A8_SRGB -> "VK_FORMAT_R8G8B8A8_SRGB";

			case VK_FORMAT_B8G8R8A8_UNORM -> "VK_FORMAT_B8G8R8A8_UNORM";
			case VK_FORMAT_B8G8R8A8_SNORM -> "VK_FORMAT_B8G8R8A8_SNORM";
			case VK_FORMAT_B8G8R8A8_USCALED -> "VK_FORMAT_B8G8R8A8_USCALED";
			case VK_FORMAT_B8G8R8A8_SSCALED -> "VK_FORMAT_B8G8R8A8_SSCALED";
			case VK_FORMAT_B8G8R8A8_UINT -> "VK_FORMAT_B8G8R8A8_UINT";
			case VK_FORMAT_B8G8R8A8_SINT -> "VK_FORMAT_B8G8R8A8_SINT";
			case VK_FORMAT_B8G8R8A8_SRGB -> "VK_FORMAT_B8G8R8A8_SRGB";

			case VK_FORMAT_A8B8G8R8_UNORM_PACK32 -> "VK_FORMAT_A8B8G8R8_UNORM_PACK32";
			case VK_FORMAT_A8B8G8R8_SNORM_PACK32 -> "VK_FORMAT_A8B8G8R8_SNORM_PACK32";
			case VK_FORMAT_A8B8G8R8_SRGB_PACK32 -> "VK_FORMAT_A8B8G8R8_SRGB_PACK32";
			case VK_FORMAT_A2R10G10B10_UNORM_PACK32 -> "VK_FORMAT_A2R10G10B10_UNORM_PACK32";
			case VK_FORMAT_A2R10G10B10_SNORM_PACK32 -> "VK_FORMAT_A2R10G10B10_SNORM_PACK32";
			case VK_FORMAT_A2B10G10R10_UNORM_PACK32 -> "VK_FORMAT_A2B10G10R10_UNORM_PACK32";
			case VK_FORMAT_A2B10G10R10_SNORM_PACK32 -> "VK_FORMAT_A2B10G10R10_SNORM_PACK32";

			case VK_FORMAT_R16_UNORM -> "VK_FORMAT_R16_UNORM";
			case VK_FORMAT_R16_SNORM -> "VK_FORMAT_R16_SNORM";
			case VK_FORMAT_R16_USCALED -> "VK_FORMAT_R16_USCALED";
			case VK_FORMAT_R16_SSCALED -> "VK_FORMAT_R16_SSCALED";
			case VK_FORMAT_R16_UINT -> "VK_FORMAT_R16_UINT";
			case VK_FORMAT_R16_SINT -> "VK_FORMAT_R16_SINT";
			case VK_FORMAT_R16_SFLOAT -> "VK_FORMAT_R16_SFLOAT";

			case VK_FORMAT_R16G16_UNORM -> "VK_FORMAT_R16G16_UNORM";
			case VK_FORMAT_R16G16_SFLOAT -> "VK_FORMAT_R16G16_SFLOAT";

			case VK_FORMAT_R16G16B16_UNORM -> "VK_FORMAT_R16G16B16_UNORM";
			case VK_FORMAT_R16G16B16_SFLOAT -> "VK_FORMAT_R16G16B16_SFLOAT";

			case VK_FORMAT_R16G16B16A16_UNORM -> "VK_FORMAT_R16G16B16A16_UNORM";
			case VK_FORMAT_R16G16B16A16_SNORM -> "VK_FORMAT_R16G16B16A16_SNORM";
			case VK_FORMAT_R16G16B16A16_SFLOAT -> "VK_FORMAT_R16G16B16A16_SFLOAT";

			case VK_FORMAT_B10G11R11_UFLOAT_PACK32 -> "VK_FORMAT_B10G11R11_UFLOAT_PACK32";

			case VK_FORMAT_R32_UINT -> "VK_FORMAT_R32_UINT"; // uint
			case VK_FORMAT_R32_SINT -> "VK_FORMAT_R32_SINT"; // int
			case VK_FORMAT_R32_SFLOAT -> "VK_FORMAT_R32_SFLOAT"; // float

			case VK_FORMAT_R32G32_UINT -> "VK_FORMAT_R32G32_UINT"; // uvec2
			case VK_FORMAT_R32G32_SINT -> "VK_FORMAT_R32G32_SINT"; // ivec2
			case VK_FORMAT_R32G32_SFLOAT -> "VK_FORMAT_R32G32_SFLOAT"; // vec2

			case VK_FORMAT_R32G32B32_UINT -> "VK_FORMAT_R32G32B32_UINT"; // uvec3
			case VK_FORMAT_R32G32B32_SINT -> "VK_FORMAT_R32G32B32_SINT"; // ivec3
			case VK_FORMAT_R32G32B32_SFLOAT -> "VK_FORMAT_R32G32B32_SFLOAT"; // vec3

			case VK_FORMAT_R32G32B32A32_UINT -> "VK_FORMAT_R32G32B32A32_UINT"; // uvec4
			case VK_FORMAT_R32G32B32A32_SINT -> "VK_FORMAT_R32G32B32A32_SINT"; // ivec4
			case VK_FORMAT_R32G32B32A32_SFLOAT -> "VK_FORMAT_R32G32B32A32_SFLOAT"; // vec4

			case VK_FORMAT_R64_UINT -> "VK_FORMAT_R64_UINT"; // unsigned double ?
			case VK_FORMAT_R64_SINT -> "VK_FORMAT_R64_UINT"; // singled double ?
			case VK_FORMAT_R64_SFLOAT -> "VK_FORMAT_R64_UINT"; // double ?

			default -> "VK_FORMAT_UNDEFINED";
		};
	}

	public static String vkTranslateMemoryProperty(int bits) {
		final List<String> stringList = new ArrayList<>();
		if ((bits & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) == VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) == VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) == VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_HOST_COHERENT_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) == VK_MEMORY_PROPERTY_HOST_CACHED_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_HOST_CACHED_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) == VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT");
		}
		final StringBuilder stringBuilder = new StringBuilder();
		for (int index = 0; index < stringList.size(); ++index) {
			stringBuilder.append(stringList.get(index));
			if (index != stringList.size() - 1) {
				stringBuilder.append(", ");
			}
		}
		return stringBuilder.toString();
	}

	public static int findMemoryType(VkPhysicalDevice vkPhysicalDevice, int typeFilter, int properties, MemoryStack stack) {
		final VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
		vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, memProperties);

//		for (int a = 0; a < memProperties.memoryTypeCount(); a++) {
//			final int memoryPropertyFlags = memProperties.memoryTypes().get(a).propertyFlags();
//			System.out.printf("Memory Property[%d]: %s\n", a, VKUtil.vkTranlateMemoryProperty(memoryPropertyFlags));
//		}

		for (int index = 0; index < memProperties.memoryTypeCount(); index++) {
			final int memoryPropertyFlags = memProperties.memoryTypes().get(index).propertyFlags();

			if ((typeFilter & (1 << index)) >= 1 && (memoryPropertyFlags & properties) == properties) {
				System.out.printf("Chose memory index %d with flags: %s\n", index, vkTranslateMemoryProperty(memoryPropertyFlags));
				return index;
			}
		}
		throw new RuntimeException("Failed to find suitable memory type!");
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
				throw new RuntimeException("Failed to create texture image view! Error: " + translateVulkanResult(result));
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

		final VkRenderingAttachmentInfo.Buffer vkRenderingAttachmentInfoBuffer = VkRenderingAttachmentInfoKHR.calloc(1, stack);
		vkRenderingAttachmentInfoBuffer.put(0, VkRenderingAttachmentInfo.calloc(stack)
				.sType$Default()
				.imageView(vkSwapChainImageView)
				.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
				.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR) // clear framebuffer before using it
				.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
				.clearValue(clearValue));

		final VkRenderingInfoKHR vkRenderingInfoKHR = VkRenderingInfoKHR.calloc(stack)
				.sType$Default()
				.layerCount(1)
				.pColorAttachments(vkRenderingAttachmentInfoBuffer);
		vkRenderingInfoKHR.renderArea().offset().set(0, 0);
		vkRenderingInfoKHR.renderArea().extent().width(extentWidth).height(extentHeight);

		vkCmdBeginRenderingKHR(commandBuffer, vkRenderingInfoKHR);
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
}
