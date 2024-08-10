package ua.rawfish2d.vklib;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ua.rawfish2d.vklib.utils.VkTranslate;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class BufferSizeInfo {
	private final List<Element> elements = new ArrayList<>();

	public BufferSizeInfo add(int format, int count) {
		elements.add(new Element(format, count));
		return this;
	}

	public int getBufferSize() {
		int size = 0;
		for (Element element : elements) {
			size += element.getByteSize();
		}
		return size;
	}

	@AllArgsConstructor
	public static class Element {
		private final int format;
		private final int count;

		public int getByteSize() {
			return VkTranslate.vkFormatToByteCount(format) * count;
		}
	}
}
