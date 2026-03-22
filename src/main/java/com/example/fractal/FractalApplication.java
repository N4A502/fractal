package com.example.fractal;

import com.example.fractal.render.EscapeTimeBackendSelector;
import javafx.application.Application;
import javafx.stage.Stage;

public final class FractalApplication {

    private FractalApplication() {
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(EscapeTimeBackendSelector::shutdown, "fractal-render-backend-shutdown"));
        Application.launch(JavaFxApp.class, args);
    }

    public static final class JavaFxApp extends Application {
        @Override
        public void start(Stage stage) {
            new FractalFxWindow(stage).show();
        }
    }
}