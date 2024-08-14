package ua.rawfish2d.vklib;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

@Getter
public class WindowVK {
	private long handle;
	private boolean fullScreen = false;
	private int displayWidth;
	private int displayHeight;
	@Setter
	private boolean transparentFramebuffer = false;
	private boolean decorated = true;
	private boolean resizable = true;
	private boolean visible = true;
	private boolean alwaysOnTop = false;
	private boolean maximized = false;

	public WindowVK() {
	}

	public void init() {
		if (!glfwInit()) {
			throw new RuntimeException("Failed to init GLFW!");
		}
		if (!glfwVulkanSupported()) {
			throw new AssertionError("GLFW failed to find the Vulkan loader");
		}
	}

	public void create(int width, int height, String title) {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
		glfwWindowHint(GLFW_DECORATED, decorated ? GLFW_TRUE : GLFW_FALSE);
		glfwWindowHint(GLFW_VISIBLE, visible ? GLFW_TRUE : GLFW_FALSE);
		glfwWindowHint(GLFW_FLOATING, alwaysOnTop ? GLFW_TRUE : GLFW_FALSE);
		glfwWindowHint(GLFW_MAXIMIZED, maximized ? GLFW_TRUE : GLFW_FALSE);
		glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, transparentFramebuffer ? GLFW_TRUE : GLFW_FALSE);

		displayWidth = width;
		displayHeight = height;
		handle = glfwCreateWindow(width, height, title, 0, 0);
	}

	public void setFullscreen(boolean fullscreen, boolean windowed, int width, int height) {
		if (fullscreen != this.fullScreen) {
			if (this.handle != 0L) {
				this.fullScreen = fullscreen;
				final long monitor = glfwGetPrimaryMonitor();
				final GLFWVidMode mode = glfwGetVideoMode(monitor);
				if (mode == null) {
					return;
				}
				if (fullscreen) {
					if (windowed) {
						System.out.printf("Switching to windowed full screen: %dx%d %d\n", width, height, mode.refreshRate());
						glfwSetWindowAttrib(handle, GLFW_DECORATED, GLFW_FALSE);
						glfwSetWindowMonitor(handle, 0L, 0, 0, width, height, mode.refreshRate());
						resizeWindow(width, height);
					} else {
						System.out.printf("Switching to full screen: %dx%d %d\n", width, height, mode.refreshRate());
						glfwSetWindowMonitor(handle, monitor, 0, 0, width, height, mode.refreshRate());
					}
				} else {
					System.out.printf("Switching to windowed: %dx%d %d\n", width, height, mode.refreshRate());
					glfwSetWindowAttrib(handle, GLFW_DECORATED, GLFW_TRUE);
					glfwSetWindowMonitor(handle, 0L, 50, 50, width, height, mode.refreshRate());
					resizeWindow(width, height);
				}
			}
		}
	}

	/**
	 * Retains original window size
	 **/
	public void setFullscreen(boolean fullscreen, boolean windowed) {
		if (fullscreen != this.fullScreen) {
			if (this.handle != 0L) {
				final long monitor = glfwGetPrimaryMonitor();
				final GLFWVidMode mode = glfwGetVideoMode(monitor);
				if (mode == null) {
					return;
				}
				setFullscreen(fullscreen, windowed, mode.width(), mode.height());
			}
		}
	}

	public void showWindow() {
		glfwShowWindow(handle);
	}

	public void hideWindow() {
		glfwHideWindow(handle);
	}

	public void resizeWindow(int w, int h) {
		if (handle != 0L) {
			glfwSetWindowSize(handle, w, h);
			displayWidth = w;
			displayHeight = h;
		}
	}

	public void setFramebufferSizeCallback(GLFWFramebufferSizeCallback framebufferSizeCallback) {
		glfwSetFramebufferSizeCallback(handle, framebufferSizeCallback);
	}

	public void setDecorated(boolean decorated) {
		this.decorated = decorated;
		if (handle != 0) {
			glfwSetWindowAttrib(handle, GLFW_DECORATED, decorated ? GLFW_TRUE : GLFW_FALSE);
		}
	}

	public void setResizable(boolean resizable) {
		this.resizable = resizable;
		if (handle != 0) {
			glfwSetWindowAttrib(handle, GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
		}
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		if (handle != 0) {
			if (visible) {
				showWindow();
			} else {
				hideWindow();
			}
		}
	}

	public void setKeyCallback(GLFWKeyCallback keyCallback) {
		glfwSetKeyCallback(handle, keyCallback);
	}

	public void getFramebufferSize(IntBuffer widthOut, IntBuffer heightOut) {
		glfwGetFramebufferSize(handle, widthOut, heightOut);
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(handle);
	}

	public void pollEvents() {
		glfwPollEvents();
	}

	public void destroyWindow() {
		glfwDestroyWindow(handle);
	}

	public void terminate() {
		glfwTerminate();
	}
}
