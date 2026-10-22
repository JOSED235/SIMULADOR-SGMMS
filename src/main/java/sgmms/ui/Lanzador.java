package sgmms.ui;

import javafx.application.Application;

/**
 * Punto de entrada. Existe aparte de {@link App} para poder arrancar desde el
 * classpath sin module-info (JavaFX exige que la clase main no herede de Application).
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
