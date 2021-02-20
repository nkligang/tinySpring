package com.tiny.spring.scheduling;

import java.util.Date;

public interface TriggerContext {
	Date lastScheduledExecutionTime();
	Date lastActualExecutionTime();
	Date lastCompletionTime();
}
