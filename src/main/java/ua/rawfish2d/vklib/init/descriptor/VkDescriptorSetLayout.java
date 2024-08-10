package ua.rawfish2d.vklib.init.descriptor;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import ua.rawfish2d.vklib.init.VkDeviceInstance;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;

public class VkDescriptorSetLayout {
	private final List<SetLayoutBindingsBuffer> setLayoutBindingsBuffers = new ArrayList<>();
	@Getter
	private long handle;

	public SetLayoutBindingsBuffer addLayout() {
		final SetLayoutBindingsBuffer setLayout = new SetLayoutBindingsBuffer();
		setLayoutBindingsBuffers.add(setLayout);
		return setLayout;
	}

	public void createDescriptorSetLayout(VkDeviceInstance vkDeviceInstance) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = makeSetLayoutBinding(stack);

			final VkDescriptorSetLayoutCreateInfo setLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
					.sType$Default()
					.pBindings(descriptorSetLayoutBindings);

			final LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
			if (vkCreateDescriptorSetLayout(vkDeviceInstance.getVkLogicalDevice(), setLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create descriptor set layout!");
			}
			handle = pDescriptorSetLayout.get(0);
		}
	}

	private VkDescriptorSetLayoutBinding.Buffer makeSetLayoutBinding(MemoryStack stack) {
		final VkDescriptorSetLayoutBinding.Buffer buffer = VkDescriptorSetLayoutBinding.calloc(getCount(), stack);
		int index = 0;
		for (final SetLayoutBindingsBuffer setLayoutBindingsBuffer : setLayoutBindingsBuffers) {
			for (final SetLayoutBinding setLayoutBinding : setLayoutBindingsBuffer.getSetLayoutBindings()) {
				buffer.put(index, VkDescriptorSetLayoutBinding.calloc(stack)
						.binding(setLayoutBinding.binding())
						.descriptorCount(1)
						.descriptorType(setLayoutBinding.descriptorType())
						.stageFlags(setLayoutBinding.stageFlags())
						.pImmutableSamplers(null));
				index++;
			}
		}
		return buffer;
	}

	private int getCount() {
		int count = 0;
		for (final SetLayoutBindingsBuffer setLayout : setLayoutBindingsBuffers) {
			count += setLayout.entriesCount();
		}
		return count;
	}

	public VkDescriptorPoolSize.Buffer makeDescriptorPoolSizes(MemoryStack stack, int descriptorPoolSize) {
		final VkDescriptorPoolSize.Buffer buffer = VkDescriptorPoolSize.calloc(getCount(), stack);

		int index = 0;
		for (final SetLayoutBindingsBuffer setLayoutBindingsBuffer : setLayoutBindingsBuffers) {
			for (final SetLayoutBinding setLayoutBinding : setLayoutBindingsBuffer.getSetLayoutBindings()) {
				buffer.put(index, VkDescriptorPoolSize.calloc(stack)
						.type(setLayoutBinding.descriptorType())
						.descriptorCount(descriptorPoolSize));
				index++;
			}
		}
		return buffer;
	}
}
