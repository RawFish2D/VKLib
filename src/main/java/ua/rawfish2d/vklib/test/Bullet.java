package ua.rawfish2d.vklib.test;

import org.joml.Vector2f;
import ua.rawfish2d.vklib.utils.PredictableRandom;

public class Bullet {
	public final Vector2f pos = new Vector2f();
	public final Vector2f motion = new Vector2f();

	public void setPos(float x, float y) {
		pos.set(x, y);
	}

	public void update() {
		pos.x += motion.x;
		pos.y += motion.y;

		if (pos.x < -BulletSceneConfig.size.x || pos.x > BulletSceneConfig.windowWidth + BulletSceneConfig.size.x ||
				pos.y < -BulletSceneConfig.size.y || pos.y > BulletSceneConfig.windowHeight + BulletSceneConfig.size.y) {
			pos.x = BulletSceneConfig.windowWidth / 2f;
			pos.y = BulletSceneConfig.windowHeight / 2f;
			randomMotion();
		}
	}

	public void randomMotion() {
		float speed = PredictableRandom.random(1.0f, 3.0f);
		float rngAngle = PredictableRandom.random(0.0f, 360.0f);
		float rad = (float) Math.toRadians(rngAngle);
		float x = (float) (Math.cos(rad) * speed);
		float y = (float) (Math.sin(rad) * speed);
		motion.set(x, y);
	}

	public void randomPos() {
		float rngX = PredictableRandom.random(0f, BulletSceneConfig.windowWidth);
		float rngY = PredictableRandom.random(0f, BulletSceneConfig.windowHeight);
		pos.set(rngX, rngY);
	}

	public static class BulletSceneConfig {
		public static final Vector2f size = new Vector2f(24f, 24f);
		public static float windowWidth = 1024;
		public static float windowHeight = 768;
	}
}