package com.fenglinga.tinyspring.scheduling;

import java.util.Date;

public class SimpleTriggerContext implements TriggerContext {

	private volatile Date lastScheduledExecutionTime;
	
	private volatile Date lastActualExecutionTime;

	private volatile Date lastCompletionTime;


	public SimpleTriggerContext() {
	}

	
	public SimpleTriggerContext(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}

	public void update(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}

	@Override
	public Date lastScheduledExecutionTime() {
		return this.lastScheduledExecutionTime;
	}

	@Override
	public Date lastActualExecutionTime() {
		return this.lastActualExecutionTime;
	}

	@Override
	public Date lastCompletionTime() {
		return this.lastCompletionTime;
	}

}
