package com.example.fractal.render;

import java.util.concurrent.atomic.AtomicLong;

public final class RenderCancellation {

    private static final AtomicLong ACTIVE_SEQUENCE = new AtomicLong();
    private static final ThreadLocal<Long> THREAD_SEQUENCE = new ThreadLocal<Long>();

    private RenderCancellation() {
    }

    public static void activate(long sequence) {
        ACTIVE_SEQUENCE.set(sequence);
    }

    public static void bindToCurrentThread(long sequence) {
        THREAD_SEQUENCE.set(sequence);
    }

    public static void clearCurrentThread() {
        THREAD_SEQUENCE.remove();
    }

    public static boolean isCancelled(long sequence) {
        return Thread.currentThread().isInterrupted() || sequence != ACTIVE_SEQUENCE.get();
    }

    public static boolean isCancelledCurrentThread() {
        Long sequence = THREAD_SEQUENCE.get();
        return sequence != null && isCancelled(sequence);
    }
}
