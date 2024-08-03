package ua.rawfish2d.vklib.init.data;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import ua.rawfish2d.vklib.utils.VkHelper;

public class SwapChainSupportDetails {
	public VkSurfaceCapabilitiesKHR capabilities;
	public int surfaceFormatCount = 0;
	public VkSurfaceFormatKHR.Buffer surfaceFormats;
	public int presentModeCount = 0;
	public int[] presentModes;

	public void print() {
		System.out.println("🔷 CapabilitiesKHR:" +
				"\n\tminImageCount: " + capabilities.minImageCount() +
				"\n\tmaxImageCount: " + capabilities.maxImageCount() +
				"\n\tmaxImageArrayLayers: " + capabilities.maxImageArrayLayers() +
				"\n\tsupportedTransforms: " + capabilities.supportedTransforms() +
				"\n\tsupportedCompositeAlpha: " + capabilities.supportedCompositeAlpha() +
				"\n\tsupportedUsageFlags: " + capabilities.supportedUsageFlags());

		System.out.println("🔷 SurfaceFormatKHR count: " + surfaceFormatCount);
		for (int a = 0; a < surfaceFormatCount; ++a) {
			final VkSurfaceFormatKHR surfaceFormatKHR = surfaceFormats.get(a);
			System.out.printf("\t[%d] colorSpace: %d %s | format: %d %s \n",
					a,
					surfaceFormatKHR.colorSpace(),
					VkHelper.translateColorSpace(surfaceFormatKHR.colorSpace()),
					surfaceFormatKHR.format(),
					VkHelper.translateSurfaceFormatBit(surfaceFormatKHR.format()));
		}

		System.out.println("🔷 PresentMode count: " + presentModeCount);
		for (int a = 0; a < presentModeCount; ++a) {
			System.out.printf("\t[%d]: %d\n", a, presentModes[a]);
		}
	}
}