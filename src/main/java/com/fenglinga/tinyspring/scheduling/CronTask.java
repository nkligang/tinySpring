package com.fenglinga.tinyspring.scheduling;

public class CronTask extends TriggerTask {
	private final String expression;

	public CronTask(Runnable runnable, String expression) {
		this(runnable, new CronTrigger(expression));
	}

	public CronTask(Runnable runnable, CronTrigger cronTrigger) {
		super(runnable, cronTrigger);
		this.expression = cronTrigger.getExpression();
	}

	public String getExpression() {
		return this.expression;
	}
}
