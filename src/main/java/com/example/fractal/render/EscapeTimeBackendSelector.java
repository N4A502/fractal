package com.example.fractal.render;

public final class EscapeTimeBackendSelector {

    private static final EscapeTimeBackend CPU_BACKEND = new CpuEscapeTimeBackend();
    private static volatile RenderBackendSelection cachedSelection;
    private static volatile LwjglGpuEscapeTimeBackend gpuBackend;

    private EscapeTimeBackendSelector() {
    }

    public static synchronized RenderBackendSelection selectAuto() {
        if (cachedSelection != null) {
            return cachedSelection;
        }

        if (!GraphicsAccelerationProbe.isGpuAccelerationLikelyAvailable()) {
            cachedSelection = new RenderBackendSelection(
                    RenderBackendType.CPU,
                    CPU_BACKEND,
                    "CPU",
                    "no GPU acceleration detected"
            );
            return cachedSelection;
        }

        try {
            gpuBackend = new LwjglGpuEscapeTimeBackend();
            String renderer = gpuBackend.initializeAndDescribe();
            cachedSelection = new RenderBackendSelection(
                    RenderBackendType.GPU,
                    gpuBackend,
                    "GPU",
                    renderer
            );
            return cachedSelection;
        } catch (Throwable throwable) {
            gpuBackend = null;
            cachedSelection = new RenderBackendSelection(
                    RenderBackendType.CPU,
                    CPU_BACKEND,
                    "CPU fallback",
                    simplifyReason(throwable)
            );
            return cachedSelection;
        }
    }

    public static synchronized void shutdown() {
        if (gpuBackend != null) {
            gpuBackend.shutdown();
            gpuBackend = null;
        }
        cachedSelection = null;
    }

    private static String simplifyReason(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }
}