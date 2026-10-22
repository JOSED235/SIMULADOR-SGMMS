package sgmms.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import sgmms.modelo.TipoIncidente;
import sgmms.simulacion.Metricas;
import sgmms.simulacion.Simulacion;

import java.util.Locale;
import java.util.Random;

/** Informe de fin de turno con los indicadores operativos. */
final class VistaInforme implements App.Vista {

    private final App app;
    private final Simulacion sim;
    private final ScrollPane raiz;

    VistaInforme(App app, Simulacion sim) {
        this.app = app;
        this.sim = sim;
        boolean nuevoRecord = Records.registrar(sim.getDificultad(), sim.getPuntaje());
        raiz = new ScrollPane(construir(nuevoRecord));
        raiz.setFitToWidth(true);
        raiz.setFitToHeight(true);
        raiz.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // El foco inicial cae en un botón del final y el scroll lo sigue: se vuelve arriba
        Platform.runLater(() -> raiz.setVvalue(0));
    }

    @Override
    public Parent raiz() {
        return raiz;
    }

    @Override
    public void detener() {
    }

    private Node construir(boolean nuevoRecord) {
        Metricas m = sim.getMetricas();
        boolean completo = sim.getResultado() == Simulacion.Resultado.TURNO_COMPLETO;

        Label estado = new Label(completo ? "TURNO COMPLETADO" : "TURNO FALLIDO");
        estado.getStyleClass().add("titulo-seccion");
        estado.setStyle("-fx-text-fill: " + (completo ? "#3ddc84" : "#ff4d5e") + ";");
        Label titulo = new Label("Informe del turno");
        titulo.getStyleClass().add("titulo-grande");
        titulo.setStyle("-fx-font-size: 32px;");
        Label contexto = new Label("Dificultad " + sim.getDificultad().getNombre() + " · semilla " + sim.getSemilla()
                + " · duración " + VistaJuego.formatoReloj(sim.getTiempo())
                + (completo ? "" : " · se superó el límite de " + sim.getDificultad().getFallosPermitidos() + " fallos"));
        contexto.getStyleClass().add("suave");

        FlowPane kpis = new FlowPane(12, 12,
                kpi("Puntaje", String.format("%,d", sim.getPuntaje()).replace(',', '.'), nuevoRecord ? "¡Nuevo récord!" : "Récord: "
                        + String.format("%,d", Records.mejor(sim.getDificultad())).replace(',', '.'), nuevoRecord ? "#38d4ff" : null),
                kpi("Resueltos", m.resueltos() + " / " + m.generados(), "incidentes del turno", null),
                kpi("Cumplimiento", porcentaje(m.cumplimiento()), "atendidos antes del plazo", m.cumplimiento() >= 0.9 ? "#3ddc84" : "#ffb020"),
                kpi("Respuesta promedio", segundos(m.respuestaPromedio()), "del reporte a la llegada", null),
                kpi("Percentil 90", segundos(m.percentilRespuesta(90)), "9 de cada 10 llegaron antes", null),
                kpi("Expirados", String.valueOf(m.expirados()), m.getReasignaciones() + " reasignaciones · " + m.getReplanificaciones() + " desvíos",
                        m.expirados() == 0 ? "#3ddc84" : "#ff4d5e"));

        HBox graficas = new HBox(14, graficaPorTipo(m), graficaEvolucion(m));
        graficas.setAlignment(Pos.TOP_LEFT);

        Button otra = new Button("Jugar otro turno");
        otra.getStyleClass().addAll("boton", "boton-primario");
        otra.setStyle("-fx-padding: 10 22;");
        otra.setOnAction(e -> app.iniciarTurno(sim.getDificultad(), Math.abs(new Random().nextInt(1_000_000))));
        Button repetir = new Button("Repetir esta semilla");
        repetir.getStyleClass().add("boton");
        repetir.setStyle("-fx-padding: 10 18;");
        repetir.setOnAction(e -> app.iniciarTurno(sim.getDificultad(), sim.getSemilla()));
        Button menu = new Button("Volver al menú");
        menu.getStyleClass().add("boton");
        menu.setStyle("-fx-padding: 10 18;");
        menu.setOnAction(e -> app.mostrarMenu());
        HBox botones = new HBox(10, otra, repetir, menu);

        VBox contenido = new VBox(14, estado, titulo, contexto, kpis, graficas, tablaPorTipo(m), botones);
        contenido.setPadding(new Insets(34, 44, 40, 44));
        contenido.setMaxWidth(1180);
        VBox.setMargin(titulo, new Insets(-10, 0, 0, 0));
        VBox envoltura = new VBox(contenido);
        envoltura.setAlignment(Pos.TOP_CENTER);
        envoltura.setStyle("-fx-background-color: #0b0e13;");
        return envoltura;
    }

    private Node kpi(String nombre, String valor, String nota, String color) {
        Label n = new Label(nombre.toUpperCase());
        n.getStyleClass().add("titulo-seccion");
        Label v = new Label(valor);
        v.getStyleClass().add("kpi-valor");
        if (color != null) v.setStyle("-fx-text-fill: " + color + ";");
        Label t = new Label(nota);
        t.getStyleClass().addAll("suave", "pequeno");
        t.setWrapText(true);
        VBox caja = new VBox(2, n, v, t);
        caja.getStyleClass().add("kpi");
        caja.setPrefWidth(170);
        return caja;
    }

    private Node graficaPorTipo(Metricas m) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setLabel("segundos");
        BarChart<String, Number> grafica = new BarChart<>(x, y);
        grafica.setTitle("Respuesta promedio por tipo");
        grafica.setLegendVisible(false);
        grafica.setAnimated(false);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (TipoIncidente t : TipoIncidente.values()) {
            serie.getData().add(new XYChart.Data<>(nombreCorto(t), m.respuestaPromedio(t)));
        }
        grafica.getData().add(serie);
        grafica.setPrefSize(520, 300);
        HBox.setHgrow(grafica, Priority.ALWAYS);
        return grafica;
    }

    private Node graficaEvolucion(Metricas m) {
        NumberAxis x = new NumberAxis();
        x.setLabel("minuto del turno");
        NumberAxis y = new NumberAxis();
        LineChart<Number, Number> grafica = new LineChart<>(x, y);
        grafica.setTitle("Carga de la ciudad durante el turno");
        grafica.setAnimated(false);
        grafica.setCreateSymbols(false);
        XYChart.Series<Number, Number> activos = new XYChart.Series<>();
        activos.setName("Incidentes activos");
        XYChart.Series<Number, Number> congestion = new XYChart.Series<>();
        congestion.setName("Congestión media (x10)");
        for (Metricas.Muestra mu : m.getMuestras()) {
            double minuto = mu.tiempo() / 60.0;
            activos.getData().add(new XYChart.Data<>(minuto, mu.incidentesActivos()));
            congestion.getData().add(new XYChart.Data<>(minuto, mu.congestion() * 10));
        }
        grafica.getData().add(activos);
        grafica.getData().add(congestion);
        grafica.setPrefSize(520, 300);
        HBox.setHgrow(grafica, Priority.ALWAYS);
        return grafica;
    }

    private Node tablaPorTipo(Metricas m) {
        GridPane tabla = new GridPane();
        tabla.setHgap(28);
        tabla.setVgap(8);
        tabla.getStyleClass().add("kpi");
        String[] encabezados = {"Tipo", "Generados", "Resueltos", "Expirados", "Respuesta prom."};
        for (int c = 0; c < encabezados.length; c++) {
            Label l = new Label(encabezados[c].toUpperCase());
            l.getStyleClass().add("titulo-seccion");
            tabla.add(l, c, 0);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(c == 0 ? 190 : 110);
            tabla.getColumnConstraints().add(cc);
        }
        int fila = 1;
        for (TipoIncidente t : TipoIncidente.values()) {
            Label nombre = new Label(t.getNombre());
            nombre.setStyle("-fx-text-fill: " + Paleta.hex(Paleta.incidente(t)) + "; -fx-font-weight: bold;");
            tabla.add(nombre, 0, fila);
            tabla.add(new Label(String.valueOf(m.generados(t))), 1, fila);
            tabla.add(new Label(String.valueOf(m.resueltos(t))), 2, fila);
            tabla.add(new Label(String.valueOf(m.expirados(t))), 3, fila);
            tabla.add(new Label(m.respuestaPromedio(t) > 0 ? segundos(m.respuestaPromedio(t)) : "—"), 4, fila);
            fila++;
        }
        return tabla;
    }

    private static String nombreCorto(TipoIncidente t) {
        return switch (t) {
            case ROBO -> "Robo";
            case EMERGENCIA_MEDICA -> "Médica";
            case INCENDIO -> "Incendio";
            case ACCIDENTE -> "Accidente";
        };
    }

    private static String porcentaje(double fraccion) {
        return String.format(Locale.ROOT, "%.0f %%", fraccion * 100);
    }

    private static String segundos(double s) {
        return String.format(Locale.ROOT, "%.1f s", s);
    }
}
