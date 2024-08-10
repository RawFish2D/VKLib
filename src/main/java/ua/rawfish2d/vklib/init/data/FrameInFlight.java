package ua.rawfish2d.vklib.init.data;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ua.rawfish2d.vklib.VkBuffer;
import ua.rawfish2d.vklib.init.VkDeviceInstance;
import ua.rawfish2d.vklib.init.descriptor.DescriptorSetUpdate;
import ua.rawfish2d.vklib.utils.VkHelper;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

@Getter
public class FrameInFlight {
	private final long vkImageAvailableSemaphore;
	private final long vkRenderFinishedSemaphore;
	private final long vkInFlightFence;
	private VkCommandBuffer vkCommandBuffer;
	private long vkCommandPool;
	private VkBuffer uniformBuffer;
	private long vkDescriptorSet;

	public void setDescriptorSet(long vkDescriptorSet) {
		this.vkDescriptorSet = vkDescriptorSet;
	}

	public FrameInFlight(VkDevice vkLogicalDevice) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
					.sType$Default();

			final VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack)
					.sType$Default()
					.flags(VK_FENCE_CREATE_SIGNALED_BIT); // this is important
			// without VK_FENCE_CREATE_SIGNALED_BIT flag we will wait for finish drawing previous frame
			// but because this is the first frame, it will just freeze forever, this flag fixes it

			final LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
			final LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
			final LongBuffer pInFlightFence = stack.mallocLong(1);
			if (vkCreateSemaphore(vkLogicalDevice, semaphoreCreateInfo, null, pImageAvailableSemaphore) != VK_SUCCESS ||
					vkCreateSemaphore(vkLogicalDevice, semaphoreCreateInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS ||
					vkCreateFence(vkLogicalDevice, fenceCreateInfo, null, pInFlightFence) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create semaphores!");
			}
			vkImageAvailableSemaphore = pImageAvailableSemaphore.get(0);
			vkRenderFinishedSemaphore = pRenderFinishedSemaphore.get(0);
			vkInFlightFence = pInFlightFence.get(0);
		}
	}

	public void createCommandBuffer(VkDeviceInstance vkDeviceInstance) {
		final VkQueueInfo vkQueueInfo = vkDeviceInstance.getVkGraphicsQueue();
		this.vkCommandPool = vkQueueInfo.getVkCommandPool();
		this.vkCommandBuffer = VkHelper.createCommandBuffer(vkDeviceInstance.getVkLogicalDevice(), vkCommandPool);
	}

	public void free(VkDeviceInstance vkDeviceInstancee) {
		final VkDevice vkLogicalDevice = vkDeviceInstancee.getVkLogicalDevice();
		uniformBuffer.destroyAndFreeMemory(vkDeviceInstancee);
		vkDestroySemaphore(vkLogicalDevice, vkImageAvailableSemaphore, null);
		vkDestroySemaphore(vkLogicalDevice, vkRenderFinishedSemaphore, null);
		vkDestroyFence(vkLogicalDevice, vkInFlightFence, null);
		vkFreeCommandBuffers(vkLogicalDevice, vkCommandPool, vkCommandBuffer);
	}

	public void createUniformBuffer(VkDeviceInstance vkDeviceInstance, int bufferSize) {
		this.uniformBuffer = new VkBuffer();
		System.out.printf("Creating new Uniform Buffer size: %d\n", bufferSize);
		uniformBuffer.createBuffer(vkDeviceInstance, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
		uniformBuffer.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		uniformBuffer.makeStagingBuffer(vkDeviceInstance, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
	}

	public void updateDescriptorSet(VkDeviceInstance vkDeviceInstance, List<DescriptorSetUpdate> descriptorSetUpdates) {
		final VkWriteDescriptorSet.Buffer descriptorWrite = VkDeviceInstance.makeWriteDescriptorSet(vkDescriptorSet, descriptorSetUpdates);
		vkUpdateDescriptorSets(vkDeviceInstance.getVkLogicalDevice(), descriptorWrite, null);
	}
}
