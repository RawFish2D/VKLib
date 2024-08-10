package ua.rawfish2d.vklib.test;

import org.lwjgl.vulkan.VkCommandBuffer;
import ua.rawfish2d.vklib.VkBuffer;
import ua.rawfish2d.vklib.attrib.AttribFormat;
import ua.rawfish2d.vklib.init.VkDeviceInstance;
import ua.rawfish2d.vklib.utils.PredictableRandom;
import ua.rawfish2d.vklib.utils.TimeHelper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class BulletScene {
	public final List<Bullet> bullets;
	private final int verticesPerObject = 4;
	private final AttribFormat attribFormat;
	private final int bulletsCount;
	private final VkDeviceInstance vkDeviceInstance;
	private final TimeHelper updateTimer = new TimeHelper();
	public boolean pause = false;

	public BulletScene(VkDeviceInstance vkDeviceInstance, int bulletsCount, AttribFormat attribFormat) {
		this.bullets = new ArrayList<>(bulletsCount);
		this.bulletsCount = bulletsCount;
		this.vkDeviceInstance = vkDeviceInstance;
		this.attribFormat = attribFormat;
		createBullets(bulletsCount);
	}

	private void createBullets(int bulletsCount) {
		bullets.clear();
		PredictableRandom.setSeed(0);
		for (int a = 0; a < bulletsCount; ++a) {
			Bullet bullet = new Bullet();
			bullet.randomPos();
			bullet.randomMotion();
			bullets.add(bullet);
		}
	}

	public void resetBullets(VkBuffer vkVertexBuffer, VkBuffer vkIndexBuffer, VkBuffer vkSSBO) {
		PredictableRandom.setSeed(0);
		for (Bullet bullet : bullets) {
			bullet.randomPos();
			bullet.randomMotion();
		}
		updateAll(vkVertexBuffer, vkIndexBuffer, vkSSBO);
	}

	public void updateAll(VkBuffer vkVertexBuffer, VkBuffer vkIndexBuffer, VkBuffer vkSSBO) {
		// upload data
		updateVertex(vkVertexBuffer);
		updateTexCoords(vkVertexBuffer);
		updateSSBO(vkSSBO);

		updateIndexBuffer(vkIndexBuffer);
	}

	public void updateIndexBuffer(VkBuffer vkIndexBuffer) {
		final ByteBuffer indexBuffer = vkIndexBuffer.getStagingBuffer(vkDeviceInstance);
		indexBuffer.clear();
		for (int a = 0; a < bullets.size() * verticesPerObject; a += verticesPerObject) {
			indexBuffer.putInt(a);
			indexBuffer.putInt(a + 1);
			indexBuffer.putInt(a + 2);
			indexBuffer.putInt(a + 2);
			indexBuffer.putInt(a + 3);
			indexBuffer.putInt(a);
		}
		int remaining = indexBuffer.remaining();
		if (remaining != 0) {
			System.out.println("indexBuffer remaining: " + remaining);
		}
		indexBuffer.flip();
	}

	public void updateVertex(VkBuffer vkVertexBuffer) {
		final ByteBuffer vertexBuffer = vkVertexBuffer.getStagingBuffer(vkDeviceInstance);
		vertexBuffer.clear();
		final float halfSize = 24f / 2f;
		int pos = 0;
		pos -= 4;
		for (int a = 0; a < 10; ++a) {
			final float x0 = -halfSize;
			final float y0 = -halfSize;
			final float x1 = halfSize;
			final float y1 = halfSize;
			vertexBuffer.putFloat(pos += 4, x0).putFloat(pos += 4, y0);
			vertexBuffer.putFloat(pos += 4, x0).putFloat(pos += 4, y1);
			vertexBuffer.putFloat(pos += 4, x1).putFloat(pos += 4, y1);
			vertexBuffer.putFloat(pos += 4, x1).putFloat(pos += 4, y0);
		}
	}

	public void updateTexCoords(VkBuffer vkVertexBuffer) {
		final ByteBuffer vertexBuffer = vkVertexBuffer.getStagingBuffer(vkDeviceInstance);
		// update texture coords
		int pos = attribFormat.getSequentialAttribPosition(1);
		pos -= 4;
		for (int a = 0; a < 10; ++a) {
			final float u0 = uv(a * 32f, 320f);
			final float u1 = uv((a + 1) * 32f, 320f);
			final float v0 = 0f;
			final float v1 = 1f;
			vertexBuffer.putFloat(pos += 4, u0).putFloat(pos += 4, v0);
			vertexBuffer.putFloat(pos += 4, u0).putFloat(pos += 4, v1);
			vertexBuffer.putFloat(pos += 4, u1).putFloat(pos += 4, v1);
			vertexBuffer.putFloat(pos += 4, u1).putFloat(pos += 4, v0);
		}
	}

	private float uv(float x, float size) {
		return (1f / size) * x;
	}

	public void updateSSBO(VkBuffer vkSSBO) {
		vkSSBO.getStagingBuffer(vkDeviceInstance).clear();
		final FloatBuffer ssbo = vkSSBO.getStagingBuffer(vkDeviceInstance).asFloatBuffer();
		int pos = 0;
		for (final Bullet bullet : bullets) {
			bullet.update();
			final float x = bullet.pos.x;
			final float y = bullet.pos.y;
			ssbo.put(pos++, x).put(pos++, y);
		}
	}

	public void uploadBuffers(VkBuffer vkVertexBuffer, VkBuffer vkIndexBuffer, VkCommandBuffer vkCommandBuffer) {
		vkVertexBuffer.uploadFromStagingBuffer(vkCommandBuffer);
		vkVertexBuffer.vertexBufferBarrier(vkCommandBuffer);
		vkIndexBuffer.uploadFromStagingBuffer(vkCommandBuffer);
		vkIndexBuffer.indexBufferBarrier(vkCommandBuffer);
	}

	public boolean shouldUpdate() {
		if (pause) {
			return false;
		}
		if (updateTimer.hasReachedMilli(1000 / 75)) {
			updateTimer.reset();
			return true;
		}
		return false;
	}

	public void updateBulletPos(VkBuffer vkSSBO) {
		updateSSBO(vkSSBO);
	}

	public void uploadBulletPos(VkBuffer vkSSBO, VkCommandBuffer commandBuffer) {
		vkSSBO.uploadFromStagingBuffer(commandBuffer);
		vkSSBO.ssboBarrier(commandBuffer);
	}
}
