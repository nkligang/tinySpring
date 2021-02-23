package com.fenglinga.tinyspring.scheduling;

import java.util.Date;

public interface Trigger {
    Date nextExecutionTime(TriggerContext triggerContext);
}
