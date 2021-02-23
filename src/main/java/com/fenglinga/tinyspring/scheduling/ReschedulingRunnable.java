package com.fenglinga.tinyspring.scheduling;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ReschedulingRunnable extends DelegatingErrorHandlingRunnable implements ScheduledFuture<Object> {

    private final Trigger trigger;

    private final SimpleTriggerContext triggerContext = new SimpleTriggerContext();

    private final ScheduledExecutorService executor;

    private ScheduledFuture<?> currentFuture;

    private Date scheduledExecutionTime;

    private final Object triggerContextMonitor = new Object();


    public ReschedulingRunnable(
            Runnable delegate, Trigger trigger, ScheduledExecutorService executor) {

        super(delegate);
        this.trigger = trigger;
        this.executor = executor;
    }

    public ScheduledFuture<?> schedule() {
        synchronized (this.triggerContextMonitor) {
            this.scheduledExecutionTime = this.trigger.nextExecutionTime(this.triggerContext);
            if (this.scheduledExecutionTime == null) {
                return null;
            }
            long initialDelay = this.scheduledExecutionTime.getTime() - System.currentTimeMillis();
            this.currentFuture = this.executor.schedule(this, initialDelay, TimeUnit.MILLISECONDS);
            return this;
        }
    }

    private ScheduledFuture<?> obtainCurrentFuture() {
        return this.currentFuture;
    }

    @Override
    public void run() {
        Date actualExecutionTime = new Date();
        super.run();
        Date completionTime = new Date();
        synchronized (this.triggerContextMonitor) {
            this.triggerContext.update(this.scheduledExecutionTime, actualExecutionTime, completionTime);
            if (!obtainCurrentFuture().isCancelled()) {
                schedule();
            }
        }
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this.triggerContextMonitor) {
            return obtainCurrentFuture().cancel(mayInterruptIfRunning);
        }
    }

    @Override
    public boolean isCancelled() {
        synchronized (this.triggerContextMonitor) {
            return obtainCurrentFuture().isCancelled();
        }
    }

    @Override
    public boolean isDone() {
        synchronized (this.triggerContextMonitor) {
            return obtainCurrentFuture().isDone();
        }
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        ScheduledFuture<?> curr;
        synchronized (this.triggerContextMonitor) {
            curr = obtainCurrentFuture();
        }
        return curr.get();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        ScheduledFuture<?> curr;
        synchronized (this.triggerContextMonitor) {
            curr = obtainCurrentFuture();
        }
        return curr.get(timeout, unit);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        ScheduledFuture<?> curr;
        synchronized (this.triggerContextMonitor) {
            curr = obtainCurrentFuture();
        }
        return curr.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed other) {
        if (this == other) {
            return 0;
        }
        long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
        return (diff == 0 ? 0 : ((diff < 0)? -1 : 1));
    }

}
