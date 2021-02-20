package com.fenglinga.tinyspring.scheduling;

import java.util.Date;

public interface TriggerContext {
	Date lastScheduledExecutionTime();
	Date lastActualExecutionTime();
	Date lastCompletionTime();
}
