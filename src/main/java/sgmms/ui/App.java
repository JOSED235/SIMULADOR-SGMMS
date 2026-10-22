package sgmms.ui;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import sgmms.modelo.Dificultad;
import sgmms.simulacion.Simulacion;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Ventana principal: cambia entre menú, turno e informe. */
public class App extends Application {

    /** Pantalla intercambiable de la aplicación. */
    interface Vista {
        Parent raiz();

        /** Libera animaciones y temporizadores al salir de la vista. */
        void detener();
    }

    private Stage ventana;
    private Scene escena;
    private Vista actual;

    @Override
    public void start(Stage ventana) {
        this.ventana = ventana;
        String carpetaCapturas = System.getProperty("sgmms.capturas");
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        // Las capturas usan un tamaño fijo, aunque no quepa en la pantalla (el snapshot no depende de ella)
        double ancho = carpetaCapturas != null ? 1600 : Math.min(1400, pantalla.getWidth() - 40);
        double alto = carpetaCapturas != null ? 900 : Math.min(860, pantalla.getHeight() - 40);

        escena = new Scene(new StackPane(), ancho, alto, Paleta.FONDO);
        escena.getStylesheets().add(getClass().getResource("estilos.css").toExternalForm());
        escena.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F12) {
                guardarCaptura(new File(System.getProperty("user.home"), "SGMMS-capturas"),
                        "sgmms-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".png");
                e.consume();
            }
        });

        ventana.setTitle("SGMMS · Simulador de despacho de emergencias");
        ventana.getIcons().add(icono());
        ventana.setMinWidth(1040);
        ventana.setMinHeight(660);
        ventana.setScene(escena);
        if (carpetaCapturas == null && pantalla.getHeight() < 820) ventana.setMaximized(true);

        if (carpetaCapturas != null) {
            ventana.show();
            new Capturas(this, new File(carpetaCapturas)).ejecutar();
            return;
        }
        mostrarMenu();
        ventana.show();
    }

    void mostrarMenu() {
        cambiar(new VistaMenu(this));
    }

    VistaJuego iniciarTurno(Dificultad dificultad, long semilla) {
        VistaJuego juego = new VistaJuego(this, new Simulacion(dificultad, semilla));
        cambiar(juego);
        return juego;
    }

    VistaInforme mostrarInforme(Simulacion sim) {
        VistaInforme informe = new VistaInforme(this, sim);
        cambiar(informe);
        return informe;
    }

    private void cambiar(Vista nueva) {
        if (actual != null) actual.detener();
        actual = nueva;
        escena.setRoot(nueva.raiz());
    }

    Stage getVentana() {
        return ventana;
    }

    /** Guarda la escena actual como PNG (F12 durante el juego). */
    File guardarCaptura(File carpeta, String nombre) {
        WritableImage imagen = escena.snapshot(null);
        int w = (int) imagen.getWidth();
        int h = (int) imagen.getHeight();
        BufferedImage salida = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        PixelReader lector = imagen.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) salida.setRGB(x, y, lector.getArgb(x, y));
        }
        try {
            carpeta.mkdirs();
            File archivo = new File(carpeta, nombre);
            ImageIO.write(salida, "png", archivo);
            return archivo;
        } catch (IOException ex) {
            System.err.println("No se pudo guardar la captura: " + ex.getMessage());
            return null;
        }
    }

    /** Ícono de la ventana dibujado en código: una cruz cian sobre fondo oscuro. */
    private static Image icono() {
        WritableImage img = new WritableImage(32, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                boolean borde = x < 2 || y < 2 || x > 29 || y > 29;
                boolean cruz = (x >= 13 && x <= 18 && y >= 6 && y <= 25) || (y >= 13 && y <= 18 && x >= 6 && x <= 25);
                int color = cruz ? 0xFF38D4FF : borde ? 0xFF1F2733 : 0xFF0B0E13;
                img.getPixelWriter().setArgb(x, y, color);
            }
        }
        return img;
    }
}
