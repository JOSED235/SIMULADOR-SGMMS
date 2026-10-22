package sgmms.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import sgmms.modelo.Dificultad;
import sgmms.modelo.TipoUnidad;
import sgmms.simulacion.Ciudad;
import sgmms.simulacion.Simulacion;

import java.util.Random;

/** Menú principal. De fondo corre una simulación real con el despacho automático. */
final class VistaMenu implements App.Vista {

    private final App app;
    private final StackPane raiz = new StackPane();
    private final Canvas fondo = new Canvas();
    private final VBox opciones = new VBox(8);
    private final Label record = new Label();
    private final TextField campoSemilla = new TextField();
    private Node ayuda;

    private Simulacion demo;
    private Renderizador renderizador;
    private final Renderizador.Seleccion seleccionDemo = new Renderizador.Seleccion();
    private AnimationTimer bucle;
    private Dificultad dificultad = Dificultad.NORMAL;

    VistaMenu(App app) {
        this.app = app;
        construir();
        reiniciarDemo();
        bucle = new AnimationTimer() {
            private long ultimo;

            @Override
            public void handle(long ahora) {
                double dt = ultimo == 0 ? 0 : Math.min(0.1, (ahora - ultimo) / 1e9);
                ultimo = ahora;
                for (double t = 0; t < dt * 1.5; t += Simulacion.PASO) demo.avanzar(Simulacion.PASO);
                if (demo.terminada()) reiniciarDemo();
                dibujarFondo();
            }
        };
        bucle.start();
    }

    @Override
    public Parent raiz() {
        return raiz;
    }

    @Override
    public void detener() {
        bucle.stop();
    }

    private void reiniciarDemo() {
        demo = new Simulacion(Dificultad.NORMAL, new Random().nextLong());
        demo.setDespachoAutomatico(true);
        demo.setIgnorarFallos(true);
        for (int i = 0; i < 60 * 40; i++) demo.avanzar(Simulacion.PASO); // arranca con la ciudad en movimiento
        if (renderizador == null) renderizador = new Renderizador(demo.getCiudad());
    }

    private void dibujarFondo() {
        GraphicsContext gc = fondo.getGraphicsContext2D();
        double w = fondo.getWidth();
        double h = fondo.getHeight();
        // Cubre todo el ancho aunque se recorte arriba y abajo
        double escala = Math.max(w / Ciudad.ANCHO, h / Ciudad.ALTO);
        Renderizador.Camara cam = new Renderizador.Camara(escala,
                (w - Ciudad.ANCHO * escala) / 2, (h - Ciudad.ALTO * escala) / 2);
        renderizador.dibujar(gc, demo, cam, seleccionDemo, w, h);
        gc.setFill(Color.rgb(6, 9, 13, 0.62));
        gc.fillRect(0, 0, w, h);
    }

    private void construir() {
        Pane contenedor = new Pane(fondo);
        fondo.widthProperty().bind(contenedor.widthProperty());
        fondo.heightProperty().bind(contenedor.heightProperty());

        Label antetitulo = new Label("CENTRO DE DESPACHO DE EMERGENCIAS");
        antetitulo.getStyleClass().add("titulo-seccion");
        Label titulo = new Label("SGMMS");
        titulo.getStyleClass().add("titulo-grande");
        Label subtitulo = new Label("Simulador de despacho sobre un grafo vial dinámico");
        subtitulo.getStyleClass().add("subtitulo");
        Label descripcion = new Label("Asigna patrullas, ambulancias y bomberos antes de que se venza el plazo de cada incidente. "
                + "El tráfico cambia el costo de cada calle en tiempo real, los accidentes cierran vías y las unidades "
                + "recalculan su ruta con A*.");
        descripcion.setWrapText(true);
        descripcion.getStyleClass().add("suave");

        Label tituloDificultad = new Label("DIFICULTAD");
        tituloDificultad.getStyleClass().add("titulo-seccion");
        dibujarOpciones();

        campoSemilla.setPromptText("Semilla (vacío = aleatoria)");
        campoSemilla.getStyleClass().add("campo");
        campoSemilla.setPrefWidth(210);
        campoSemilla.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) iniciar();
        });

        Button iniciar = new Button("Iniciar turno");
        iniciar.getStyleClass().addAll("boton", "boton-primario");
        iniciar.setStyle("-fx-font-size: 15px; -fx-padding: 10 26;");
        iniciar.setOnAction(e -> iniciar());
        iniciar.setDefaultButton(true);
        Button comoJugar = new Button("Cómo se juega");
        comoJugar.getStyleClass().add("boton");
        comoJugar.setStyle("-fx-padding: 10 18;");
        comoJugar.setOnAction(e -> mostrarAyuda());
        Button salir = new Button("Salir");
        salir.getStyleClass().add("boton");
        salir.setStyle("-fx-padding: 10 18;");
        salir.setOnAction(e -> app.getVentana().close());
        HBox botones = new HBox(10, iniciar, comoJugar, salir);
        botones.setAlignment(Pos.CENTER_LEFT);

        record.getStyleClass().addAll("suave", "pequeno");
        actualizarRecord();
        HBox semillaYRecord = new HBox(14, campoSemilla, record);
        semillaYRecord.setAlignment(Pos.CENTER_LEFT);

        Label pie = new Label("Java 17 · JavaFX · lista enlazada, tabla hash, montículo, árbol AVL y grafo propios");
        pie.getStyleClass().addAll("suave", "pequeno");

        VBox tarjeta = new VBox(12, antetitulo, titulo, subtitulo, descripcion, tituloDificultad, opciones,
                semillaYRecord, botones, pie);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setMaxWidth(560);
        tarjeta.setMaxHeight(Double.NEGATIVE_INFINITY);
        VBox.setMargin(titulo, new Insets(-8, 0, 0, 0));

        StackPane.setAlignment(tarjeta, Pos.CENTER_LEFT);
        StackPane.setMargin(tarjeta, new Insets(30, 30, 30, 60));
        raiz.getChildren().addAll(contenedor, tarjeta);
        raiz.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && ayuda != null) cerrarAyuda();
        });
    }

    private void dibujarOpciones() {
        opciones.getChildren().clear();
        for (Dificultad d : Dificultad.values()) {
            Label nombre = new Label(d.getNombre());
            nombre.getStyleClass().add("negrita");
            Label detalle = new Label(String.format("%d min · %d patrullas, %d ambulancias, %d camiones · ~%d autos · %d fallos permitidos",
                    (int) d.getDuracionTurno() / 60, d.unidades(TipoUnidad.PATRULLA), d.unidades(TipoUnidad.AMBULANCIA),
                    d.unidades(TipoUnidad.BOMBEROS), d.getAutosCiviles(), d.getFallosPermitidos()));
            detalle.getStyleClass().addAll("suave", "pequeno");
            VBox opcion = new VBox(nombre, detalle);
            opcion.getStyleClass().add("opcion");
            if (d == dificultad) opcion.getStyleClass().add("opcion-activa");
            HBox.setHgrow(opcion, Priority.ALWAYS);
            opcion.setOnMouseClicked(e -> {
                dificultad = d;
                dibujarOpciones();
                actualizarRecord();
            });
            opciones.getChildren().add(opcion);
        }
    }

    private void actualizarRecord() {
        int mejor = Records.mejor(dificultad);
        record.setText(mejor > 0 ? "Récord en " + dificultad.getNombre() + ": " + String.format("%,d", mejor).replace(',', '.') + " pts"
                : "Aún no hay récord en " + dificultad.getNombre());
    }

    private void iniciar() {
        long semilla;
        String texto = campoSemilla.getText().trim();
        try {
            semilla = texto.isEmpty() ? Math.abs(new Random().nextInt(1_000_000)) : Long.parseLong(texto);
        } catch (NumberFormatException ex) {
            semilla = texto.hashCode(); // también se aceptan palabras como semilla
        }
        app.iniciarTurno(dificultad, semilla);
    }

    private void mostrarAyuda() {
        if (ayuda != null) return;
        ayuda = Ayuda.crear(this::cerrarAyuda);
        raiz.getChildren().add(ayuda);
        raiz.requestFocus();
    }

    private void cerrarAyuda() {
        raiz.getChildren().remove(ayuda);
        ayuda = null;
    }
}
