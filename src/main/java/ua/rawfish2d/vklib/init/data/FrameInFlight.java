package ua.rawfish2d.vklib.init.data;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import ua.rawfish2d.vklib.init.VkDeviceInstance;
import ua.rawfish2d.vklib.utils.VkHelper;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

@Getter
public class FrameInFlight {
	private final long vkImageAvailableSemaphore;
	private final long vkRenderFinishedSemaphore;
	private final long vkInFlightFence;
	private VkCommandBuffer vkCommandBuffer;
	private long vkCommandPool;

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

	public void free(VkDevice vkLogicalDevice) {
		vkDestroySemaphore(vkLogicalDevice, vkImageAvailableSemaphore, null);
		vkDestroySemaphore(vkLogicalDevice, vkRenderFinishedSemaphore, null);
		vkDestroyFence(vkLogicalDevice, vkInFlightFence, null);
		vkFreeCommandBuffers(vkLogicalDevice, vkCommandPool, vkCommandBuffer);
	}
}
