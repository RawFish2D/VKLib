package ua.rawfish2d.vklib.attrib;

import lombok.Getter;
import lombok.Setter;
import ua.rawfish2d.vklib.utils.VkTranslate;

@Getter
public class AttribInfo {
	private final int attribBinding;
	private final int attribLocation;
	private final int elementByteSize;
	private final int format;
	@Setter
	private int offset = 0;
	@Setter
	private int stride = 0;
	private final int divisor;

	public AttribInfo(int attribBinding, int attribLocation, int format, int divisor) {
		this.attribBinding = attribBinding;
		this.attribLocation = attribLocation;
		this.format = format;
		this.elementByteSize = VkTranslate.vkFormatToByteCount(format);
		this.divisor = divisor;
	}

	public int getSize() {
		return elementByteSize;
	}

	public boolean isInstanced() {
		return divisor != 0;
	}
}
