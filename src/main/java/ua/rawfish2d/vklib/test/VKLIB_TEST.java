package ua.rawfish2d.vklib.test;

import org.lwjgl.vulkan.VK13;
import ua.rawfish2d.vklib.WindowVK;
import ua.rawfish2d.vklib.init.VkDeviceInstance;
import ua.rawfish2d.vklib.utils.FPSCounter;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;

public class VKLIB_TEST {
	private WindowVK windowVK;
	private VkDeviceInstance vkDeviceInstance;
	private int screenWidth = 1024;
	private int screenHeight = 768;

	public VKLIB_TEST() {
		windowVK = new WindowVK(screenWidth, screenHeight, "VKLib Test");
		windowVK.showWindow();

		createVulkanContext();

		mainLoop();
		vkDeviceInstance.destroy();
		windowVK.terminate();
	}

	private void mainLoop() {
		windowVK.showWindow();

		final FPSCounter fpsCounter = new FPSCounter();
		while (!windowVK.shouldClose()) {
			glfwPollEvents();
			fpsCounter.pre();
			render();
			fpsCounter.post();
		}
	}

	private void render() {
		int imageIndex = vkDeviceInstance.acquireImage();
		// TODO buffer updating/uploading
		// TODO command buffer recording...
		// TODO command buffer submitting
//		int result = vkQueueSubmit(vkGraphicsQueue, submitInfo, drawCommand.getFramebuffer().getVkInFlightFence());
//		if (result != VK_SUCCESS) {
//			throw new RuntimeException("Failed to submit command buffer! " + VkHelper.translateVulkanResult(result));
//		}

		vkDeviceInstance.renderNothing(imageIndex);

		// image presenting
		vkDeviceInstance.presentImage();
	}

	private void createVulkanContext() {
		vkDeviceInstance = new VkDeviceInstance()
				.applicationName("Vulkan Lib Test")
				.apiVersion(VK13.VK_API_VERSION_1_3)
				.validationLayers(false)
				.vsync(true)
				.swapChainImageCount(3)
				.create(windowVK);
	}

	public static void main(String[] args) {
		new VKLIB_TEST();
	}
}
