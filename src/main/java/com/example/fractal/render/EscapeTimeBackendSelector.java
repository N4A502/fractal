package com.example.fractal.render;

public final class EscapeTimeBackendSelector {

    private static final EscapeTimeBackend CPU_BACKEND = new CpuEscapeTimeBackend();

    private EscapeTimeBackendSelector() {
    }

    public static RenderBackendSelection selectAuto() {
        String gpuHint = GraphicsAccelerationProbe.describeLikelyGpuAcceleration();
        if (gpuHint != null) {
            return new RenderBackendSelection(
                    RenderBackendType.CPU,
                    CPU_BACKEND,
                    "CPU fallback",
                    gpuHint + ", GPU kernel not linked"
            );
        }

        return new RenderBackendSelection(
                RenderBackendType.CPU,
                CPU_BACKEND,
                "CPU",
                "no GPU acceleration detected"
        );
    }
}