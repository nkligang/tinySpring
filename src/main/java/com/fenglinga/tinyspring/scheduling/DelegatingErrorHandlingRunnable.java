package com.fenglinga.tinyspring.scheduling;

public class DelegatingErrorHandlingRunnable implements Runnable {

    private final Runnable delegate;


    /**
     * Create a new DelegatingErrorHandlingRunnable.
     * @param delegate the Runnable implementation to delegate to
     * @param errorHandler the ErrorHandler for handling any exceptions
     */
    public DelegatingErrorHandlingRunnable(Runnable delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        try {
            this.delegate.run();
        }
        catch (Throwable ex) {
            ex.printStackTrace();;
        }
    }

    @Override
    public String toString() {
        return "DelegatingErrorHandlingRunnable for " + this.delegate;
    }

}
