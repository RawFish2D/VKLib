package ua.rawfish2d.vklib.init.enums;

import static org.lwjgl.vulkan.VK10.*;

public enum TextureFiltering {
	NEAREST,
	LINEAR,
	MIPMAP_NEAREST,
	MIPMAP_LINEAR;

	public int get() {
		return switch (this) {
			case NEAREST -> VK_FILTER_NEAREST;
			case LINEAR -> VK_FILTER_LINEAR;
			case MIPMAP_NEAREST -> VK_SAMPLER_MIPMAP_MODE_NEAREST;
			case MIPMAP_LINEAR -> VK_SAMPLER_MIPMAP_MODE_LINEAR;
		};
	}
}