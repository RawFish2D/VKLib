package ua.rawfish2d.vklib.attrib;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import ua.rawfish2d.vklib.utils.VkTranslate;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;

@NoArgsConstructor
public class AttribFormat {
	private final List<AttribInfo> attribInfoList = new ArrayList<>();
	@Getter
	private BufferLayout bufferLayout;
	@Getter
	private int primitiveCount;
	@Getter
	private int verticesPerPrimitive;

	public AttribFormat add(int attribBinding, int attribLocation, int format) {
		attribInfoList.add(new AttribInfo(attribBinding, attribLocation, format, 0));
		return this;
	}

	public AttribFormat addWithDivisor(int attribBinding, int attribLocation, int format, int divisor) {
		attribInfoList.add(new AttribInfo(attribBinding, attribLocation, format, divisor));
		return this;
	}

	public AttribFormat setBufferLayout(BufferLayout bufferLayout) {
		this.bufferLayout = bufferLayout;
		return this;
	}

	public AttribFormat setPrimitiveCount(int primitiveCount) {
		this.primitiveCount = primitiveCount;
		return this;
	}

	public AttribFormat setVerticesPerPrimitive(int verticesPerPrimitive) {
		this.verticesPerPrimitive = verticesPerPrimitive;
		return this;
	}

	private int getSequentialOffset(int index, int objectCount, int verticesPerObject) {
		final AttribInfo attribute = attribInfoList.get(index);
		if (attribute.isInstanced()) {
			return attribute.getSize() * objectCount;
		} else {
			return attribute.getSize() * objectCount * verticesPerObject;
		}
	}

	private int getSequentialOffset(AttribInfo attribute, int objectCount, int verticesPerObject) {
		if (attribute.isInstanced()) {
			return attribute.getSize() * objectCount;
		} else {
			return attribute.getSize() * objectCount * verticesPerObject;
		}
	}

	private int getInterleavedStride() {
		int stride = 0;
		for (AttribInfo attribute : attribInfoList) {
			stride += attribute.getSize();
		}
		return stride;
	}

	private int getInterleavedOffset(int attributeIndex) {
		int offset = 0;
		for (int a = 0; a < attributeIndex; ++a) {
			final AttribInfo attribute = attribInfoList.get(a);
			offset += attribute.getSize();
		}
		return offset;
	}

	public int getSeparateBufferSize(int primitiveCount, int verticesPerPrimitive) {
		if (attribInfoList.size() != 1) {
			System.err.printf("Converting attribute format with %d attributes to separate format. Something is off...", attribInfoList.size());
		}
		final int totalBufferSizeBytes;
		final AttribInfo attribInfo = attribInfoList.get(0);
		if (attribInfo.isInstanced()) {
			totalBufferSizeBytes = attribInfo.getSize() * primitiveCount;
		} else {
			totalBufferSizeBytes = attribInfo.getSize() * primitiveCount * verticesPerPrimitive;
		}
		return totalBufferSizeBytes;
	}

	public int getSequentialBufferSize(int primitiveCount, int verticesPerPrimitive) {
		int totalBufferSizeBytes = 0;
		for (AttribInfo attribute : attribInfoList) {
			final int attributeSize;
			if (attribute.isInstanced()) {
				attributeSize = attribute.getSize() * primitiveCount;
			} else {
				attributeSize = attribute.getSize() * primitiveCount * verticesPerPrimitive;
			}
			totalBufferSizeBytes += attributeSize;
		}
		return totalBufferSizeBytes;
	}

	public int getInterleavedBufferSize(int primitiveCount, int verticesPerPrimitive) {
		int totalBufferSizeBytes = 0;
		for (AttribInfo attribute : attribInfoList) {
			final int attributeSize;
			if (attribute.isInstanced()) {
				attributeSize = attribute.getSize() * primitiveCount;
			} else {
				attributeSize = attribute.getSize() * primitiveCount * verticesPerPrimitive;
			}
			totalBufferSizeBytes += attributeSize;
		}
		return totalBufferSizeBytes;
	}

	public void attachAsInterleaved(int vaoID, int slot) {
		int stride = getInterleavedStride();
		int index = 0;
		for (AttribInfo attribute : attribInfoList) {
			int offset = getInterleavedOffset(index);
			attribute.setOffset(offset);
			attribute.setStride(stride);
//			attachGL45(attribute, vaoID, 0, slot, stride, offset);
			index++;
		}
	}

	public void attachAsSequential(int vaoID, int slot, int primitiveCount, int verticesPerPrimitive) {
		int relativeOffset = 0;
		for (AttribInfo attribute : attribInfoList) {
			final int stride = 0;
			attribute.setOffset(relativeOffset);
			attribute.setStride(stride);
			// doesnt work with GL45 functions for some reason
			// attachGL45(attribute, vaoID, buffer.getBufferID(), slot, stride, relativeOffset);
//			attachGL33(attribute, vaoID, 0, slot, stride, relativeOffset);

			relativeOffset += getSequentialOffset(attribute, primitiveCount, verticesPerPrimitive);
		}
	}

	public void attachAsSeparate(int vaoID, int slot) {
		if (attribInfoList.size() > 1) {
			System.err.println("You are trying to attach buffer with more than 1 attribute as Separate. Which is not okay.");
		}
		final AttribInfo attribute = attribInfoList.get(0);
		final int stride = attribute.getSize();
		attribute.setStride(stride);
		attribute.setOffset(0);
//		buffer.setInitilized(true);
//		attachGL45(attribute, vaoID, 0, slot, stride, 0);
	}

	//	 java 14
//	public int getBufferSize(int primitivesCount, int verticesPerPrimitive, BufferLayout bufferLayout) {
//		return switch (bufferLayout) {
//			case SEPARATE -> getSeparateBufferSize(primitivesCount, verticesPerPrimitive);
//			case SEQUENTIAL -> getSequentialBufferSize(primitivesCount, verticesPerPrimitive);
//			case INTERLEAVED -> getInterleavedBufferSize(primitivesCount, verticesPerPrimitive);
//		};
//	}

	public int getBufferSize() {
		return switch (bufferLayout) {
			case SEPARATE -> getSeparateBufferSize(primitiveCount, verticesPerPrimitive);
			case SEQUENTIAL -> getSequentialBufferSize(primitiveCount, verticesPerPrimitive);
			case INTERLEAVED -> getInterleavedBufferSize(primitiveCount, verticesPerPrimitive);
		};
	}

	public int getShaderStorageBufferSize(int entriesCount) {
		int totalBufferSizeBytes = 0;
		for (AttribInfo attribute : attribInfoList) {
			totalBufferSizeBytes += attribute.getSize();
		}
		return totalBufferSizeBytes * entriesCount;
	}

	// VULKAN
	public VkVertexInputAttributeDescription.Buffer getInterleavedDescriptions(MemoryStack stack) {
		final VkVertexInputAttributeDescription.Buffer attributeDescriptionList = VkVertexInputAttributeDescription.calloc(attribInfoList.size(), stack);

		final int stride = getInterleavedStride();
		int index = 0;
		for (AttribInfo attribute : attribInfoList) {
			final int offset = getInterleavedOffset(index);
			attribute.setOffset(offset);
			attribute.setStride(stride);
			System.out.printf("offset: %d | stride: %d\n", offset, stride);
//			attachGL45(attribute, vaoID, 0, slot, stride, offset);
			attributeDescriptionList.put(index, makeAttributeDescription(attribute, stack));
			index++;
		}
		return attributeDescriptionList;
	}

	public VkVertexInputAttributeDescription.Buffer getSeparateDescriptions(MemoryStack stack) {
		throw new RuntimeException("Not implemented");
	}

	public VkVertexInputAttributeDescription.Buffer getSequentialDescriptions(MemoryStack stack, int primitiveCount, int verticesPerPrimitive) {
		final VkVertexInputAttributeDescription.Buffer attributeDescriptionList = VkVertexInputAttributeDescription.calloc(attribInfoList.size(), stack);

		int relativeOffset = 0;
		int index = 0;
		for (AttribInfo attribute : attribInfoList) {
			final int stride = 0;
			attribute.setOffset(relativeOffset);
			attribute.setStride(stride);
			// doesnt work with GL45 functions for some reason
			// attachGL45(attribute, vaoID, buffer.getBufferID(), slot, stride, relativeOffset);
//			attachGL33(attribute, vaoID, buffer.getBufferID(), slot, stride, relativeOffset);
			attributeDescriptionList.put(index, makeAttributeDescription(attribute, stack));

			relativeOffset += getSequentialOffset(attribute, primitiveCount, verticesPerPrimitive);
			index++;
		}
		return attributeDescriptionList;
	}

	public VkVertexInputAttributeDescription makeAttributeDescription(AttribInfo attribInfo, MemoryStack stack) {
		return VkVertexInputAttributeDescription.calloc(stack)
				.binding(attribInfo.getAttribBinding())
				.location(attribInfo.getAttribLocation())
				.format(attribInfo.getFormat())
				.offset(attribInfo.getOffset());
	}

	public VkPipelineVertexInputStateCreateInfo makeVertexAttribDescription(MemoryStack stack) {
		final VkVertexInputAttributeDescription.Buffer attributeDescriptionList =
				switch (getBufferLayout()) {
					case INTERLEAVED -> getInterleavedDescriptions(stack);
					case SEPARATE -> getSeparateDescriptions(stack);
					case SEQUENTIAL -> getSequentialDescriptions(stack, getPrimitiveCount(), getVerticesPerPrimitive());
				};

//		int stride = switch (format.getBufferLayout()) {
//			case INTERLEAVED -> format.getStride();
//			case SEPARATE -> format.getStride();
//			case SEQUENTIAL -> 0;
//		};

		final VkVertexInputBindingDescription.Buffer bindingDescription = makeVertexInputBindingDescriptions(stack);

//		final VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1, stack)
//				.binding(binding)
//				.stride(stride)
//				.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

		return VkPipelineVertexInputStateCreateInfo.calloc(stack)
				.sType$Default()
				.pVertexBindingDescriptions(bindingDescription)
				.pVertexAttributeDescriptions(attributeDescriptionList);
	}

	public VkVertexInputBindingDescription.Buffer makeVertexInputBindingDescriptions(MemoryStack stack) {
		@RequiredArgsConstructor
		class BindingData {
			final int binding;
			final int stride;

			static boolean exists(List<BindingData> list, int binding) {
				for (BindingData data : list) {
					if (data.binding == binding) {
						return true;
					}
				}
				return false;
			}
		}
		final List<BindingData> bindingDataList = new ArrayList<>();
		for (AttribInfo info : attribInfoList) {
			System.out.printf("AttribInfo: binding: %d | location: %d | format: %s | size: %d | offset: %d\n",
					info.getAttribBinding(),
					info.getAttribLocation(),
					VkTranslate.translateSurfaceFormatBit(info.getFormat()),
					info.getSize(),
					info.getOffset());
			if (!BindingData.exists(bindingDataList, info.getAttribBinding())) {
				bindingDataList.add(new BindingData(info.getAttribBinding(), info.getSize()));
			}
		}

		final VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(bindingDataList.size(), stack);
		int index = 0;
		for (BindingData data : bindingDataList) {
			System.out.printf("VkVertexInputBindingDescription [%d]: binding: %d | stride: %d\n", index, data.binding, data.stride);
			bindingDescription.put(index, VkVertexInputBindingDescription.calloc(stack)
					.binding(data.binding)
					.stride(data.stride)
					.inputRate(VK_VERTEX_INPUT_RATE_VERTEX));
			index++;
		}

		return bindingDescription;
	}

	public int getSequentialAttribPosition(int attributeIndex) {
		if (attributeIndex < 0 || attributeIndex >= attribInfoList.size()) {
			throw new RuntimeException("WTF?? attribute index: " + attributeIndex + " attribute count: " + attribInfoList.size());
		}
		int offset = 0;
		for (int a = 0; a < attributeIndex; ++a) {
			final AttribInfo attribute = attribInfoList.get(a);
			offset += attribute.getSize() * verticesPerPrimitive * primitiveCount;
		}
		return offset;
	}
}
