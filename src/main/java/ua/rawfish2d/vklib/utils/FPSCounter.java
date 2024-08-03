package ua.rawfish2d.vklib.utils;

import lombok.Getter;

import java.util.Locale;

public class FPSCounter {
	private final TimeHelper fpsTimer = new TimeHelper();
	private float frametime = 0f;
	@Getter
	private float avgFrametime = 0f;
	private float frametimeMin = Float.MAX_VALUE;
	private float frametimeMax = Float.MIN_VALUE;
	private int fpsCounter = 0;
	@Getter
	private int fps = 0;
	private long time;

	public void pre() {
		time = System.nanoTime();
	}

	public void post() {
		time = System.nanoTime() - time; // delta time nano
		float deltaMS = ((float) time / 1000000f);
		if (frametimeMin > deltaMS) {
			frametimeMin = deltaMS;
		}
		if (frametimeMax < deltaMS) {
			frametimeMax = deltaMS;
		}
		frametime += deltaMS;
		fpsCounter++;

		if (fpsTimer.hasReachedMilli(1000)) {
			fpsTimer.reset();
			fps = fpsCounter;
			avgFrametime = frametime / (float) fps;

//			long totalMemory = Runtime.getRuntime().totalMemory();
//			long freeMemory = Runtime.getRuntime().freeMemory();
//			long usedMemory = totalMemory - freeMemory;
//			String totalMemoryStr = OtherUtils.humanReadableByteCount(totalMemory, true);
//			String freeMemoryStr = OtherUtils.humanReadableByteCount(freeMemory, true);
//			String usedMemoryStr = OtherUtils.humanReadableByteCount(usedMemory, true);
			System.out.printf(Locale.US, "fps %d | frametime avg %.4f | min %.4f | max %.4f\n", fps, avgFrametime, frametimeMin, frametimeMax);
			//System.out.printf(Locale.US, "used %s | total %s | free %s\n", usedMemoryStr, totalMemoryStr, freeMemoryStr);

			fpsCounter = 0;
			frametime = 0f;
			frametimeMin = Float.MAX_VALUE;
			frametimeMax = Float.MIN_VALUE;
		}
	}
}