package com.example.fractal.render;

public class RenderBackendSelection {

    private final RenderBackendType type;
    private final EscapeTimeBackend backend;
    private final String label;
    private final String reason;

    public RenderBackendSelection(RenderBackendType type, EscapeTimeBackend backend, String label, String reason) {
        this.type = type;
        this.backend = backend;
        this.label = label;
        this.reason = reason;
    }

    public RenderBackendType type() {
        return type;
    }

    public EscapeTimeBackend backend() {
        return backend;
    }

    public String label() {
        return label;
    }

    public String reason() {
        return reason;
    }

    public String describe() {
        return reason == null || reason.isEmpty() ? label : label + " (" + reason + ")";
    }
}