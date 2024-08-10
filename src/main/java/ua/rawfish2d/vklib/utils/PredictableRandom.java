package ua.rawfish2d.vklib.utils;

import java.util.Random;

public class PredictableRandom {
	private static final Random random = new Random();

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

	public static int random(int min, int max) {
		int max2 = (max - min) + 1;
		if (max2 < 0) {
			max2 = min + 1;
		}
		return random.nextInt(max2) + min;
	}

	public static float random(float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	public static double random(double min, double max) {
		return min + random.nextDouble() * (max - min);
	}
}
