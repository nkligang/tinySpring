package com.fenglinga.tinyspring.scheduling;

public class TriggerTask extends Task {
	private final Trigger trigger;

	public TriggerTask(Runnable runnable, Trigger trigger) {
		super(runnable);
		this.trigger = trigger;
	}

	public Trigger getTrigger() {
		return this.trigger;
	}
}
