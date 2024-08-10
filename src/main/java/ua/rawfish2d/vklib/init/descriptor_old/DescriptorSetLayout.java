package ua.rawfish2d.vklib.init.descriptor_old;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ua.rawfish2d.vklib.VkBuffer;
import ua.rawfish2d.vklib.utils.VkTranslate;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

@Deprecated
public class DescriptorSetLayout {
	private final List<BufferLayout> bufferLayoutList = new ArrayList<>();

	public BufferLayout addBufferInfo() {
		final BufferLayout bufferInfo = new BufferLayout();
		bufferLayoutList.add(bufferInfo);
		return bufferInfo;
	}

	public int getCount() {
		int count = 0;
		for (final BufferLayout bufferLayout : bufferLayoutList) {
			count += bufferLayout.bufferRanges.size();
		}
		return count;
	}

	public VkDescriptorSetLayoutBinding.Buffer makeSetLayoutBinding(MemoryStack stack) {
		final VkDescriptorSetLayoutBinding.Buffer buffer = VkDescriptorSetLayoutBinding.calloc(getCount(), stack);
		int index = 0;
		for (final BufferLayout bufferLayout : bufferLayoutList) {
			for (final BufferRange bufferRange : bufferLayout.bufferRanges) {
				buffer.put(index, VkDescriptorSetLayoutBinding.calloc(stack)
						.binding(bufferRange.binding)
						.descriptorCount(1)
						.descriptorType(bufferRange.descriptorType)
						.stageFlags(bufferRange.stageFlags)
						.pImmutableSamplers(null));
				index++;
			}
		}
		return buffer;
	}

	public VkWriteDescriptorSet.Buffer makeWriteDescriptorSet(long vkDescriptorSet, BufferLayout bufferLayout, VkBuffer vkBuffer, MemoryStack stack) {
		final int bufferRanges = bufferLayout.bufferRanges.size();
		final VkWriteDescriptorSet.Buffer buffer = VkWriteDescriptorSet.calloc(bufferRanges, stack);

		for (int index = 0; index < bufferRanges; ++index) {
			final BufferRange bufferRange = bufferLayout.bufferRanges.get(index);
			System.out.printf("🔷 vkBuffer: %s | type: %s | offset: %d | range: %d\n", String.format("0x%08x", vkBuffer.getHandle()), VkTranslate.translateDescriptorType(bufferRange.descriptorType), bufferRange.offset, bufferRange.range);
			if (!bufferRange.image) {
				final VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
						.buffer(vkBuffer.getHandle())
						.offset(bufferRange.offset)
						.range(bufferRange.range);

				buffer.put(index, VkWriteDescriptorSet.calloc(stack)
						.sType$Default()
						.dstSet(vkDescriptorSet)
						.dstBinding(bufferRange.binding)
						.dstArrayElement(0)
						.descriptorType(bufferRange.descriptorType)
						.descriptorCount(1)
						.pBufferInfo(bufferInfo)
						.pImageInfo(null)
						.pTexelBufferView(null));
			} else {
				final VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
						.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
						.imageView(bufferRange.imageView)
						.sampler(bufferRange.imageSampler);

				buffer.put(index, VkWriteDescriptorSet.calloc(stack)
						.sType$Default()
						.dstSet(vkDescriptorSet)
						.dstBinding(bufferRange.binding)
						.dstArrayElement(0)
						.descriptorType(bufferRange.descriptorType)
						.descriptorCount(1)
						.pBufferInfo(null)
						.pImageInfo(imageInfo)
						.pTexelBufferView(null));
			}
		}
		return buffer;
	}

	public int calculateDescriptorPoolSize(int swapChainImageCount) {
		int count = 0;
		for (final BufferLayout bufferLayout : bufferLayoutList) {
			count += bufferLayout.bufferRanges.size();
		}
		return count * swapChainImageCount;
	}

	public VkDescriptorPoolSize.Buffer makeDescriptorPoolSizes(MemoryStack stack, int descriptorPoolSize) {
		final VkDescriptorPoolSize.Buffer buffer = VkDescriptorPoolSize.calloc(getCount(), stack);

		int index = 0;
		for (final BufferLayout bufferLayout : bufferLayoutList) {
			for (final BufferRange bufferRange : bufferLayout.bufferRanges) {
				buffer.put(index, VkDescriptorPoolSize.calloc(stack)
						.type(bufferRange.descriptorType)
						.descriptorCount(descriptorPoolSize));
				index++;
			}
		}
		return buffer;
	}

	public record UpdateDescriptorSetInfo(int swapChainIndex, BufferLayout bufferLayout, VkBuffer vkBuffer) {
	}

	public static class BufferLayout {
		private final List<BufferRange> bufferRanges = new ArrayList<>();

		public BufferLayout add(int binding, int descriptorType, int stageFlags, int format, int count) {
			bufferRanges.add(new BufferRange(binding, descriptorType, stageFlags, format, count));
			calculateOffsets();
			return this;
		}

		public BufferLayout addSampler(int binding, int descriptorType, int stageFlags) {
			bufferRanges.add(new BufferRange(binding, descriptorType, stageFlags));
			calculateOffsets();
			return this;
		}

		private void calculateOffsets() {
			int offset = 0;
			for (BufferRange info : bufferRanges) {
				info.offset = offset;
				offset += info.range;
			}
		}

		public int getBufferSize() {
			int size = 0;
			for (BufferRange info : bufferRanges) {
				size += info.range;
			}
			return size;
		}
	}

	public static class BufferRange {
		private final int binding;
		private final int descriptorType;
		private final int stageFlags;
		private final int range;
		private final boolean image;
		private int offset = 0;
		private long imageView = 0;
		private long imageSampler = 0;

		public BufferRange(int binding, int descriptorType, int stageFlags, int format, int count) {
			this.binding = binding;
			this.descriptorType = descriptorType;
			this.stageFlags = stageFlags;
			this.range = VkTranslate.vkFormatToByteCount(format) * count;
			this.image = false;
		}

		public BufferRange(int binding, int descriptorType, int stageFlags) {
			this.binding = binding;
			this.descriptorType = descriptorType;
			this.stageFlags = stageFlags;
			this.range = 0;
			this.image = true;
		}
	}
}
