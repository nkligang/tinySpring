package com.tiny.spring.scheduling;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.tiny.spring.common.Utils;

import com.tiny.spring.framework.annotation.Component;
import com.tiny.spring.scheduling.annotation.Scheduled;

public class ScheduledTaskRegistrar {
	private List<CronTask> cronTasks = new ArrayList<CronTask>();
	private ScheduledThreadPoolExecutor taskScheduler = null;

	public ScheduledTaskRegistrar(ScheduledThreadPoolExecutor executor) {
		this.taskScheduler = executor;
		
		try {
			Set<Class<?>> klassSet = Utils.getClasses("task");
			for (Class<?> klass : klassSet) {
				boolean isComponent = false;
				Annotation[] annos = klass.getAnnotations();
	            for (Annotation annotation : annos) {
	            	Class<?> annotationType = annotation.annotationType();
	            	if (annotationType.getSimpleName().equals(Component.class.getSimpleName())) {
	            		isComponent = true;
	            		break;
	            	}
	            }
	            if (!isComponent) {
	            	continue;
	            }
				Method[] methods = klass.getDeclaredMethods();
				Object bean = klass.newInstance();
				for (Method method : methods) {
					Scheduled schd = method.getAnnotation(Scheduled.class);
	                if (schd == null) {
	                	continue;
	                }
	                processScheduled(schd, method, bean);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void processScheduled(Scheduled scheduled, Method method, Object bean) {
		Runnable runnable = new ScheduledMethodRunnable(bean, method);
		CronTask cronTask = new CronTask(runnable, new CronTrigger(scheduled.cron()));
		new ReschedulingRunnable(cronTask.getRunnable(), cronTask.getTrigger(), taskScheduler).schedule();
		cronTasks.add(cronTask);
	}
}
