package com.tiny.spring.scheduling;

import java.util.Date;

public interface Trigger {
	Date nextExecutionTime(TriggerContext triggerContext);
}
