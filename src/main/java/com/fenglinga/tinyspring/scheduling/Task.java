package com.fenglinga.tinyspring.scheduling;

public class Task {
	private final Runnable runnable;

	public Task(Runnable runnable) {
		this.runnable = runnable;
	}

	public Runnable getRunnable() {
		return this.runnable;
	}

	@Override
	public String toString() {
		return this.runnable.toString();
	}

}
