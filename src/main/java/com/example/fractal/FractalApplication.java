package com.example.fractal;

import com.example.fractal.render.EscapeTimeBackendSelector;
import javafx.application.Application;
import javafx.stage.Stage;

public final class FractalApplication extends Application {

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(EscapeTimeBackendSelector::shutdown, "fractal-render-backend-shutdown"));
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        new FractalFxWindow(stage).show();
    }
}
