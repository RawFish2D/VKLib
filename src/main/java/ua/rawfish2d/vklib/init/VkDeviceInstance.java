package ua.rawfish2d.vklib.init;

import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import ua.rawfish2d.vklib.WindowVK;
import ua.rawfish2d.vklib.init.data.*;
import ua.rawfish2d.vklib.utils.VkHelper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;
import static org.lwjgl.vulkan.VK13.vkQueueSubmit2;

@Getter
public class VkDeviceInstance {
	private static final List<String> validationLayers = new ArrayList<>();
	private static final List<String> requiredDeviceExtensions = new ArrayList<>();
	private final List<VkQueueInfo> vkQueueDataList = new ArrayList<>();
	private WindowVK windowVK;
	private boolean enableValidationLayers = false;
	private boolean overlappingQueues = false;
	private VkInstance vkInstance;
	private VkPhysicalDevice vkPhysicalDevice;
	private VkDevice vkLogicalDevice;
	private long vkSurface;
	private String applicationName = "Vulkan App";
	private String engineName = "No Engine";
	private int vulkanVersion = VK_API_VERSION_1_3;
	// swap chain stuff
	private boolean vsync = true;
	private int swapChainImageCount = 2; // also frames in flight count
	private long vkSwapchainKHR;
	private int vkSwapChainImageFormat;
	private VkExtent2D vkExtent2D = null;
	private LongBuffer vkSwapChainImages;
	private long[] vkSwapChainImageViews;
	private boolean shouldRecreateSwapChain = false;
	// frame in flight stuff
	private final IntBuffer pCurrentFrameIndex = memAllocInt(1);
	private int currentFrame = 0;
	private final List<FrameInFlight> frameInFlights = new ArrayList<>();

	static {
		validationLayers.add("VK_LAYER_KHRONOS_validation");
		requiredDeviceExtensions.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
		requiredDeviceExtensions.add(VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME);
		requiredDeviceExtensions.add("VK_KHR_synchronization2");
	}

	public VkDeviceInstance applicationName(String applicationName) {
		this.applicationName = applicationName;
		return this;
	}

	public VkDeviceInstance engineName(String engineName) {
		this.engineName = engineName;
		return this;
	}

	public VkDeviceInstance apiVersion(int vulkanVersion) {
		this.vulkanVersion = vulkanVersion;
		return this;
	}

	public VkDeviceInstance validationLayers(boolean enableValidationLayers) {
		this.enableValidationLayers = enableValidationLayers;
		return this;
	}

	public VkDeviceInstance swapChainImageCount(int swapChainImageCount) {
		this.swapChainImageCount = swapChainImageCount;
		return this;
	}

	public VkDeviceInstance vsync(boolean vsync) {
		this.vsync = vsync;
		return this;
	}

	public VkDeviceInstance create(WindowVK window) {
		this.windowVK = window;
		createInstance(enableValidationLayers);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final LongBuffer pSurface = stack.mallocLong(1);
			final int result = glfwCreateWindowSurface(vkInstance, window.getHandle(), null, pSurface);
			if (result != VK_SUCCESS) {
				throw new AssertionError("Failed to create window surface! Error: " + VkHelper.translateVulkanResult(result));
			}
			vkSurface = pSurface.get(0);
		}
		pickPhysicalDevice();
		createLogicalDevice();
		// swap chain stuff
		createSwapChain();
		createImageViews();
		for (VkQueueInfo vkQueueInfo : vkQueueDataList) {
			vkQueueInfo.createCommandPool();
		}
		createFramesInFlight(swapChainImageCount);
		for (FrameInFlight frameInFlight : frameInFlights) {
			frameInFlight.createCommandBuffer(this);
		}
		return this;
	}

	private void createInstance(boolean enableValidationLayers) {
		if (enableValidationLayers && !checkValidationLayerSupport()) {
			throw new RuntimeException("Validation layers requested, but not available!");
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkApplicationInfo vkAppInto = VkApplicationInfo.calloc(stack)
					.sType$Default()
					.pApplicationName(stack.UTF8(applicationName))
					.applicationVersion(VK_MAKE_VERSION(1, 0, 0))
					.pEngineName(engineName.isEmpty() ? null : stack.UTF8(engineName)) // stack.UTF8("No Engine")
					.engineVersion(VK_MAKE_VERSION(1, 0, 0))
					.apiVersion(VK_API_VERSION_1_3);

			final PointerBuffer requiredExtensions = getRequiredExtensions(stack);

			final VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
					.sType$Default()
					.pApplicationInfo(vkAppInto)
					.ppEnabledExtensionNames(requiredExtensions);
			if (enableValidationLayers) {
				final PointerBuffer pValidationLayers = stack.mallocPointer(validationLayers.size());
				for (int a = 0; a < validationLayers.size(); ++a) {
					pValidationLayers.put(a, stack.UTF8(validationLayers.get(a)));
				}

				createInfo.ppEnabledLayerNames(pValidationLayers);
			} else {
				createInfo.ppEnabledLayerNames(null);
			}

			final PointerBuffer pInstance = stack.mallocPointer(1);
			final int result = vkCreateInstance(createInfo, null, pInstance);
			final long instance = pInstance.get(0);
			if (result != VK_SUCCESS) {
				throw new AssertionError("Failed to create VkInstance: " + VkHelper.translateVulkanResult(result));
			}
			vkInstance = new VkInstance(instance, createInfo);
		}
	}

	/**
	 * returns number of physical devices with Vulkan support
	 */
	public int getNumberOfVulkanPhysicalDevices() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer pPhysicalDeviceCount = stack.mallocInt(1);
			int result = vkEnumeratePhysicalDevices(vkInstance, pPhysicalDeviceCount, null);
			if (result != VK_SUCCESS) {
				throw new AssertionError("Failed to get number of physical devices: " + VkHelper.translateVulkanResult(result));
			}
			final int deviceCount = pPhysicalDeviceCount.get(0);
			if (deviceCount == 0) {
				throw new AssertionError("Failed to find physical devices with Vulkan support!");
			}
			return deviceCount;
		}
	}

	private void pickPhysicalDevice() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
//			// get GPU count
//			final IntBuffer pPhysicalDeviceCount = stack.mallocInt(1);
//			int result = vkEnumeratePhysicalDevices(vkInstance, pPhysicalDeviceCount, null);
//			if (result != VK_SUCCESS) {
//				throw new AssertionError("Failed to get number of physical devices: " + VkHelper.translateVulkanResult(result));
//			}
//			final int deviceCount = pPhysicalDeviceCount.get(0);
//			if (deviceCount == 0) {
//				throw new AssertionError("Failed to find physical devices with Vulkan support!");
//			}
//			System.out.printf("🔷 Found %d physical devices with Vulkan support.\n", deviceCount);
			final int deviceCount = getNumberOfVulkanPhysicalDevices();
			final IntBuffer pPhysicalDeviceCount = stack.mallocInt(1);
			pPhysicalDeviceCount.put(0, deviceCount);

			final PointerBuffer pPhysicalDevice = stack.mallocPointer(deviceCount);
			final int result = vkEnumeratePhysicalDevices(vkInstance, pPhysicalDeviceCount, pPhysicalDevice);

			final long physicalDevice = getBestPhysicalDevice(deviceCount, pPhysicalDevice);
			if (physicalDevice == 0) {
				throw new RuntimeException("Cannot find suitable physical device!");
			}
			if (result != VK_SUCCESS) {
				throw new AssertionError("Failed to get physical devices: " + VkHelper.translateVulkanResult(result));
			}
			this.vkPhysicalDevice = new VkPhysicalDevice(physicalDevice, vkInstance);
		}
	}

	private void createLogicalDevice() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// using 2 different queues kinda breaks RTSS overlay for some reason
			final List<VkQueueIndexInfo> queueIndices = findQueueIndices(vkPhysicalDevice, stack);

			// queue creation
			final FloatBuffer queuePriorities = stack.floats(0.0f);
			final VkDeviceQueueCreateInfo.Buffer queueCreateInfoBuffer = VkDeviceQueueCreateInfo.malloc(queueIndices.size(), stack);
			int index = 0;
			System.out.printf("🔷 Creating %d queues\n", queueIndices.size());
			for (VkQueueIndexInfo queueInfo : queueIndices) {
				final VkDeviceQueueCreateInfo queueCreateInfo = VkDeviceQueueCreateInfo.calloc(stack)
						.sType$Default()
						.queueFamilyIndex(queueInfo.queueIndex())
						.pQueuePriorities(queuePriorities);
				queueCreateInfoBuffer.put(index, queueCreateInfo);
				System.out.printf("\tqueueFamilyIndex: %d\n", queueInfo.queueIndex());
				index++;
			}
			// queue creation

			final int requiredDeviceExtensionsCount = requiredDeviceExtensions.size();
			final PointerBuffer extensions = stack.mallocPointer(requiredDeviceExtensionsCount);
			for (String name : requiredDeviceExtensions) {
				extensions.put(stack.UTF8(name));
			}
			extensions.flip();

			try (MemoryStack stack2 = MemoryStack.stackPush()) {
				// check feature support
				final VkPhysicalDeviceVulkan13Features deviceVulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack2)
						.sType$Default();

				final VkPhysicalDeviceVulkan11Features deviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack2)
						.sType$Default()
						.pNext(deviceVulkan13Features.address());

				final VkPhysicalDeviceFeatures2 deviceFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack2)
						.sType$Default()
						.pNext(deviceVulkan11Features.address());

				vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, deviceFeatures2);

				final VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack2);
				vkGetPhysicalDeviceFeatures(vkPhysicalDevice, deviceFeatures);

				System.out.printf("shader draw parameters: %b\n", deviceVulkan11Features.shaderDrawParameters());
				System.out.printf("dynamic rendering: %b\n", deviceVulkan13Features.dynamicRendering());
				System.out.printf("synchronization2: %b\n", deviceVulkan13Features.synchronization2());
				System.out.printf("sampler anisotropy: %b\n", deviceFeatures.samplerAnisotropy());
				System.out.printf("multiDrawIndirect: %b\n", deviceFeatures.multiDrawIndirect());
			}

			final VkPhysicalDeviceVulkan13Features deviceVulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack)
					.sType$Default();

			final VkPhysicalDeviceVulkan11Features deviceVulkan11Features = VkPhysicalDeviceVulkan11Features.calloc(stack)
					.sType$Default()
					.pNext(deviceVulkan13Features.address());

			final VkPhysicalDeviceFeatures2 deviceFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack)
					.sType$Default()
					.pNext(deviceVulkan11Features.address());

			deviceVulkan11Features.shaderDrawParameters(true);
			deviceVulkan13Features.dynamicRendering(true);
			deviceVulkan13Features.synchronization2(true);
			deviceFeatures2.features().samplerAnisotropy(true);
			deviceFeatures2.features().multiDrawIndirect(true);

			// can be malloc() only if all fields are explicitly set
			// otherwise use calloc()
			// generally, if you allocate memory which will be immediately rewritten - use malloc
			// for structures that you will fill yourself - use calloc (unless you fill the whole structure)
			final VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
					.sType$Default()
					.pQueueCreateInfos(queueCreateInfoBuffer)
					.ppEnabledExtensionNames(extensions)
					.pEnabledFeatures(null) // deviceFeatures or null if pNext is not null
					.pNext(deviceFeatures2.address()); // deviceFeatures2
			if (enableValidationLayers) {
				final PointerBuffer pValidationLayers = stack.mallocPointer(validationLayers.size());
				for (int a = 0; a < validationLayers.size(); ++a) {
					pValidationLayers.put(a, stack.UTF8(validationLayers.get(a)));
				}
				createInfo.ppEnabledLayerNames(pValidationLayers);
			} else {
				createInfo.ppEnabledLayerNames(null);
			}

			final PointerBuffer pDevice = stack.mallocPointer(1);
			final int result = vkCreateDevice(vkPhysicalDevice, createInfo, null, pDevice);
			if (result != VK_SUCCESS) {
				throw new AssertionError("Failed to create logical device! Error code: " + VkHelper.translateVulkanResult(result));
			}
			this.vkLogicalDevice = new VkDevice(pDevice.get(0), vkPhysicalDevice, createInfo);
			System.out.printf("🔷 Logical device created: %d\n", vkLogicalDevice.address());

			// real queue creation
			vkQueueDataList.clear();
			for (VkQueueIndexInfo queueInfo : queueIndices) {
				final PointerBuffer pQueue = stack.mallocPointer(1);
				vkGetDeviceQueue(vkLogicalDevice, queueInfo.queueIndex(), 0, pQueue);
				final long vkQueueHandle = pQueue.get(0);
				final VkQueue vkQueue = new VkQueue(vkQueueHandle, vkLogicalDevice);
				System.out.printf("🔷 vkGetDeviceQueue queue type: %s | index: %d | present: %b | handle: %s\n", VkHelper.translateQueueBit(queueInfo.queueType()), queueInfo.queueIndex(), queueInfo.presentSupport(), String.format("0x%08x", vkQueueHandle));
				this.vkQueueDataList.add(new VkQueueInfo(vkLogicalDevice, vkQueue, queueInfo));
			}
		}
	}

	public static void enumerateAndPrintDeviceExtensions() {
		// VK_EXT_descriptor_buffer
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer ip = stack.mallocInt(1);
			vkEnumerateInstanceExtensionProperties((String) null, ip, null);

			if (ip.get(0) != 0) {
				final VkExtensionProperties.Buffer instance_extensions = VkExtensionProperties.malloc(ip.get(0), stack);
				vkEnumerateInstanceExtensionProperties((String) null, ip, instance_extensions);

				System.out.println("Supported extensions:");
				for (int i = 0; i < ip.get(0); i++) {
					instance_extensions.position(i);
					System.out.println(instance_extensions.extensionNameString());
				}
			}
		}
	}

	private VkQueueInfo getQueue(int type, boolean getWithPresent) {
		for (VkQueueInfo queue2 : vkQueueDataList) {
			if ((queue2.getQueueInfo().queueType() & type) == type) {
				if (getWithPresent) {
					if (queue2.getQueueInfo().presentSupport()) {
						return queue2;
					} else {
						return null;
					}
				}
				return queue2;
			}
		}
		return null;
	}

	public VkQueueInfo getVkGraphicsQueue() {
		for (VkQueueInfo queue2 : vkQueueDataList) {
			if ((queue2.getQueueInfo().queueType() & VK_QUEUE_GRAPHICS_BIT) == VK_QUEUE_GRAPHICS_BIT) {
				return queue2;
			}
		}
		return null;
	}

	public VkQueueInfo getVkPresentQueue() {
		for (VkQueueInfo queue2 : vkQueueDataList) {
			if (queue2.getQueueInfo().presentSupport()) {
				return queue2;
			}
		}
		return null;
	}

	public VkQueueInfo getVkTransferQueue() {
		for (VkQueueInfo queue2 : vkQueueDataList) {
			if ((queue2.getQueueInfo().queueType() & VK_QUEUE_TRANSFER_BIT) == VK_QUEUE_TRANSFER_BIT) {
				return queue2;
			}
		}
		return null;
	}

	private List<VkQueueIndexInfo> findQueueIndices(VkPhysicalDevice device, MemoryStack stack) {
		final IntBuffer pQueueFamilyCount = stack.mallocInt(1);
		vkGetPhysicalDeviceQueueFamilyProperties(device, pQueueFamilyCount, null);
		final int queueFamilyCount = pQueueFamilyCount.get(0);
		final VkQueueFamilyProperties.Buffer vkQueueFamilyPropertiesBuffer = VkQueueFamilyProperties.malloc(queueFamilyCount, stack);
		vkGetPhysicalDeviceQueueFamilyProperties(device, pQueueFamilyCount, vkQueueFamilyPropertiesBuffer);

		final List<VkQueueIndexInfo> queueInfoList = new ArrayList<>();
		System.out.printf("🔷 vkGetPhysicalDeviceQueueFamilyProperties queueFamilyCount: %d\n", queueFamilyCount);
		final int[] presentSupportRef = new int[1];
		for (int index = 0; index < queueFamilyCount; ++index) {
			final VkQueueFamilyProperties queueFamilyProperties = vkQueueFamilyPropertiesBuffer.get(index);
			final int flags = queueFamilyProperties.queueFlags();
			vkGetPhysicalDeviceSurfaceSupportKHR(device, index, vkSurface, presentSupportRef);
			final int presentSupport = presentSupportRef[0];

			if ((flags & VK_QUEUE_GRAPHICS_BIT) == VK_QUEUE_GRAPHICS_BIT) {
				if (VkQueueIndexInfo.getByIndex(queueInfoList, index) == null) {
					queueInfoList.add(new VkQueueIndexInfo(index, false, flags));
				}
			}

			if ((flags & VK_QUEUE_GRAPHICS_BIT) != VK_QUEUE_GRAPHICS_BIT && presentSupport == 1) {
				if (VkQueueIndexInfo.getByIndex(queueInfoList, index) == null) {
					queueInfoList.add(new VkQueueIndexInfo(index, true, flags));
				}
			}

			if ((flags & VK_QUEUE_TRANSFER_BIT) == VK_QUEUE_TRANSFER_BIT) {
				if (VkQueueIndexInfo.getByIndex(queueInfoList, index) == null) {
					queueInfoList.add(new VkQueueIndexInfo(index, false, flags));
				}
			}
			System.out.printf("🔷 Queue %d presentSupport %d flags %d - %s\n", index, presentSupport, flags, VkHelper.translateQueueBit(flags));
		}
		System.out.printf("🔷 Selected queues:\n");
		for (VkQueueIndexInfo queueInfo : queueInfoList) {
			System.out.printf("\tindex: %d | present support: %b | type: %s\n", queueInfo.queueIndex(), queueInfo.presentSupport(), VkHelper.translateQueueBit(queueInfo.queueType()));
		}
		return queueInfoList;
	}

	private PointerBuffer getRequiredExtensions(MemoryStack stack) {
		final PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
		if (requiredExtensions == null) {
			throw new AssertionError("Failed to get required instance extensions!");
		}
		final PointerBuffer ppEnabledExtensionNames = stack.mallocPointer(requiredExtensions.remaining());
		ppEnabledExtensionNames.put(requiredExtensions);
		ppEnabledExtensionNames.flip();

		for (int index = 0; index < requiredExtensions.capacity(); ++index) {
			long pointer = requiredExtensions.get(index);
			String string = memUTF8(pointer);
			System.out.printf("Required extension [%d] %s\n", index, string);
		}

		return ppEnabledExtensionNames;
	}

	private long getBestPhysicalDevice(int deviceCount, PointerBuffer pPhysicalDevice) {
		final List<Pair<Integer, Long>> candidates = new ArrayList<>();
		for (int a = 0; a < deviceCount; ++a) {
			long physicalDevice = pPhysicalDevice.get();
			int score = rateDeviceSuitability(physicalDevice);
			candidates.add(new Pair<>(score, physicalDevice));
		}
		int maxScore = Integer.MIN_VALUE;
		long bestDevice = 0;
		for (Pair<Integer, Long> pair : candidates) {
			if (pair.left() > maxScore && pair.left() != 0) {
				maxScore = pair.left();
				bestDevice = pair.right();
			}
		}
		return bestDevice;
	}

	private int rateDeviceSuitability(long physicalDevice) {
		final VkPhysicalDevice device = new VkPhysicalDevice(physicalDevice, vkInstance);
		int score = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.malloc(stack);
			vkGetPhysicalDeviceProperties(device, deviceProperties);
			final VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.malloc(stack);
			vkGetPhysicalDeviceFeatures(device, deviceFeatures);

			if (deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
				score += 5000;
			} else if (deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
				score += 100;
			}

			score += deviceProperties.limits().maxImageDimension2D();

			if (!deviceFeatures.geometryShader()) {
				score = 0;
				System.err.print("Bad Device. No geometry shader support found.\n");
			}

			final List<VkQueueIndexInfo> queueIndices = findQueueIndices(device, stack);
			final VkQueueIndexInfo graphicsQueueInfo = VkQueueIndexInfo.getByType(queueIndices, VK_QUEUE_GRAPHICS_BIT);
			final VkQueueIndexInfo presentQueueInfo = VkQueueIndexInfo.getWithPresent(queueIndices);
//			final VkQueueIndexInfo transferQueueInfo = VkQueueIndexInfo.getByType(queueIndices, VK_QUEUE_TRANSFER_BIT);
			if (graphicsQueueInfo == null) {
				score = 0;
				System.err.print("Bad Device. Doesn't have queue with 'VK_QUEUE_GRAPHICS_BIT' bit!\n");
			}
			if (presentQueueInfo == null) {
				score = 0;
				System.err.print("Bad Device. Doesn't have present queue!\n");
			}

			// check if device supports important extensions
			final boolean extensionsSupported = checkDeviceExtensionSupport(device, stack);
			if (!extensionsSupported) {
				score = 0;
				System.err.print("Bad. Device doesn't support all required extensions!\n");
			}
			if (extensionsSupported) {
				final SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device, stack);
				final boolean swapChainSupportFound = swapChainSupport.surfaceFormatCount != 0 && swapChainSupport.presentModeCount != 0;
				if (!swapChainSupportFound) {
					score = 0;
					System.err.print("Bad. Device doesn't have any surface formats and present modes\n");
				}
				swapChainSupport.print();
			}
			if (!deviceFeatures.samplerAnisotropy()) {
				score = 0;
				System.err.print("Bad. Device doesn't support anisotropy!\n");
			}

			printDeviceProperties(deviceProperties);
			printDeviceFeatures(deviceFeatures);

			System.out.printf("Score for device %s is: %d \n", deviceProperties.deviceNameString(), score);
			return score;
		}
	}

	private boolean checkDeviceExtensionSupport(VkPhysicalDevice device, MemoryStack stack) {
		final int[] extensionCountRef = new int[1];
		vkEnumerateDeviceExtensionProperties(device, (ByteBuffer) null, extensionCountRef, null);
		final int extensionCount = extensionCountRef[0];

		final VkExtensionProperties.Buffer vkExtensionProperties = VkExtensionProperties.malloc(extensionCount, stack);
		vkEnumerateDeviceExtensionProperties(device, (ByteBuffer) null, extensionCountRef, vkExtensionProperties);

		int found = 0;
		for (int a = 0; a < extensionCount; ++a) {
			VkExtensionProperties extensionProperties = vkExtensionProperties.get(a);
			if (requiredDeviceExtensions.contains(extensionProperties.extensionNameString())) {
				found++;
			}
		}

		return found == requiredDeviceExtensions.size();
	}

	public SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {
		final SwapChainSupportDetails details = new SwapChainSupportDetails();

		details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, vkSurface, details.capabilities);
		final IntBuffer formatCountRef = stack.mallocInt(1);
		vkGetPhysicalDeviceSurfaceFormatsKHR(device, vkSurface, formatCountRef, null);
		final int formatCount = formatCountRef.get(0);

		if (formatCount != 0) {
			details.surfaceFormats = VkSurfaceFormatKHR.malloc(formatCount, stack);
			details.surfaceFormatCount = formatCount;
			vkGetPhysicalDeviceSurfaceFormatsKHR(device, vkSurface, formatCountRef, details.surfaceFormats);
		}

		final int[] presentModeCountRef = new int[1];
		vkGetPhysicalDeviceSurfacePresentModesKHR(device, vkSurface, presentModeCountRef, null);
		final int presentModeCount = presentModeCountRef[0];

		if (presentModeCount != 0) {
			details.presentModes = new int[presentModeCount];
			details.presentModeCount = presentModeCount;
			vkGetPhysicalDeviceSurfacePresentModesKHR(device, vkSurface, presentModeCountRef, details.presentModes);
		}

		return details;
	}

	private boolean checkValidationLayerSupport() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final IntBuffer pLayerCount = stack.mallocInt(1);
			vkEnumerateInstanceLayerProperties(pLayerCount, null);
			final int layerCount = pLayerCount.get(0);

			final VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount, stack);
			vkEnumerateInstanceLayerProperties(pLayerCount, availableLayers);

			boolean layerFound = false;
			for (String layerName : validationLayers) {
				for (VkLayerProperties layerProperties : availableLayers) {
					System.out.printf("Layer: %s\n", layerProperties.layerNameString());
					if (layerName.equals(layerProperties.layerNameString())) {
						layerFound = true;
						break;
					}
				}
			}
			return layerFound;
		}
	}

	private void createSwapChain() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final SwapChainSupportDetails swapChainSupport = querySwapChainSupport(vkPhysicalDevice, stack);

			final VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.surfaceFormats);
			final int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
			final VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);
			final int imageCount;
			final int minImageCount = swapChainSupport.capabilities.minImageCount();
			final int maxImageCount = swapChainSupport.capabilities.maxImageCount();
			imageCount = Math.max(swapChainImageCount, Math.min(minImageCount, maxImageCount));
			vkSwapChainImageFormat = surfaceFormat.format();
			if (vkExtent2D != null) {
				vkExtent2D.width(extent.width());
				vkExtent2D.height(extent.height());
				extent.free();
			} else {
				vkExtent2D = extent;
			}
			System.out.printf("Chosen format: %s\n", VkHelper.translateSurfaceFormatBit(vkSwapChainImageFormat));

			final VkSwapchainCreateInfoKHR swapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.calloc(stack)
					.sType$Default()
					.surface(vkSurface)
					.minImageCount(imageCount)
					.imageFormat(vkSwapChainImageFormat)
					.imageColorSpace(surfaceFormat.colorSpace())
					.imageExtent(extent)
					.imageArrayLayers(1)
					.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
					.preTransform(swapChainSupport.capabilities.currentTransform()) // image rotation/flip
					.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) // non transparent framebuffer
					.presentMode(presentMode)
					.clipped(true)
					.oldSwapchain(NULL);

			final List<VkQueueInfo> queues = getVkQueueDataList();
			System.out.printf("🔷 queue count: %d\n", queues.size());

			final IntBuffer queueFamilyIndices = stack.mallocInt(queues.size());
			int index = 0;
			for (VkQueueInfo queue2 : queues) {
				System.out.printf("\t[swapchainCreateInfoKHR] queueFamilyIndices [%d]: %d\n", index, queue2.getQueueInfo().queueIndex());
				queueFamilyIndices.put(index, queue2.getQueueInfo().queueIndex());
				index++;
			}
			if (queues.size() > 1) {
				swapchainCreateInfoKHR.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
						.queueFamilyIndexCount(queues.size())
						.pQueueFamilyIndices(queueFamilyIndices);
				System.out.println("🔷 SwapChain ImageSharingMode: VK_SHARING_MODE_CONCURRENT");
			} else {
				swapchainCreateInfoKHR.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
						.queueFamilyIndexCount(queues.size())
						.pQueueFamilyIndices(queueFamilyIndices);
				System.out.println("🔷 SwapChain ImageSharingMode: VK_SHARING_MODE_EXCLUSIVE");
			}

			final LongBuffer swapChainRef = stack.mallocLong(1);
			if (vkCreateSwapchainKHR(vkLogicalDevice, swapchainCreateInfoKHR, null, swapChainRef) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create swap chain!");
			}
			vkSwapchainKHR = swapChainRef.get(0);

			final IntBuffer imageCountInSwapChainRef = stack.mallocInt(1);
			vkGetSwapchainImagesKHR(vkLogicalDevice, vkSwapchainKHR, imageCountInSwapChainRef, null);
			vkSwapChainImages = MemoryUtil.memAllocLong(imageCountInSwapChainRef.get(0)); // allocating not on stack is intentional
			vkGetSwapchainImagesKHR(vkLogicalDevice, vkSwapchainKHR, imageCountInSwapChainRef, vkSwapChainImages);

			System.out.printf("🔷 SwapChainImageCount: %d | framebuffers count: %d\n", imageCountInSwapChainRef.get(0), swapChainImageCount);
		}
	}

	private void createImageViews() {
		final int swapChainImagesCount = vkSwapChainImages.capacity();
		vkSwapChainImageViews = new long[swapChainImagesCount];

		for (int a = 0; a < swapChainImagesCount; ++a) {
			vkSwapChainImageViews[a] = VkHelper.createTextureImageView(vkLogicalDevice, vkSwapChainImages.get(a), vkSwapChainImageFormat);
		}
	}

	private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
		int formatIndex = 0;
		System.out.println("Choosing SwapChainSurfaceFormat...");
		for (VkSurfaceFormatKHR availableFormat : availableFormats) {
			// gives error
//			if (availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB) {
//				return availableFormat;
//			}

			try (MemoryStack stack = MemoryStack.stackPush()) {
				final VkImageFormatProperties imageFormatProperties = VkImageFormatProperties.malloc(stack);
				vkGetPhysicalDeviceImageFormatProperties(vkPhysicalDevice,
						availableFormat.format(),
						VK_IMAGE_TYPE_2D,
						VK_IMAGE_TILING_OPTIMAL,
						VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
						0,
						imageFormatProperties);

				final int maxArrayLayers = imageFormatProperties.maxArrayLayers();
				System.out.printf("[%d] VkSurfaceFormatKHR colorSpace: %d %s | format: %d %s \n",
						formatIndex,
						availableFormat.colorSpace(),
						VkHelper.translateColorSpace(availableFormat.colorSpace()),
						availableFormat.format(),
						VkHelper.translateSurfaceFormatBit(availableFormat.format()));

				if (maxArrayLayers != 0 &&
						imageFormatProperties.maxExtent().width() > 0 &&
						imageFormatProperties.maxExtent().height() > 0) {
					return availableFormat;
				}
				formatIndex++;
			}
		}

		return availableFormats.get(0);
	}

	private int chooseSwapPresentMode(int[] availablePresentModes) {
		// return VK_PRESENT_MODE_MAILBOX_KHR; // "triple buffering" vsync but with less input lag
		// return VK_PRESENT_MODE_IMMEDIATE_KHR; // no vsync
		for (int presentMode : availablePresentModes) {
			if (vsync) {
				if (presentMode == VK_PRESENT_MODE_MAILBOX_KHR ||
						presentMode == VK_PRESENT_MODE_FIFO_KHR) {
					return presentMode;
				}
			} else if (presentMode == VK_PRESENT_MODE_IMMEDIATE_KHR) {
				return presentMode;
			}
		}
		System.err.printf("Failed to find required present mode! Using the default VK_PRESENT_MODE_FIFO_KHR\n");
		return VK_PRESENT_MODE_FIFO_KHR;
	}

	private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
		if (capabilities.currentExtent().width() != Integer.MAX_VALUE) {
			final VkExtent2D extent2D = VkExtent2D.malloc(); // allocating not on stack is intentional
			extent2D.width(capabilities.currentExtent().width());
			extent2D.height(capabilities.currentExtent().height());
			return extent2D;
		} else {
			final IntBuffer fbWidth = MemoryUtil.memAllocInt(1);
			final IntBuffer fbHeight = MemoryUtil.memAllocInt(1);
			windowVK.getFramebufferSize(fbWidth, fbHeight);

			final VkExtent2D actualExtent = VkExtent2D.malloc(); // allocating not on stack is intentional
			actualExtent.width(Math.clamp(fbWidth.get(0), capabilities.minImageExtent().width(), capabilities.maxImageExtent().width()));
			actualExtent.height(Math.clamp(fbHeight.get(0), capabilities.minImageExtent().height(), capabilities.maxImageExtent().height()));
			memFree(fbWidth);
			memFree(fbHeight);
			return actualExtent;
		}
	}

	private void createFramesInFlight(int framesInFlightCount) {
		frameInFlights.clear();
		for (int a = 0; a < framesInFlightCount; ++a) {
			final FrameInFlight frameInFlight = new FrameInFlight(vkLogicalDevice);
//			if (!useDynamicRendering) {
//				framebuffer.createFramebuffer(vkLogicalDevice, vkExtent2D, vkRenderPass, vkSwapChainImages.get(a), vkSwapChainImageViews[a]);
//			}
			frameInFlights.add(frameInFlight);
		}
	}

	public int acquireImage() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
			final FrameInFlight frameInFlight = frameInFlights.get(currentFrame);

			if (shouldRecreateSwapChain) {
				recreateSwapChain(stack);
			}

			// takes 1.3 - 1.5 ms
//			long time = System.nanoTime();
			vkWaitForFences(vkLogicalDevice, frameInFlight.getVkInFlightFence(), true, UINT64_MAX);
//			log_perf("vkWaitForFences", System.nanoTime() - time);
//			while (vkWaitForFences(vkLogicalDevice, framebuffer.getVkInFlightFence(), true, 100_000) == VK_TIMEOUT) {
//				;
//			}

			// takes 0.05 - 0.1 ms
//			time = System.nanoTime();
			final int acquireNextImageKHRResult = vkAcquireNextImageKHR(vkLogicalDevice, vkSwapchainKHR, UINT64_MAX,
					frameInFlight.getVkImageAvailableSemaphore(), VK_NULL_HANDLE, pCurrentFrameIndex);
			final int imageIndex = pCurrentFrameIndex.get(0);
//			log_perf("vkAcquireNextImageKHR", System.nanoTime() - time);

//			System.out.printf("currentFrame: %d imageIndex: %d\n", currentFrame, imageIndex);

			if (acquireNextImageKHRResult == VK_ERROR_OUT_OF_DATE_KHR) {
				recreateSwapChain(stack);
				return -1;
			} else if (acquireNextImageKHRResult != VK_SUCCESS && acquireNextImageKHRResult != VK_SUBOPTIMAL_KHR) {
				throw new RuntimeException("Failed to acquire swap chain image!");
			}
			vkResetFences(vkLogicalDevice, frameInFlight.getVkInFlightFence());
			return imageIndex;
		}
	}

	private void recreateSwapChain(MemoryStack stack) {
		this.shouldRecreateSwapChain = false;

		final IntBuffer fbWidth = stack.mallocInt(1);
		final IntBuffer fbHeight = stack.mallocInt(1);
		windowVK.getFramebufferSize(fbWidth, fbHeight);
		if (fbWidth.get(0) == 0 ||
				fbHeight.get(0) == 0) {
			sleep(100);
			return;
		}
		System.out.printf("SwapChain creation. Framebuffer %d %d, extent %d %d\n", fbWidth.get(0), fbHeight.get(0), vkExtent2D.width(), vkExtent2D.height());

		vkDeviceWaitIdle(vkLogicalDevice);

		// cleanupSwapChain();
		// destroy framebuffers and their image view
//		if (!useDynamicRendering) {
//			for (FrameInFlight framebuffer : frameInFlights) {
//				framebuffer.onRecreateSwapChain_1(vkLogicalDevice);
//			}
//		}
		vkDestroySwapchainKHR(vkLogicalDevice, vkSwapchainKHR, null);
		//

		createSwapChain();
		createImageViews();

		// createFramebuffers();
		// create framebuffers and image views again
//		if (!useDynamicRendering) {
//			for (int a = 0; a < framebufferList.size(); ++a) {
//				final Framebuffer framebuffer = framebufferList.get(a);
//				framebuffer.onRecreateSwapChain_2(vkLogicalDevice, vkExtent2D, vkRenderPass, vkSwapchainKHR, vkSwapChainImages.get(a), vkSwapChainImageViews[a]);
//			}
//		}
		//

		// remove this and enable Riva Tuner Overlay for really cool visual glitch, and also trigger this function
		// invalidate cached command buffers
//		for (VkMTDRAW.DrawCommand drawCommand : drawCommands) {
//			drawCommand.setDrawAndUploadCommandCached(false);
//			drawCommand.setDrawCommandCached(false);
//		}
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception ignored) {

		}
	}

	private void printDeviceProperties(VkPhysicalDeviceProperties deviceProperties) {
		System.out.println("🔷 Device Properties");
		System.out.printf("\tDevice ID: %d \n", deviceProperties.deviceID());
		System.out.printf("\tDevice name string: %s \n", deviceProperties.deviceNameString());
		System.out.printf("\tDevice type: %s \n", VkHelper.translatePhysicalDeviceType(deviceProperties.deviceType()));
		System.out.printf("\tVendor ID: %d \n", deviceProperties.vendorID());

		final int apiVersion = deviceProperties.apiVersion();
		System.out.printf("\tVulkan API version: %d.%d.%d \n", VK_API_VERSION_MAJOR(apiVersion), VK_API_VERSION_MINOR(apiVersion), VK_API_VERSION_PATCH(apiVersion));
		final int driverVersion = deviceProperties.driverVersion();
		System.out.printf("\tVulkan Driver version: %d.%d.%d \n", VK_VERSION_MAJOR(driverVersion), VK_VERSION_MINOR(driverVersion), VK_VERSION_PATCH(driverVersion));

		System.out.printf("\tMax image dimension 2d: %d \n", deviceProperties.limits().maxImageDimension2D());
		System.out.printf("\tMax framebuffer layers: %d \n", deviceProperties.limits().maxFramebufferLayers());
		System.out.printf("\tMax framebuffer color sample counts: %d \n", deviceProperties.limits().framebufferColorSampleCounts());
		System.out.printf("\tMax framebuffer depth sample counts: %d \n", deviceProperties.limits().framebufferDepthSampleCounts());
		System.out.printf("\tMax framebuffer no attachment sample counts: %d \n", deviceProperties.limits().framebufferNoAttachmentsSampleCounts());
		System.out.printf("\tMax framebuffer stencil sample counts: %d \n", deviceProperties.limits().framebufferStencilSampleCounts());
		System.out.printf("\tMax viewports: %d \n", deviceProperties.limits().maxViewports());
		System.out.printf("\tMax image layers arrays: %d \n", deviceProperties.limits().maxImageArrayLayers());
		System.out.printf("\tMax vertex input attributes: %d \n", deviceProperties.limits().maxVertexInputAttributes());
		System.out.printf("\tMax vertex input bindings: %d \n", deviceProperties.limits().maxVertexInputBindings());
		System.out.printf("\tMax fragment input components: %d \n", deviceProperties.limits().maxFragmentInputComponents());
		System.out.printf("\tMax fragment output attachments: %d \n", deviceProperties.limits().maxFragmentOutputAttachments());

		final IntBuffer computeGroupCount = deviceProperties.limits().maxComputeWorkGroupCount();
		final int computeGroupCountRemaining = computeGroupCount.remaining();
		System.out.print("\tMax compute work group count: ");
		for (int a = 0; a < computeGroupCountRemaining; ++a) {
			System.out.printf("%d ", computeGroupCount.get());
		}
		System.out.print("\n");

		final IntBuffer computeGroupSize = deviceProperties.limits().maxComputeWorkGroupSize();
		final int computeGroupSizeRemaining = computeGroupSize.remaining();
		System.out.print("\tMax compute work group size: ");
		for (int a = 0; a < computeGroupSizeRemaining; ++a) {
			System.out.printf("%d ", computeGroupSize.get());
		}
		System.out.print("\n");
		System.out.printf("\tMax compute shared memory size: %d \n", deviceProperties.limits().maxComputeSharedMemorySize());
		System.out.printf("\tMax compute work group invocations: %d \n", deviceProperties.limits().maxComputeWorkGroupInvocations());
	}

	private void printDeviceFeatures(VkPhysicalDeviceFeatures deviceFeatures) {
		System.out.println("🔷 Device Features");
		System.out.printf("\tMultiViewport: %b\n", deviceFeatures.multiViewport());
		System.out.printf("\tTessellationShader: %b\n", deviceFeatures.tessellationShader());
		System.out.printf("\tGeometryShader: %b\n", deviceFeatures.geometryShader());
		System.out.printf("\tMultiDrawIndirect: %b\n", deviceFeatures.multiDrawIndirect());
		System.out.printf("\tDrawIndirectFirstInstance: %b\n", deviceFeatures.drawIndirectFirstInstance());
		System.out.printf("\tShaderFloat64: %b\n", deviceFeatures.shaderFloat64());
		System.out.printf("\tShaderInt16: %b\n", deviceFeatures.shaderInt16());
		System.out.printf("\tShaderInt64: %b\n", deviceFeatures.shaderInt64());
	}

	public void destroy() {
		vkDeviceWaitIdle(vkLogicalDevice);

		if (vkExtent2D != null) {
			vkExtent2D.free();
			vkExtent2D = null;
		}
		for (long imageView : vkSwapChainImageViews) {
			vkDestroyImageView(vkLogicalDevice, imageView, null);
		}
		memFree(vkSwapChainImages);
		memFree(pCurrentFrameIndex);
		vkDestroySwapchainKHR(vkLogicalDevice, vkSwapchainKHR, null);

		for (FrameInFlight frameInFlight : frameInFlights) {
			frameInFlight.free(vkLogicalDevice);
		}
		frameInFlights.clear();

		for (VkQueueInfo vkQueueInfo : vkQueueDataList) {
			vkQueueInfo.destroyCommandPool();
		}

		vkDestroyDevice(vkLogicalDevice, null);
		vkDestroySurfaceKHR(vkInstance, vkSurface, null);
		vkDestroyInstance(vkInstance, null);
	}

	public void renderNothing(int imageIndex) {
		final FrameInFlight frameInFlight = frameInFlights.get(currentFrame);
		final VkCommandBuffer vkCommandBuffer = frameInFlight.getVkCommandBuffer();
		VkHelper.beginCommandBuffer(vkCommandBuffer);
		recordTestCommandBuffer(vkCommandBuffer, imageIndex);
		VkHelper.endCommandBuffer(vkCommandBuffer);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final VkQueueInfo graphicsQueue = getVkGraphicsQueue();

			final VkSubmitInfo2 submitInfo = getSubmitInfo(stack, vkCommandBuffer, frameInFlight);
			final VkSubmitInfo2.Buffer submitInfoBuffer = VkSubmitInfo2.calloc(1, stack);
			submitInfoBuffer.put(0, submitInfo);
			final int result = vkQueueSubmit2(graphicsQueue.getQueue(), submitInfoBuffer, frameInFlight.getVkInFlightFence());
			if (result != VK_SUCCESS) {
				throw new RuntimeException("Failed to submit command buffer! " + VkHelper.translateVulkanResult(result));
			}
		}
	}

	public VkSubmitInfo2 getSubmitInfo(MemoryStack stack, VkCommandBuffer commandBuffer, FrameInFlight frameInFlight) {
		final VkSubmitInfo2 submitInfo = VkSubmitInfo2.calloc(stack)
				.sType$Default()
				.pWaitSemaphoreInfos(VkSemaphoreSubmitInfo.calloc(1, stack).put(0, VkSemaphoreSubmitInfo.calloc(stack)))
				.pCommandBufferInfos(VkCommandBufferSubmitInfo.calloc(1, stack).put(0, VkCommandBufferSubmitInfo.calloc(stack)))
				.pSignalSemaphoreInfos(VkSemaphoreSubmitInfo.calloc(1, stack).put(0, VkSemaphoreSubmitInfo.calloc(stack)));

		submitInfo.pWaitSemaphoreInfos().get(0)
				.sType$Default()
				.deviceIndex(0)
				.stageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
				.semaphore(frameInFlight.getVkImageAvailableSemaphore());

		submitInfo.pCommandBufferInfos().get(0)
				.sType$Default()
				.commandBuffer(commandBuffer)
				.deviceMask(0);

		submitInfo.pSignalSemaphoreInfos().get(0)
				.sType$Default()
				.deviceIndex(0)
				.stageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
				.semaphore(frameInFlight.getVkRenderFinishedSemaphore());
		return submitInfo;
	}

	public void recordTestCommandBuffer(VkCommandBuffer commandBuffer, int imageIndex) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			VkHelper.beginRendering(stack, commandBuffer,
					vkSwapChainImages.get(imageIndex),
					vkSwapChainImageViews[imageIndex],
					VkHelper.vkGetClearValue(stack, 0, 0, 0, 255),
					vkExtent2D.width(),
					vkExtent2D.height());

			VkHelper.setViewport(stack, commandBuffer, vkExtent2D.width(), vkExtent2D.height());
			VkHelper.setScissor(stack, commandBuffer, vkExtent2D.width(), vkExtent2D.height());

//          vkGraphicsPipeline.bindPipeline(commandBuffer);

//			vkVertexBuffer.bindVertexBuffer(commandBuffer);
//			vkIndexBuffer.bindIndexBuffer(commandBuffer, VK_INDEX_TYPE_UINT32);

//			final LongBuffer pDescriptorSet = stack.longs(descriptor.getDescriptorSet(imageIndex));
//			vkCmdBindDescriptorSets(commandBuffer,
//					VK_PIPELINE_BIND_POINT_GRAPHICS,
//					vkGraphicsPipeline.getVkPipelineLayout(), 0, pDescriptorSet, null);

//			vkCmdDrawIndexedIndirect(commandBuffer, vkIndirectBuffer.getBufferHandle(), 0, 10, 5 * 4);

			VkHelper.endRendering(stack, commandBuffer, vkSwapChainImages.get(imageIndex));
		}
	}

	public void presentImage() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// Present the swap chain image
			final LongBuffer pSwapChains = stack.longs(vkSwapchainKHR);
			final VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
					.sType$Default()
					.pWaitSemaphores(stack.longs(getFrameInFlights().get(currentFrame).getVkRenderFinishedSemaphore()))
					.swapchainCount(1)
					.pSwapchains(pSwapChains)
					.pImageIndices(pCurrentFrameIndex)
					.pResults(null);

			final VkQueueInfo vkPresentQueue = getVkPresentQueue();

			// takes 0.25 - 0.27 ms
			final int queuePresentKHRResult = nvkQueuePresentKHR(vkPresentQueue.getQueue(), presentInfo.address());
			if (queuePresentKHRResult == VK_ERROR_OUT_OF_DATE_KHR || queuePresentKHRResult == VK_SUBOPTIMAL_KHR) {
				recreateSwapChain(stack);
			} else if (queuePresentKHRResult != VK_SUCCESS) {
				throw new RuntimeException("Failed to present swap chain image!");
			}

			currentFrame = (currentFrame + 1) % swapChainImageCount;
		}
	}
}
