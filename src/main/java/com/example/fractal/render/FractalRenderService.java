package com.example.fractal.render;

import com.example.fractal.model.FractalViewState;

import javax.swing.SwingWorker;
import java.awt.image.BufferedImage;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class FractalRenderService {

    public interface Listener {
        void onRenderStarted(RenderRequest request);

        void onRenderCompleted(RenderResult result);

        void onRenderFailed(RenderRequest request, Exception exception);
    }

    private long renderSequence;
    private SwingWorker<RenderResult, Void> renderWorker;

    public void renderAsync(RenderRequest request, Listener listener) {
        cancelActiveRender();

        final long requestSequence = ++renderSequence;
        listener.onRenderStarted(request);
        renderWorker = new SwingWorker<RenderResult, Void>() {
            @Override
            protected RenderResult doInBackground() {
                return renderMeasured(request);
            }

            @Override
            protected void done() {
                if (isCancelled() || requestSequence != renderSequence) {
                    return;
                }

                try {
                    listener.onRenderCompleted(get());
                } catch (CancellationException ignored) {
                    // Ignore stale render jobs.
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof Exception) {
                        listener.onRenderFailed(request, (Exception) cause);
                    } else {
                        listener.onRenderFailed(request, new RuntimeException(cause));
                    }
                }
            }
        };
        renderWorker.execute();
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
        if (renderWorker != null && !renderWorker.isDone()) {
            renderWorker.cancel(true);
        }
    }
}