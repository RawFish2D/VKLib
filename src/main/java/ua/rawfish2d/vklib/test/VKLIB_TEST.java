package ua.rawfish2d.vklib.test;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import ua.rawfish2d.vklib.*;
import ua.rawfish2d.vklib.attrib.AttribFormat;
import ua.rawfish2d.vklib.attrib.BufferLayout;
import ua.rawfish2d.vklib.init.VkDeviceInstance;
import ua.rawfish2d.vklib.init.VkGraphicsPipeline;
import ua.rawfish2d.vklib.init.data.FrameInFlight;
import ua.rawfish2d.vklib.init.data.VkQueueInfo;
import ua.rawfish2d.vklib.init.descriptor.DescriptorSetUpdate;
import ua.rawfish2d.vklib.init.descriptor.SetLayoutBindingsBuffer;
import ua.rawfish2d.vklib.init.descriptor.VkDescriptorSetLayout;
import ua.rawfish2d.vklib.utils.FPSCounter;
import ua.rawfish2d.vklib.utils.TimeHelper;
import ua.rawfish2d.vklib.utils.VkHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.vulkan.VK10.*;

public class VKLIB_TEST {
	private final WindowVK windowVK;
	private VkDeviceInstance vkDeviceInstance;
	private VkGraphicsPipeline vkGraphicsPipeline;
	private VkTexture vkTexture;
	private VkBuffer vkSSBO;
	private VkBuffer vkVertexBuffer;
	private VkBuffer vkIndexBuffer;
	private IndirectBuffer vkIndirectBuffer;
	private BulletScene bulletScene;
	private int bulletsCount = 20000;
	private int screenWidth = 1024;
	private int screenHeight = 768;

	public VKLIB_TEST() {
		windowVK = new WindowVK();
		windowVK.init();
		windowVK.setTransparentFramebuffer(false);
		windowVK.create(screenWidth, screenHeight, "VKLib Test");
		try {
			windowVK.setKeyCallback(new GLFWKeyCallback() {
				private final TimeHelper inputTimer = new TimeHelper();

				@Override
				public void invoke(long hwnd, int key, int scancode, int action, int mods) {
					if (key == GLFW.GLFW_KEY_F && inputTimer.hasReachedMilli(1000)) {
						inputTimer.reset();

						windowVK.setFullscreen(!windowVK.isFullScreen(), false, screenWidth, screenHeight);
					}
					if (key == GLFW.GLFW_KEY_V && inputTimer.hasReachedMilli(250)) {
						inputTimer.reset();
						vkDeviceInstance.vsync(!vkDeviceInstance.isVsync());
						vkDeviceInstance.recreateSwapChain();
						System.out.printf("Vsync: %b\n", vkDeviceInstance.isVsync());
					}
					if (key == GLFW.GLFW_KEY_SPACE && inputTimer.hasReachedMilli(250)) {
						inputTimer.reset();
						bulletScene.pause = !bulletScene.pause;

						if (bulletScene.pause) {
							System.out.println("Paused");
						} else {
							System.out.println("Unpaused");
						}
					}
					if (key == GLFW.GLFW_KEY_R && inputTimer.hasReachedMilli(250)) {
						inputTimer.reset();
						bulletScene.resetBullets(vkVertexBuffer, vkIndexBuffer, vkSSBO);
					}
					if (key == GLFW.GLFW_KEY_Q && inputTimer.hasReachedMilli(250)) {
						inputTimer.reset();
						if (vkDeviceInstance.queueSubmitMode == VkDeviceInstance.QueueSubmitMode.DEVICE) {
							vkDeviceInstance.queueSubmitMode = VkDeviceInstance.QueueSubmitMode.EXTENSION;

						} else if (vkDeviceInstance.queueSubmitMode == VkDeviceInstance.QueueSubmitMode.EXTENSION) {
							vkDeviceInstance.queueSubmitMode = VkDeviceInstance.QueueSubmitMode.DEVICE_NATIVE;

						} else if (vkDeviceInstance.queueSubmitMode == VkDeviceInstance.QueueSubmitMode.DEVICE_NATIVE) {
							vkDeviceInstance.queueSubmitMode = VkDeviceInstance.QueueSubmitMode.DEVICE;
						}
						System.out.printf("Switched queueSubmitMode to: %s\n", vkDeviceInstance.queueSubmitMode);
					}
				}
			});

			windowVK.setFramebufferSizeCallback(new GLFWFramebufferSizeCallback() {
				@Override
				public void invoke(long window, int width, int height) {
					updateWindowSize(width, height);
				}
			});

			Bullet.BulletSceneConfig.size.set(24f, 24f);
			Bullet.BulletSceneConfig.windowWidth = screenWidth;
			Bullet.BulletSceneConfig.windowHeight = screenHeight;

			createVulkanContext();
			createGraphicsPipeline();

			loadTextures();
			createUniformBuffers();
			createSSBO();
			updateDescriptors();

			createVertexBuffer();
			createIndexBuffer();
			vkIndirectBuffer = IndirectBuffer.createIndirectBuffer(vkDeviceInstance, 10, true);
			createBulletScene();

			windowVK.showWindow();

			mainLoop();

			vkDeviceWaitIdle(vkDeviceInstance.getVkLogicalDevice());

			vkVertexBuffer.destroyAndFreeMemory(vkDeviceInstance);
			vkIndexBuffer.destroyAndFreeMemory(vkDeviceInstance);
			vkIndirectBuffer.destroy(vkDeviceInstance);
			vkTexture.destroy(vkDeviceInstance);
			vkSSBO.destroyAndFreeMemory(vkDeviceInstance);
			vkGraphicsPipeline.destroy(vkDeviceInstance);

			vkDeviceInstance.destroy();
			windowVK.terminate();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Crashed!");
			while (!windowVK.shouldClose()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException ignored) {
					;
				}
				windowVK.pollEvents();
			}
		}
		System.exit(0);
	}

	private void updateWindowSize(int width, int height) {
		System.out.printf("[updateWindowSize] %d %d\n", width, height);
		this.screenWidth = width;
		this.screenHeight = height;
		Bullet.BulletSceneConfig.windowWidth = screenWidth;
		Bullet.BulletSceneConfig.windowHeight = screenHeight;
	}

	private void createBulletScene() {
		final VkDevice vkLogicalDevice = vkDeviceInstance.getVkLogicalDevice();
		final VkQueueInfo vkTransferQueueInfo = vkDeviceInstance.getVkTransferQueue();
		final long commandPool = VkHelper.createCommandPool(vkLogicalDevice, vkTransferQueueInfo.getIndexInfo().queueIndex());
		final VkCommandBuffer commandBuffer = VkHelper.beginSingleTimeCommands(vkLogicalDevice, commandPool);
		bulletScene = new BulletScene(vkDeviceInstance, bulletsCount, getVertexAttrib());
		bulletScene.updateAll(vkVertexBuffer, vkIndexBuffer, vkSSBO);
		bulletScene.uploadBuffers(vkVertexBuffer, vkIndexBuffer, commandBuffer);

		vkIndirectBuffer.reset();
		for (int a = 0; a < 10; ++a) {
			final int step = 2000;
			vkIndirectBuffer.addIndirectCommand(vkDeviceInstance, 6, step, a * 6, 0, step * a);
		}
		vkIndirectBuffer.upload(commandBuffer);

		VkHelper.endSingleTimeCommands(vkLogicalDevice, commandPool, commandBuffer, vkTransferQueueInfo.getQueue());
		vkDestroyCommandPool(vkLogicalDevice, commandPool, null);
	}

	private void createIndexBuffer() {
		// create index buffer
		vkIndexBuffer = new VkBuffer();
		System.out.printf("Creating index buffer:\n");
		vkIndexBuffer.createBuffer(vkDeviceInstance, bulletsCount * 6 * 4, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
		vkIndexBuffer.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		vkIndexBuffer.makeStagingBuffer(vkDeviceInstance, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
	}

	private void createVertexBuffer() {
		final AttribFormat vertexAttribFormat = getVertexAttrib();
		final int vertexBufferSize = vertexAttribFormat.getBufferSize();
		System.out.printf("Vertex buffer size: %d\n", vertexBufferSize);

		// create vertex buffer
		vkVertexBuffer = new VkBuffer();
		System.out.printf("Creating vertex buffer:\n");
		vkVertexBuffer.createBuffer(vkDeviceInstance, vertexBufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
		vkVertexBuffer.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		vkVertexBuffer.makeStagingBuffer(vkDeviceInstance, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
	}

	private void loadTextures() {
		final String filename = "assets\\textures\\bullets0.png";
		vkTexture = new VkTexture();
		vkTexture.loadAndCreateImage(filename, vkDeviceInstance);
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
		vkDeviceInstance.acquireImage();

		final VkCommandBuffer vkCommandBuffer = vkDeviceInstance.startRecording();

		final boolean shouldUpdate = bulletScene.shouldUpdate();
		if (shouldUpdate) {
			bulletScene.updateSSBO(vkSSBO);
			bulletScene.uploadBulletPos(vkSSBO, vkCommandBuffer);

			final FrameInFlight frameInFlight = vkDeviceInstance.getCurrentFrameInFlight();
			final VkBuffer uniformBuffer = frameInFlight.getUniformBuffer();
			final ByteBuffer buffer = uniformBuffer.getStagingBuffer(vkDeviceInstance);
			buffer.putFloat(0, screenWidth);
			buffer.putFloat(4, screenHeight);
			uniformBuffer.uploadFromStagingBuffer(vkCommandBuffer);
			uniformBuffer.uniformBarrier(vkCommandBuffer);
		}

		vkDeviceInstance.beginRendering(vkCommandBuffer);
		vkDeviceInstance.setViewport(vkCommandBuffer);
		vkDeviceInstance.setScissor(vkCommandBuffer);

		vkGraphicsPipeline.bindPipeline(vkCommandBuffer);

		vkVertexBuffer.bindVertexBuffer(vkCommandBuffer);
		vkIndexBuffer.bindIndexBuffer(vkCommandBuffer);

		vkDeviceInstance.bindDescriptorSet(vkCommandBuffer, vkGraphicsPipeline);

		vkCmdDrawIndexedIndirect(vkCommandBuffer, vkIndirectBuffer.getBufferHandle(), 0, 10, 5 * 4);

		vkDeviceInstance.endRendering(vkCommandBuffer);
		vkDeviceInstance.stopRecording();

		vkDeviceInstance.submit();

		// image presenting
		vkDeviceInstance.presentImage();
	}

	private AttribFormat getVertexAttrib() {
		return new AttribFormat()
				.add(0, 0, VK_FORMAT_R32G32_SFLOAT)
				.add(0, 1, VK_FORMAT_R32G32_SFLOAT)
				.setBufferLayout(BufferLayout.SEQUENTIAL)
				.setPrimitiveCount(bulletsCount)
				.setVerticesPerPrimitive(4);
	}

	private void createGraphicsPipeline() {
		final AttribFormat attribFormat = getVertexAttrib();

		// TODO improve descriptor set layout stuff
		final VkDescriptorSetLayout descriptorSetLayout = new VkDescriptorSetLayout();
		final SetLayoutBindingsBuffer uniformBufferLayout = descriptorSetLayout.addLayout()
				.add(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_VERTEX_BIT)
				.add(1, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
		final SetLayoutBindingsBuffer ssboLayout = descriptorSetLayout.addLayout()
				.add(2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT);
		descriptorSetLayout.createDescriptorSetLayout(vkDeviceInstance);

		// vkGraphicsPipeline needs DescriptorSetLayout {binding, descriptorType, stageFlags}
		// Buffer creation need buffer size which is calculated with {format, count}
		// Then we need DescriptorPool which needs descriptorPoolSize which is calculated with {format, count}
		//      and multiplied by total buffers count (because we have uniform buffer for each frame in flight)
		// Then we need to create DescriptorSet's (one for each frame in flight)
		//      each descriptor set needs {vkDescriptorPool}
		// Then we need to call vkUpdateDescriptorSets and each call needs
		//      for buffers {buffer handle, buffer offset, buffer range, binding, descriptorType} or
		//      for textures {image view, image sampler, binding, descriptorType}

		vkGraphicsPipeline = new VkGraphicsPipeline()
				.addVertexShader("assets/shaders/indirect_ssbo/shader.vert")
				.addFragmentShader("assets/shaders/indirect_ssbo/shader.frag")
				.attribFormat(attribFormat)
				.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST) // optional
				.primitiveRestart(false) // optional
				.polygonModeFill() // optional
				.lineWidth(1f) // optional
				.cullModeFront() // optional
				.windingCounterClockwise() // optional
				.blending(false) // optional
				.blendFunc(VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA) // optional
				// depth is not supported yet
				.setDescriptorSetLayout(descriptorSetLayout)
				.create(vkDeviceInstance);

		vkDeviceInstance.createDescriptorStuff(vkGraphicsPipeline.getVkDescriptorSetLayout());
	}

	private void createVulkanContext() {
		vkDeviceInstance = new VkDeviceInstance()
				.applicationName("Vulkan Lib Test")
				.apiVersion(VK13.VK_API_VERSION_1_3)
				.validationLayers(true)
				.vsync(true)
				.swapChainImageCount(3) // also frames in flight count
				.create(windowVK);
	}

	private void createUniformBuffers() {
		final int fifCount = vkDeviceInstance.getSwapChainImageCount();
		final BufferSizeInfo bufferSizeInfo = new BufferSizeInfo()
				.add(VK_FORMAT_R32G32_SFLOAT, 1)
				.add(VK_FORMAT_R32G32_SFLOAT, 1);
		final int bufferSize = bufferSizeInfo.getBufferSize() * fifCount;
		vkDeviceInstance.createUniformBuffers(bufferSize);
	}

	private void createSSBO() {
		final BufferSizeInfo bufferSizeInfo = new BufferSizeInfo()
				.add(VK_FORMAT_R32G32_SFLOAT, 20000);
		final int bufferSize = bufferSizeInfo.getBufferSize();

		vkSSBO = new VkBuffer();
		vkSSBO.createBuffer(vkDeviceInstance, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
		vkSSBO.allocateMemory(vkDeviceInstance, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		vkSSBO.makeStagingBuffer(vkDeviceInstance, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
	}

	private void updateDescriptors() {
		for (FrameInFlight frameInFlight : vkDeviceInstance.getFrameInFlights()) {
			final List<DescriptorSetUpdate> descriptorSetUpdateList = new ArrayList<>();

			final long bufferHandle = frameInFlight.getUniformBuffer().getHandle();
			descriptorSetUpdateList.add(DescriptorSetUpdate.buffer(
					0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
					bufferHandle, 0, Float.BYTES * 4));

			descriptorSetUpdateList.add(DescriptorSetUpdate.image(
					1, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
					vkTexture.getVkTextureImageView(), vkTexture.getVkTextureSampler()));

			descriptorSetUpdateList.add(DescriptorSetUpdate.buffer(
					2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
					vkSSBO.getHandle(), 0, Float.BYTES * (2 * 20000)));

			frameInFlight.updateDescriptorSet(vkDeviceInstance, descriptorSetUpdateList);
		}
	}

	public static void main(String[] args) {
		new VKLIB_TEST();
	}
}
