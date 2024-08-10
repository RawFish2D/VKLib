package ua.rawfish2d.vklib.init.data;

import lombok.Getter;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;
import ua.rawfish2d.vklib.utils.VkHelper;

import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;

@Getter
public class VkQueueInfo {
	private final VkDevice vkLogicalDevice;
	private final VkQueue queue;
	private final VkQueueIndexInfo indexInfo;
	private long vkCommandPool = 0;

	public VkQueueInfo(VkDevice vkLogicalDevice, VkQueue queue, VkQueueIndexInfo queueInfo) {
		this.vkLogicalDevice = vkLogicalDevice;
		this.queue = queue;
		this.indexInfo = queueInfo;
	}

	public void createCommandPool() {
		this.vkCommandPool = VkHelper.createCommandPool(vkLogicalDevice, indexInfo.queueIndex());
	}

	public void destroyCommandPool() {
		vkDestroyCommandPool(vkLogicalDevice, vkCommandPool, null);
		vkCommandPool = 0;
	}
}
