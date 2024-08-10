package ua.rawfish2d.vklib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ua.rawfish2d.vklib.init.VkDeviceInstance;
import ua.rawfish2d.vklib.utils.VkHelper;
import ua.rawfish2d.vklib.utils.VkTranslate;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.*;

@RequiredArgsConstructor
public class VkBuffer {
	private long vkBuffer;
	private long vkBufferMemory;
	@Getter
	private int bufferSize;
	private long mappedBufferPointer = 0;
	private ByteBuffer buffer;
	private VkBuffer vkStagingBuffer = null;

	public void createBuffer(VkDeviceInstance vkDeviceInstance, int bufferSize, int usageFlags) {
		this.bufferSize = bufferSize;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			System.out.printf("Buffer size: %d\n", bufferSize);
			// create vertex buffer
			final VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
					.sType$Default()
					.size(bufferSize)
					.usage(usageFlags)
					.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

			final LongBuffer pBuffer = stack.mallocLong(1);
			final int result = vkCreateBuffer(vkDeviceInstance.getVkLogicalDevice(), bufferInfo, null, pBuffer);
			if (result != VK_SUCCESS) {
				throw new RuntimeException("Failed to create vertex buffer! Error: " + VkTranslate.translateVulkanResult(result));
			}
			vkBuffer = pBuffer.get(0);
			System.out.printf("buffer address: %s\n", String.format("0x%08x", vkBuffer));
		}
	}

	public void allocateMemory(VkDeviceInstance vkDeviceInstance, int memoryFlags) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// check available memory types
			final VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
			vkGetBufferMemoryRequirements(vkDeviceInstance.getVkLogicalDevice(), vkBuffer, memRequirements);

			final int memoryType = VkHelper.findMemoryType(vkDeviceInstance.getVkPhysicalDevice(), memRequirements.memoryTypeBits(), memoryFlags, stack);

			// allocate memory
			final VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
					.sType$Default()
					.allocationSize(bufferSize)
					.memoryTypeIndex(memoryType);

			final LongBuffer pVertexBufferMemory = stack.mallocLong(1);
			if (vkAllocateMemory(vkDeviceInstance.getVkLogicalDevice(), allocInfo, null, pVertexBufferMemory) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocate vertex buffer memory!");
			}
			vkBufferMemory = pVertexBufferMemory.get(0);
		}
		// attach/bind memory to vertex buffer
		vkBindBufferMemory(vkDeviceInstance.getVkLogicalDevice(), vkBuffer, vkBufferMemory, 0);
	}

	public long getHandle() {
		return vkBuffer;
	}

	public long getMemoryHandle() {
		return vkBufferMemory;
	}

	public void bindVertexBuffer(VkCommandBuffer commandBuffer) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final LongBuffer pVertexBuffer = stack.longs(this.getHandle());
			final LongBuffer pVertexBufferOffsets = stack.longs(0);
			vkCmdBindVertexBuffers(commandBuffer, 0, pVertexBuffer, pVertexBufferOffsets);
		}
	}

	public void bindIndexBuffer(VkCommandBuffer commandBuffer, int type) {
		vkCmdBindIndexBuffer(commandBuffer, vkBuffer, 0, type);
	}

	public void bindIndexBuffer(VkCommandBuffer commandBuffer) {
		vkCmdBindIndexBuffer(commandBuffer, vkBuffer, 0, VK_INDEX_TYPE_UINT32);
	}

	public ByteBuffer getStagingBuffer(VkDeviceInstance vkDeviceInstance) {
		return vkStagingBuffer.getBuffer(vkDeviceInstance);
	}

	public ByteBuffer getBuffer(VkDeviceInstance vkDeviceInstance) {
		if (mappedBufferPointer == 0) {
			// map memory and get buffer
			try (MemoryStack stack = MemoryStack.stackPush()) {
				final PointerBuffer pData = stack.mallocPointer(1);
				vkMapMemory(vkDeviceInstance.getVkLogicalDevice(), vkBufferMemory, 0, bufferSize, 0, pData);
				mappedBufferPointer = pData.get(0);
			}
			buffer = memByteBuffer(mappedBufferPointer, bufferSize);
		}
		return buffer;
	}

	public void makeStagingBuffer(VkDeviceInstance vkDeviceInstance, int usageFlags) {
		if (vkStagingBuffer != null) {
			return;
		}
		vkStagingBuffer = new VkBuffer();
		System.out.printf("Creating staging buffer:\n");
		vkStagingBuffer.createBuffer(vkDeviceInstance, bufferSize, usageFlags);
		vkStagingBuffer.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
	}

	public void deleteStagingBuffer(VkDeviceInstance vkDeviceInstance) {
		if (vkStagingBuffer != null) {
			vkStagingBuffer.destroyAndFreeMemory(vkDeviceInstance);
			vkStagingBuffer = null;
		}
	}

	public void destroyAndFreeMemory(VkDeviceInstance vkDeviceInstance) {
		deleteStagingBuffer(vkDeviceInstance);
		if (mappedBufferPointer != 0) {
			vkUnmapMemory(vkDeviceInstance.getVkLogicalDevice(), vkBufferMemory);
		}
		vkDestroyBuffer(vkDeviceInstance.getVkLogicalDevice(), vkBuffer, null);
		vkFreeMemory(vkDeviceInstance.getVkLogicalDevice(), vkBufferMemory, null);
	}

	public void uploadFromStagingBuffer(VkCommandBuffer commandBuffer) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
					.srcOffset(0)
					.dstOffset(0)
					.size(bufferSize);
			vkCmdCopyBuffer(commandBuffer, vkStagingBuffer.vkBuffer, vkBuffer, copyRegion);
		}
	}

	public static void gigaBarrier(VkCommandBuffer commandBuffer) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkMemoryBarrier.Buffer memoryBarriers = VkMemoryBarrier.calloc(1, stack).sType$Default();
			final VkMemoryBarrier vkMemoryBarrier = VkMemoryBarrier.calloc(stack)
					.sType$Default()
					.pNext(0)
					.srcAccessMask(VK_ACCESS_MEMORY_WRITE_BIT)
					.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT);
			memoryBarriers.put(0, vkMemoryBarrier);

			vkCmdPipelineBarrier(commandBuffer,
					VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
					VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
					0,
					memoryBarriers,
					null,
					null);
		}
	}

	public void vertexBufferBarrier(VkCommandBuffer commandBuffer) {
		VkHelper.bufferMemoryBarrier2(commandBuffer,
				vkBuffer, bufferSize,
				VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
				VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT, VK_PIPELINE_STAGE_VERTEX_INPUT_BIT);
	}

	public void indexBufferBarrier(VkCommandBuffer commandBuffer) {
		VkHelper.bufferMemoryBarrier2(commandBuffer,
				vkBuffer, bufferSize,
				VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
				VK_ACCESS_INDEX_READ_BIT, VK_PIPELINE_STAGE_VERTEX_INPUT_BIT);
	}

	public void indirectBufferBarrier(VkCommandBuffer commandBuffer) {
		VkHelper.bufferMemoryBarrier2(commandBuffer,
				vkBuffer, bufferSize,
				VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
				VK_ACCESS_INDIRECT_COMMAND_READ_BIT, VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT);
	}

	public void ssboBarrier(VkCommandBuffer commandBuffer) {
		VkHelper.bufferMemoryBarrier2(commandBuffer,
				vkBuffer, bufferSize,
				VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
				VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT);
	}

	public void uniformBarrier(VkCommandBuffer commandBuffer) {
		VkHelper.bufferMemoryBarrier2(commandBuffer,
				vkBuffer, bufferSize,
				VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
				VK_ACCESS_UNIFORM_READ_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT);
	}
}
