package ua.rawfish2d.vklib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.lwjgl.vulkan.VkCommandBuffer;
import ua.rawfish2d.vklib.init.VkDeviceInstance;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

@RequiredArgsConstructor
public class IndirectBuffer {
	private final VkBuffer buffer;
	private final int commandsMax;
	private final boolean useStagingBuffer;
	@Getter
	private int commands = 0;
	private int index = 0;

	public static IndirectBuffer createIndirectBuffer(VkDeviceInstance vkDeviceInstance, int maxCommands, boolean stagingBuffer) {
		final int stride = 5 * Integer.BYTES;
		final int bufferSize = stride * maxCommands;
		System.out.printf("Indirect buffer size: %d\n", bufferSize);

		// create indirect buffer
		final VkBuffer vkBuffer = new VkBuffer();
		System.out.printf("Creating indirect buffer:\n");
		vkBuffer.createBuffer(vkDeviceInstance, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT);
		if (stagingBuffer) {
			vkBuffer.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			vkBuffer.makeStagingBuffer(vkDeviceInstance, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
		} else {
			vkBuffer.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT); // no staging buffer
		}
		return new IndirectBuffer(vkBuffer, maxCommands, stagingBuffer);
	}

	public void reset() {
		commands = 0;
		index = 0;
	}

	public void addIndirectCommand(VkDeviceInstance vkDeviceInstance, int indices, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
		if (commands >= commandsMax) {
			System.out.printf("Max command count reached! %d / %d\n", commands, commandsMax);
			return;
		}
		final IntBuffer intBuffer;
		if (useStagingBuffer) {
			intBuffer = buffer.getStagingBuffer(vkDeviceInstance).asIntBuffer();
		} else {
			intBuffer = buffer.getBuffer(vkDeviceInstance).asIntBuffer();
		}
		intBuffer
				.put(index++, indices) // indices
				.put(index++, instanceCount) // instance count
				.put(index++, firstIndex) // first index
				.put(index++, baseVertex) // base vertex
				.put(index++, baseInstance); // base instance
		commands++;
	}

	public void upload(VkCommandBuffer commandBuffer) {
		if (!useStagingBuffer) {
			return;
		}

		buffer.uploadFromStagingBuffer(commandBuffer);
		buffer.indirectBufferBarrier(commandBuffer);
		// TODO needs barrier
	}

	public long getBufferHandle() {
		return buffer.getHandle();
	}

	public void destroy(VkDeviceInstance vkDeviceInstance) {
		buffer.destroyAndFreeMemory(vkDeviceInstance);
	}
}
