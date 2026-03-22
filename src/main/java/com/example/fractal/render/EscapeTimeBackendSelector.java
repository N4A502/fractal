package com.example.fractal.render;

import java.lang.reflect.Method;

public final class EscapeTimeBackendSelector {

    private static final EscapeTimeBackend CPU_BACKEND = new CpuEscapeTimeBackend();
    private static volatile RenderBackendSelection cachedSelection;
    private static volatile EscapeTimeBackend gpuBackend;

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
            Object backendInstance = createGpuBackendInstance();
            String renderer = initializeAndDescribe(backendInstance);
            gpuBackend = (EscapeTimeBackend) backendInstance;
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
            try {
                gpuBackend.getClass().getMethod("shutdown").invoke(gpuBackend);
            } catch (ReflectiveOperationException ignored) {
                // Best-effort cleanup for optional GPU backend.
            }
            gpuBackend = null;
        }
        cachedSelection = null;
    }

    private static Object createGpuBackendInstance() throws ReflectiveOperationException {
        Class<?> backendClass = Class.forName("com.example.fractal.render.LwjglGpuEscapeTimeBackend");
        return backendClass.getDeclaredConstructor().newInstance();
    }

    private static String initializeAndDescribe(Object backendInstance) throws ReflectiveOperationException {
        Method method = backendInstance.getClass().getMethod("initializeAndDescribe");
        Object result = method.invoke(backendInstance);
        return result instanceof String ? (String) result : "GPU";
    }

    private static String simplifyReason(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }
}