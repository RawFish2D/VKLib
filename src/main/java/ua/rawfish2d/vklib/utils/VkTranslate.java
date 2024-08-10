package ua.rawfish2d.vklib.utils;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_BT709_NONLINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.NVRayTracing.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;

public class VkTranslate {
	/**
	 * Translates a Vulkan {@code VkResult} value to a String describing the result.
	 *
	 * @param result the {@code VkResult} value
	 * @return the result description
	 */
	public static String translateVulkanResult(int result) {
		return switch (result) {
			// Success codes
			case VK_SUCCESS -> "Command successfully completed.";
			case VK_NOT_READY -> "A fence or query has not yet completed.";
			case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
			case VK_EVENT_SET -> "An event is signaled.";
			case VK_EVENT_RESET -> "An event is unsignaled.";
			case VK_INCOMPLETE -> "A return array was too small for the result.";
			case VK_SUBOPTIMAL_KHR ->
					"A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

			// Error codes
			case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
			case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
			case VK_ERROR_INITIALIZATION_FAILED ->
					"Initialization of an object could not be completed for implementation-specific reasons.";
			case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
			case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
			case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
			case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
			case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
			case VK_ERROR_INCOMPATIBLE_DRIVER ->
					"The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
			case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
			case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
			case VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL";
			case VK_ERROR_UNKNOWN -> "VK_ERROR_UNKNOWN";
			case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
			case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR ->
					"The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
			case VK_ERROR_OUT_OF_DATE_KHR ->
					"A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
							+ "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue" + "presenting to the surface.";
			case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR ->
					"The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an" + " image.";
			case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
			case VK_ERROR_OUT_OF_POOL_MEMORY -> "VK_ERROR_OUT_OF_POOL_MEMORY";
			default -> String.format("%s %d", "Unknown enum:", result);
		};
	}

	public static String translateQueueBit(int bits) {
		final List<String> stringList = new ArrayList<>();
		if ((bits & VK_QUEUE_GRAPHICS_BIT) == 1) {
			stringList.add("VK_QUEUE_GRAPHICS_BIT");
		}
		if ((bits & VK_QUEUE_COMPUTE_BIT) == 2) {
			stringList.add("VK_QUEUE_COMPUTE_BIT");
		}
		if ((bits & VK_QUEUE_TRANSFER_BIT) == 4) {
			stringList.add("VK_QUEUE_TRANSFER_BIT");
		}
		if ((bits & VK_QUEUE_SPARSE_BINDING_BIT) == 8) {
			stringList.add("VK_QUEUE_SPARSE_BINDING_BIT");
		}
		if ((bits & VK_QUEUE_FAMILY_IGNORED) == -1) {
			stringList.add("VK_QUEUE_FAMILY_IGNORED");
		}
		if ((bits & VK_QUEUE_FAMILY_EXTERNAL) == -2) {
			stringList.add("VK_QUEUE_FAMILY_EXTERNAL");
		}
		final StringBuilder stringBuilder = new StringBuilder();
		for (int a = 0; a < stringList.size(); ++a) {
			stringBuilder.append(stringList.get(a));
			if (a != stringList.size() - 1) {
				stringBuilder.append(", ");
			}
		}
		return stringBuilder.toString();
	}

	public static String translatePhysicalDeviceType(int deviceType) {
		return switch (deviceType) {
			case VK_PHYSICAL_DEVICE_TYPE_OTHER -> "VK_PHYSICAL_DEVICE_TYPE_OTHER";
			case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU";
			case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU";
			case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU";
			case VK_PHYSICAL_DEVICE_TYPE_CPU -> "VK_PHYSICAL_DEVICE_TYPE_CPU";
			default -> "UNKNOWN";
		};
	}

	public static String translateColorSpace(int bits) {
		return switch (bits) {
			case VK_COLOR_SPACE_SRGB_NONLINEAR_KHR -> "VK_COLOR_SPACE_SRGB_NONLINEAR_KHR";
			case VK_COLOR_SPACE_BT709_NONLINEAR_EXT -> "VK_COLOR_SPACE_BT709_NONLINEAR_EXT"; // EXT
			case VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT -> "VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT"; // EXT
			default -> "unknown";
		};
	}

	public static String translateSurfaceFormatBit(int bits) {
		return switch (bits) {
			case VK_FORMAT_R4G4_UNORM_PACK8 -> "VK_FORMAT_R4G4_UNORM_PACK8";
			case VK_FORMAT_R4G4B4A4_UNORM_PACK16 -> "VK_FORMAT_R4G4B4A4_UNORM_PACK16";
			case VK_FORMAT_B4G4R4A4_UNORM_PACK16 -> "VK_FORMAT_B4G4R4A4_UNORM_PACK16";
			case VK_FORMAT_R5G6B5_UNORM_PACK16 -> "VK_FORMAT_R5G6B5_UNORM_PACK16";
			case VK_FORMAT_B5G6R5_UNORM_PACK16 -> "VK_FORMAT_B5G6R5_UNORM_PACK16";
			case VK_FORMAT_R5G5B5A1_UNORM_PACK16 -> "VK_FORMAT_R5G5B5A1_UNORM_PACK16";
			case VK_FORMAT_B5G5R5A1_UNORM_PACK16 -> "VK_FORMAT_B5G5R5A1_UNORM_PACK16";
			case VK_FORMAT_A1R5G5B5_UNORM_PACK16 -> "VK_FORMAT_A1R5G5B5_UNORM_PACK16";

			case VK_FORMAT_R8_UNORM -> "VK_FORMAT_R8_UNORM";
			case VK_FORMAT_R8_SNORM -> "VK_FORMAT_R8_SNORM";
			case VK_FORMAT_R8_USCALED -> "VK_FORMAT_R8_USCALED";
			case VK_FORMAT_R8_SSCALED -> "VK_FORMAT_R8_SSCALED";
			case VK_FORMAT_R8_UINT -> "VK_FORMAT_R8_UINT";
			case VK_FORMAT_R8_SINT -> "VK_FORMAT_R8_SINT";
			case VK_FORMAT_R8_SRGB -> "VK_FORMAT_R8_SRGB";

			case VK_FORMAT_R8G8_UNORM -> "VK_FORMAT_R8G8_UNORM";
			case VK_FORMAT_R8G8_SNORM -> "VK_FORMAT_R8G8_SNORM";
			case VK_FORMAT_R8G8_USCALED -> "VK_FORMAT_R8G8_USCALED";
			case VK_FORMAT_R8G8_SSCALED -> "VK_FORMAT_R8G8_SSCALED";
			case VK_FORMAT_R8G8_UINT -> "VK_FORMAT_R8G8_UINT";
			case VK_FORMAT_R8G8_SINT -> "VK_FORMAT_R8G8_SINT";
			case VK_FORMAT_R8G8_SRGB -> "VK_FORMAT_R8G8_SRGB";

			case VK_FORMAT_R8G8B8_UNORM -> "VK_FORMAT_R8G8B8_UNORM";
			case VK_FORMAT_R8G8B8_SNORM -> "VK_FORMAT_R8G8B8_SNORM";
			case VK_FORMAT_R8G8B8_USCALED -> "VK_FORMAT_R8G8B8_USCALED";
			case VK_FORMAT_R8G8B8_SSCALED -> "VK_FORMAT_R8G8B8_SSCALED";
			case VK_FORMAT_R8G8B8_UINT -> "VK_FORMAT_R8G8B8_UINT";
			case VK_FORMAT_R8G8B8_SINT -> "VK_FORMAT_R8G8B8_SINT";
			case VK_FORMAT_R8G8B8_SRGB -> "VK_FORMAT_R8G8B8_SRGB";

			case VK_FORMAT_B8G8R8_UNORM -> "VK_FORMAT_B8G8R8_UNORM";
			case VK_FORMAT_B8G8R8_SNORM -> "VK_FORMAT_B8G8R8_SNORM";
			case VK_FORMAT_B8G8R8_USCALED -> "VK_FORMAT_B8G8R8_USCALED";
			case VK_FORMAT_B8G8R8_SSCALED -> "VK_FORMAT_B8G8R8_SSCALED";
			case VK_FORMAT_B8G8R8_UINT -> "VK_FORMAT_B8G8R8_UINT";
			case VK_FORMAT_B8G8R8_SINT -> "VK_FORMAT_B8G8R8_SINT";
			case VK_FORMAT_B8G8R8_SRGB -> "VK_FORMAT_B8G8R8_SRGB";

			case VK_FORMAT_R8G8B8A8_UNORM -> "VK_FORMAT_R8G8B8A8_UNORM";
			case VK_FORMAT_R8G8B8A8_SNORM -> "VK_FORMAT_R8G8B8A8_SNORM";
			case VK_FORMAT_R8G8B8A8_USCALED -> "VK_FORMAT_R8G8B8A8_USCALED";
			case VK_FORMAT_R8G8B8A8_SSCALED -> "VK_FORMAT_R8G8B8A8_SSCALED";
			case VK_FORMAT_R8G8B8A8_UINT -> "VK_FORMAT_R8G8B8A8_UINT";
			case VK_FORMAT_R8G8B8A8_SINT -> "VK_FORMAT_R8G8B8A8_SINT";
			case VK_FORMAT_R8G8B8A8_SRGB -> "VK_FORMAT_R8G8B8A8_SRGB";

			case VK_FORMAT_B8G8R8A8_UNORM -> "VK_FORMAT_B8G8R8A8_UNORM";
			case VK_FORMAT_B8G8R8A8_SNORM -> "VK_FORMAT_B8G8R8A8_SNORM";
			case VK_FORMAT_B8G8R8A8_USCALED -> "VK_FORMAT_B8G8R8A8_USCALED";
			case VK_FORMAT_B8G8R8A8_SSCALED -> "VK_FORMAT_B8G8R8A8_SSCALED";
			case VK_FORMAT_B8G8R8A8_UINT -> "VK_FORMAT_B8G8R8A8_UINT";
			case VK_FORMAT_B8G8R8A8_SINT -> "VK_FORMAT_B8G8R8A8_SINT";
			case VK_FORMAT_B8G8R8A8_SRGB -> "VK_FORMAT_B8G8R8A8_SRGB";

			case VK_FORMAT_A8B8G8R8_UNORM_PACK32 -> "VK_FORMAT_A8B8G8R8_UNORM_PACK32";
			case VK_FORMAT_A8B8G8R8_SNORM_PACK32 -> "VK_FORMAT_A8B8G8R8_SNORM_PACK32";
			case VK_FORMAT_A8B8G8R8_SRGB_PACK32 -> "VK_FORMAT_A8B8G8R8_SRGB_PACK32";
			case VK_FORMAT_A2R10G10B10_UNORM_PACK32 -> "VK_FORMAT_A2R10G10B10_UNORM_PACK32";
			case VK_FORMAT_A2R10G10B10_SNORM_PACK32 -> "VK_FORMAT_A2R10G10B10_SNORM_PACK32";
			case VK_FORMAT_A2B10G10R10_UNORM_PACK32 -> "VK_FORMAT_A2B10G10R10_UNORM_PACK32";
			case VK_FORMAT_A2B10G10R10_SNORM_PACK32 -> "VK_FORMAT_A2B10G10R10_SNORM_PACK32";

			case VK_FORMAT_R16_UNORM -> "VK_FORMAT_R16_UNORM";
			case VK_FORMAT_R16_SNORM -> "VK_FORMAT_R16_SNORM";
			case VK_FORMAT_R16_USCALED -> "VK_FORMAT_R16_USCALED";
			case VK_FORMAT_R16_SSCALED -> "VK_FORMAT_R16_SSCALED";
			case VK_FORMAT_R16_UINT -> "VK_FORMAT_R16_UINT";
			case VK_FORMAT_R16_SINT -> "VK_FORMAT_R16_SINT";
			case VK_FORMAT_R16_SFLOAT -> "VK_FORMAT_R16_SFLOAT";

			case VK_FORMAT_R16G16_UNORM -> "VK_FORMAT_R16G16_UNORM";
			case VK_FORMAT_R16G16_SFLOAT -> "VK_FORMAT_R16G16_SFLOAT";

			case VK_FORMAT_R16G16B16_UNORM -> "VK_FORMAT_R16G16B16_UNORM";
			case VK_FORMAT_R16G16B16_SFLOAT -> "VK_FORMAT_R16G16B16_SFLOAT";

			case VK_FORMAT_R16G16B16A16_UNORM -> "VK_FORMAT_R16G16B16A16_UNORM";
			case VK_FORMAT_R16G16B16A16_SNORM -> "VK_FORMAT_R16G16B16A16_SNORM";
			case VK_FORMAT_R16G16B16A16_SFLOAT -> "VK_FORMAT_R16G16B16A16_SFLOAT";

			case VK_FORMAT_B10G11R11_UFLOAT_PACK32 -> "VK_FORMAT_B10G11R11_UFLOAT_PACK32";

			case VK_FORMAT_R32_UINT -> "VK_FORMAT_R32_UINT"; // uint
			case VK_FORMAT_R32_SINT -> "VK_FORMAT_R32_SINT"; // int
			case VK_FORMAT_R32_SFLOAT -> "VK_FORMAT_R32_SFLOAT"; // float

			case VK_FORMAT_R32G32_UINT -> "VK_FORMAT_R32G32_UINT"; // uvec2
			case VK_FORMAT_R32G32_SINT -> "VK_FORMAT_R32G32_SINT"; // ivec2
			case VK_FORMAT_R32G32_SFLOAT -> "VK_FORMAT_R32G32_SFLOAT"; // vec2

			case VK_FORMAT_R32G32B32_UINT -> "VK_FORMAT_R32G32B32_UINT"; // uvec3
			case VK_FORMAT_R32G32B32_SINT -> "VK_FORMAT_R32G32B32_SINT"; // ivec3
			case VK_FORMAT_R32G32B32_SFLOAT -> "VK_FORMAT_R32G32B32_SFLOAT"; // vec3

			case VK_FORMAT_R32G32B32A32_UINT -> "VK_FORMAT_R32G32B32A32_UINT"; // uvec4
			case VK_FORMAT_R32G32B32A32_SINT -> "VK_FORMAT_R32G32B32A32_SINT"; // ivec4
			case VK_FORMAT_R32G32B32A32_SFLOAT -> "VK_FORMAT_R32G32B32A32_SFLOAT"; // vec4

			case VK_FORMAT_R64_UINT -> "VK_FORMAT_R64_UINT"; // unsigned double ?
			case VK_FORMAT_R64_SINT -> "VK_FORMAT_R64_UINT"; // singled double ?
			case VK_FORMAT_R64_SFLOAT -> "VK_FORMAT_R64_UINT"; // double ?

			default -> "VK_FORMAT_UNDEFINED";
		};
	}

	public static String vkTranslateMemoryProperty(int bits) {
		final List<String> stringList = new ArrayList<>();
		if ((bits & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) == VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) == VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) == VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_HOST_COHERENT_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) == VK_MEMORY_PROPERTY_HOST_CACHED_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_HOST_CACHED_BIT");
		}
		if ((bits & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) == VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) {
			stringList.add("VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT");
		}
		final StringBuilder stringBuilder = new StringBuilder();
		for (int index = 0; index < stringList.size(); ++index) {
			stringBuilder.append(stringList.get(index));
			if (index != stringList.size() - 1) {
				stringBuilder.append(", ");
			}
		}
		return stringBuilder.toString();
	}

	public static int vulkanStageToShadercKind(int stage) {
		return switch (stage) {
			case VK_SHADER_STAGE_VERTEX_BIT -> shaderc_vertex_shader;
			case VK_SHADER_STAGE_FRAGMENT_BIT -> shaderc_fragment_shader;
			case VK_SHADER_STAGE_RAYGEN_BIT_NV -> shaderc_raygen_shader;
			case VK_SHADER_STAGE_CLOSEST_HIT_BIT_NV -> shaderc_closesthit_shader;
			case VK_SHADER_STAGE_MISS_BIT_NV -> shaderc_miss_shader;
			case VK_SHADER_STAGE_ANY_HIT_BIT_NV -> shaderc_anyhit_shader;
			default -> throw new IllegalArgumentException("Stage: " + stage);
		};
	}

	public static int vkFormatToByteCount(int format) {
		return switch (format) {
			case VK_FORMAT_R8_SINT -> 1; // int

			case VK_FORMAT_R16_SFLOAT -> 2; // half float
			case VK_FORMAT_R16G16_SFLOAT -> 4; // half float [2]
			case VK_FORMAT_R16G16B16_SFLOAT -> 6; // half float [3]
			case VK_FORMAT_R16G16B16A16_SFLOAT -> 8; // half float [4]

			case VK_FORMAT_R32_SFLOAT -> 4; // float
			case VK_FORMAT_R32G32_SFLOAT -> 8; // vec2
			case VK_FORMAT_R32G32B32_SFLOAT -> 12; // vec3
			case VK_FORMAT_R32G32B32A32_SFLOAT -> 16; // vec4

			case VK_FORMAT_R32_SINT -> 4; // int
			case VK_FORMAT_R32G32_SINT -> 8; // ivec2
			case VK_FORMAT_R32G32B32_SINT -> 12; // ivec3
			case VK_FORMAT_R32G32B32A32_SINT -> 16; // ivec4

			case VK_FORMAT_R32_UINT -> 4; // uint
			case VK_FORMAT_R32G32_UINT -> 8; // uvec2
			case VK_FORMAT_R32G32B32_UINT -> 12; // uvec3
			case VK_FORMAT_R32G32B32A32_UINT -> 16; // uvec4
			default -> throw new IllegalStateException("Unexpected value: " + format);
		};
	}

	public static String translateDescriptorType(int type) {
		return switch (type) {
			case VK_DESCRIPTOR_TYPE_SAMPLER -> "VK_DESCRIPTOR_TYPE_SAMPLER";
			case VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER -> "VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER";
			case VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE -> "VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE";
			case VK_DESCRIPTOR_TYPE_STORAGE_IMAGE -> "VK_DESCRIPTOR_TYPE_STORAGE_IMAGE";
			case VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER -> "VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER";
			case VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER -> "VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER";
			case VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER -> "VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER";
			case VK_DESCRIPTOR_TYPE_STORAGE_BUFFER -> "VK_DESCRIPTOR_TYPE_STORAGE_BUFFER";
			case VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC -> "VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC";
			case VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC -> "VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC";
			case VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT -> "VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT";
			case VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_NV -> "VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_NV";
			default -> throw new IllegalStateException("Unexpected value: " + type);
		};
	}

	public static String translatePresentMode(int presentMode) {
		return switch (presentMode) {
			case VK_PRESENT_MODE_IMMEDIATE_KHR -> "VK_PRESENT_MODE_IMMEDIATE_KHR";
			case VK_PRESENT_MODE_MAILBOX_KHR -> "VK_PRESENT_MODE_MAILBOX_KHR";
			case VK_PRESENT_MODE_FIFO_KHR -> "VK_PRESENT_MODE_FIFO_KHR";
			case VK_PRESENT_MODE_FIFO_RELAXED_KHR -> "VK_PRESENT_MODE_FIFO_RELAXED_KHR";
			default -> "UNKNOWN_PRESENT_MODE";
		};
	}
}
