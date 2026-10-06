package org.mygeneexplorer;

import javafx.application.Application;
import javafx.stage.Stage;
import org.mygeneexplorer.controller.MainController;
import org.mygeneexplorer.i18n.I18n;
import org.mygeneexplorer.model.AppState;
import org.mygeneexplorer.prefs.UserPreferences;
import org.mygeneexplorer.service.MyGeneService;
import org.mygeneexplorer.ui.MainView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point of the desktop application: assembles the model, the controller and the view.
 */
public class MyGeneExplorerApp extends Application {

    private MainController controller;
    private MainView view;

    /**
     * Launches the application.
     *
     * @param args command-line arguments, unused
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Creates the main window from the saved settings and shows it.
     *
     * @param stage primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        UserPreferences preferences = UserPreferences.load();
        I18n i18n = new I18n(preferences.locale());
        AppState state = new AppState();
        controller = new MainController(state, i18n, preferences, new MyGeneService(),
                requestExecutor());
        view = new MainView(stage, getHostServices(), state, i18n, controller);
        view.show(preferences.windowWidth(), preferences.windowHeight(),
                preferences.windowMaximized());
    }

    /** Saves the window size and settings, and cancels pending requests. */
    @Override
    public void stop() {
        if (view != null) {
            view.saveWindowState();
        }
        if (controller != null) {
            controller.shutdown();
        }
    }

    /**
     * Creates the executor running the HTTP requests. Its threads are daemons so they never
     * keep the application alive.
     *
     * @return the executor
     */
    private static ExecutorService requestExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "mygene-request");
            thread.setDaemon(true);
            return thread;
        });
    }
}
