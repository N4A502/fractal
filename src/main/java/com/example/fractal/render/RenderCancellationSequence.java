package com.example.fractal.render;

public final class RenderCancellationSequence {

    private static final ThreadLocal<Long> CURRENT = ThreadLocal.withInitial(() -> -1L);

    private RenderCancellationSequence() {
    }

    public static void bind(long sequence) {
        CURRENT.set(sequence);
    }

    public static long current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
