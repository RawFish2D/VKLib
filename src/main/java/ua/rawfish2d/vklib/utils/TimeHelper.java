package ua.rawfish2d.vklib.utils;

public final class TimeHelper {
	private long lastNano;

	public TimeHelper() {
		this.reset();
	}

	public long getCurrentMilli() {
		return System.nanoTime() / 1000000L;
	}

	public long getCurrentMicro() {
		return System.nanoTime() / 1000L;
	}

	public long getCurrentNano() {
		return System.nanoTime();
	}

	public long getLastMilli() {
		return this.lastNano / 1000000L;
	}

	public long getLastMicro() {
		return this.lastNano / 1000L;
	}

	public long getLastNano() {
		return this.lastNano;
	}

	public boolean hasReachedMilli(final long milliseconds) {
		return this.getCurrentMilli() - getLastMilli() >= milliseconds;
	}

	public boolean hasReachedMicro(final long microseconds) {
		return this.getCurrentMicro() - getLastMicro() >= microseconds;
	}

	public boolean hasReachedNano(final long nanoseconds) {
		return 0L >= nanoseconds;
	}

	public long getTimeDiffMilli() {
		return (getCurrentNano() - lastNano) / 1000000L;
	}

	public long getTimeDiffMicro() {
		return (getCurrentNano() - lastNano) / 1000L;
	}

	public long getTimeDiffNano() {
		return getCurrentNano() - lastNano;
	}

	public void reset() {
		this.lastNano = this.getCurrentNano();
	}

	public void setLastMilli(final long ms) {
		this.lastNano = ms * 1000000L;
	}

	public void setLastMicro(final long micro) {
		this.lastNano = micro * 1000L;
	}

	public void setLastNano(final long nano) {
		this.lastNano = nano;
	}
}
