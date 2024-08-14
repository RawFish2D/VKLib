package ua.rawfish2d.vklib;

import lombok.Getter;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ua.rawfish2d.vklib.init.VkDeviceInstance;
import ua.rawfish2d.vklib.init.data.VkQueueInfo;
import ua.rawfish2d.vklib.init.enums.TextureFiltering;
import ua.rawfish2d.vklib.init.enums.TextureWrap;
import ua.rawfish2d.vklib.utils.VkHelper;
import ua.rawfish2d.vklib.utils.VkTranslate;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Locale;

import static org.lwjgl.vulkan.VK10.*;

public class VkTexture {
	private int width = 0;
	private int height = 0;
	private int vkFormat = VK_FORMAT_R8G8B8A8_UNORM; // VK_FORMAT_R8G8B8A8_SRGB makes texture appear darker for some reason
	private long vkTextureImage;
	private long vkTextureImageMemory;
	@Getter
	private long vkTextureImageView;
	@Getter
	private long vkTextureSampler;

	public void loadAndCreateImage(final String filename, VkDeviceInstance vkDeviceInstance) {
		final VkDevice vkLogicalDevice = vkDeviceInstance.getVkLogicalDevice();
		final VkPhysicalDevice vkPhysicalDevice = vkDeviceInstance.getVkPhysicalDevice();
		final VkQueueInfo queueInfo = vkDeviceInstance.getVkGraphicsQueue();
		final VkQueue vkGraphicsQueue = queueInfo.getQueue();
		long vkCommandPool = queueInfo.getVkCommandPool();
		final VkBuffer vkStagingBuffer = new VkBuffer();
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer pWidth = stack.mallocInt(1);
			final IntBuffer pHeight = stack.mallocInt(1);
			final IntBuffer pChannels = stack.mallocInt(1);
			final ByteBuffer image = STBImage.stbi_load(filename, pWidth, pHeight, pChannels, STBImage.STBI_rgb_alpha);
			if (image == null) {
				throw new RuntimeException("Failed to load image!");
			}
			width = pWidth.get(0);
			height = pHeight.get(0);

			final int imageSize = width * height * 4;

			// staging buffer
			System.out.printf("Creating texture buffer:\n");
			vkStagingBuffer.createBuffer(vkDeviceInstance, imageSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
			vkStagingBuffer.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			final ByteBuffer byteBuffer = vkStagingBuffer.getBuffer(vkDeviceInstance);
			byteBuffer.put(image);

			image.position(0);
			STBImage.stbi_image_free(image);

			final LongBuffer pTextureImage = stack.mallocLong(1);
			final LongBuffer pTextureImageMemory = stack.mallocLong(1);
			VkHelper.createImage(vkLogicalDevice, vkPhysicalDevice, vkFormat, width, height, pTextureImage, pTextureImageMemory);
			vkTextureImage = pTextureImage.get(0);
			vkTextureImageMemory = pTextureImageMemory.get(0);
		}

		// TODO make it work with existing command buffer
		boolean shouldDestroy = false;
		if (vkCommandPool == 0) {
			shouldDestroy = true;
			vkCommandPool = VkHelper.createCommandPool(vkLogicalDevice, 0);
		}
		VkHelper.transitionImageLayout(vkLogicalDevice, vkCommandPool, vkGraphicsQueue,
				vkTextureImage, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		VkHelper.copyBufferToImage(vkLogicalDevice, vkCommandPool, vkGraphicsQueue,
				vkStagingBuffer, vkTextureImage, width, height);
		VkHelper.transitionImageLayout(vkLogicalDevice, vkCommandPool, vkGraphicsQueue,
				vkTextureImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
		if (shouldDestroy) {
			vkDestroyCommandPool(vkLogicalDevice, vkCommandPool, null);
		}

		vkStagingBuffer.destroyAndFreeMemory(vkDeviceInstance);

		vkTextureImageView = VkHelper.createTextureImageView(vkLogicalDevice, vkTextureImage, vkFormat);

		vkTextureSampler = createSampler(vkPhysicalDevice, vkLogicalDevice);
	}

	private long createSampler(VkPhysicalDevice vkPhysicalDevice, VkDevice vkLogicalDevice) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkPhysicalDeviceProperties vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.malloc(stack);
			vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties);

			final float maxAnisotropyLevel = vkPhysicalDeviceProperties.limits().maxSamplerAnisotropy();
			System.out.printf(Locale.US, "🔷 max anisotropy: %.1f\n", maxAnisotropyLevel);

			final VkSamplerCreateInfo vkSamplerCreateInfo = VkHelper.makeSamplerCreateInfo(
					stack,
					vkPhysicalDeviceProperties.limits().maxSamplerAnisotropy(),
					TextureFiltering.NEAREST,
					TextureFiltering.NEAREST,
					TextureFiltering.MIPMAP_LINEAR,
					TextureWrap.REPEAT,
					TextureWrap.REPEAT);

			final LongBuffer pTextureSampler = stack.mallocLong(1);
			final int result = vkCreateSampler(vkLogicalDevice, vkSamplerCreateInfo, null, pTextureSampler);
			if (result != VK_SUCCESS) {
				throw new RuntimeException("Failed to create texture sampler! Error: " + VkTranslate.translateVulkanResult(result));
			}
			return pTextureSampler.get(0);
		}
	}

	public void destroy(VkDeviceInstance vkDeviceInstance) {
		final VkDevice vkLogicalDevice = vkDeviceInstance.getVkLogicalDevice();
		vkDestroySampler(vkLogicalDevice, vkTextureSampler, null);
		vkDestroyImageView(vkLogicalDevice, vkTextureImageView, null);
		vkDestroyImage(vkLogicalDevice, vkTextureImage, null);
		vkFreeMemory(vkLogicalDevice, vkTextureImageMemory, null);
	}
}
