package com.example.fractal.render;

import com.example.fractal.model.FractalViewState;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FractalRenderService {

    public interface Listener {
        void onRenderStarted(RenderRequest request);

        void onRenderCompleted(RenderResult result);

        void onRenderFailed(RenderRequest request, Exception exception);
    }

    private long renderSequence;
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fractal-render-worker");
        thread.setDaemon(true);
        return thread;
    });
    private Future<?> renderFuture;

    public void renderAsync(RenderRequest request, Listener listener) {
        cancelActiveRender();

        final long requestSequence = ++renderSequence;
        listener.onRenderStarted(request);
        renderFuture = renderExecutor.submit(() -> {
            try {
                RenderResult result = renderMeasured(request);
                if (!Thread.currentThread().isInterrupted() && requestSequence == renderSequence) {
                    listener.onRenderCompleted(result);
                }
            } catch (Exception ex) {
                if (Thread.currentThread().isInterrupted() || requestSequence != renderSequence) {
                    return;
                }
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                listener.onRenderFailed(request, ex);
            }
        });
    }

    public RenderResult renderMeasured(RenderRequest request) {
        long startedAt = System.nanoTime();
        FractalViewState viewState = request.viewState();
        BufferedImage image = viewState.definition().renderer().renderImage(
                request.width(),
                request.height(),
                viewState.depth(),
                viewState.zoom(),
                viewState.offsetX(),
                viewState.offsetY()
        );
        long finishedAt = System.nanoTime();
        return new RenderResult(request, image, (finishedAt - startedAt) / 1_000_000L);
    }

    public void cancelActiveRender() {
        if (renderFuture != null && !renderFuture.isDone()) {
            renderFuture.cancel(true);
        }
    }

    public void shutdown() {
        cancelActiveRender();
        renderExecutor.shutdownNow();
    }
}
