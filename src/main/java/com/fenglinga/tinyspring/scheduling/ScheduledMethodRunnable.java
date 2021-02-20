package com.fenglinga.tinyspring.scheduling;

import java.lang.reflect.Method;

public class ScheduledMethodRunnable implements Runnable {
	private final Object target;
	private final Method method;

	public ScheduledMethodRunnable(Object target, Method method) {
		this.target = target;
		this.method = method;
	}

	public ScheduledMethodRunnable(Object target, String methodName) throws NoSuchMethodException {
		this.target = target;
		this.method = target.getClass().getMethod(methodName);
	}

	public Object getTarget() {
		return this.target;
	}

	public Method getMethod() {
		return this.method;
	}


	@Override
	public void run() {
		try {
			this.method.invoke(this.target);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return this.method.getDeclaringClass().getName() + "." + this.method.getName();
	}
}
