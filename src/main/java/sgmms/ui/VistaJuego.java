package sgmms.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import sgmms.estructuras.ListaEnlazada;
import sgmms.modelo.EstadoIncidente;
import sgmms.modelo.EstadoUnidad;
import sgmms.modelo.Estacion;
import sgmms.modelo.Evento;
import sgmms.modelo.Incidente;
import sgmms.modelo.TipoUnidad;
import sgmms.modelo.Unidad;
import sgmms.simulacion.Simulacion;
import sgmms.simulacion.Simulacion.Recomendacion;

/** Pantalla del turno: mapa en vivo a la izquierda y consola de despacho a la derecha. */
final class VistaJuego implements App.Vista {

    private static final double[] VELOCIDADES = {1, 2, 4};
    private static final double REFRESCO_PANEL = 0.25;

    private final App app;
    private final Simulacion sim;
    private final Renderizador renderizador;
    private final Renderizador.Seleccion sel = new Renderizador.Seleccion();

    private final BorderPane raiz = new BorderPane();
    private final Canvas canvas = new Canvas();
    private final StackPane capaMapa = new StackPane();
    private final VBox avisos = new VBox(6);
    private final Label bandaManual = new Label();
    private Node ayuda;

    // Barra superior
    private final Label reloj = new Label();
    private final ProgressBar progresoTurno = new ProgressBar(0);
    private final Label puntaje = new Label();
    private final HBox fallos = new HBox(4);
    private final Button[] botonesVelocidad = new Button[VELOCIDADES.length];
    private final Button botonPausa = new Button("Pausa");
    private final Button botonTrafico = new Button("Tráfico");

    // Panel lateral
    private final VBox listaTriage = new VBox(5);
    private final VBox detalle = new VBox(8);
    private final VBox listaFlota = new VBox(5);
    private final VBox listaBitacora = new VBox(3);
    private final Label tituloTriage = new Label();

    private AnimationTimer bucle;
    private long ultimoNanos;
    private double acumulado;
    private double relojPanel;
    private int indiceVelocidad;
    private boolean pausado;
    private boolean pausadoAntesDeAyuda;
    private boolean congelado;
    private boolean finMostrado;
    private boolean arriba, abajo, izquierda, derecha;

    VistaJuego(App app, Simulacion sim) {
        this.app = app;
        this.sim = sim;
        this.renderizador = new Renderizador(sim.getCiudad());
        construir();
        iniciarBucle();
    }

    @Override
    public Parent raiz() {
        return raiz;
    }

    @Override
    public void detener() {
        if (bucle != null) bucle.stop();
    }

    Simulacion getSimulacion() {
        return sim;
    }

    // =====================================================================
    // Construcción
    // =====================================================================

    private void construir() {
        raiz.setTop(barraSuperior());
        raiz.setCenter(mapa());
        raiz.setRight(panelLateral());

        raiz.addEventFilter(KeyEvent.KEY_PRESSED, this::teclaPresionada);
        raiz.addEventFilter(KeyEvent.KEY_RELEASED, this::teclaSoltada);
        raiz.sceneProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null) canvas.requestFocus();
        });
        refrescarPanel();
    }

    private Node barraSuperior() {
        Label marca = new Label("SGMMS");
        marca.getStyleClass().add("marca");
        Label sub = new Label("Centro de despacho · " + sim.getDificultad().getNombre());
        sub.getStyleClass().addAll("suave", "pequeno");
        VBox identidad = new VBox(0, marca, sub);

        reloj.getStyleClass().add("reloj");
        progresoTurno.setPrefWidth(120);
        progresoTurno.setPrefHeight(6);
        progresoTurno.setStyle("-fx-accent: #38d4ff;");
        VBox turno = new VBox(3, etiqueta("TURNO RESTANTE"), reloj, progresoTurno);

        puntaje.getStyleClass().add("dato");
        VBox bloquePuntaje = new VBox(1, etiqueta("PUNTAJE"), puntaje);
        VBox bloqueFallos = new VBox(5, etiqueta("FALLOS"), fallos);
        fallos.setAlignment(Pos.CENTER_LEFT);

        HBox controles = new HBox(6);
        controles.setAlignment(Pos.CENTER);
        botonPausa.getStyleClass().add("boton");
        botonPausa.setFocusTraversable(false);
        botonPausa.setOnAction(e -> alternarPausa());
        controles.getChildren().add(botonPausa);
        for (int i = 0; i < VELOCIDADES.length; i++) {
            int indice = i;
            Button b = new Button("x" + (int) VELOCIDADES[i]);
            b.getStyleClass().add("boton");
            b.setFocusTraversable(false);
            b.setOnAction(e -> cambiarVelocidad(indice));
            botonesVelocidad[i] = b;
            controles.getChildren().add(b);
        }
        botonTrafico.getStyleClass().add("boton");
        botonTrafico.setFocusTraversable(false);
        botonTrafico.setOnAction(e -> alternarTrafico());
        Button botonAyuda = new Button("Ayuda");
        botonAyuda.getStyleClass().add("boton");
        botonAyuda.setFocusTraversable(false);
        botonAyuda.setOnAction(e -> mostrarAyuda());
        Button botonMenu = new Button("Menú");
        botonMenu.getStyleClass().add("boton");
        botonMenu.setFocusTraversable(false);
        botonMenu.setOnAction(e -> app.mostrarMenu());
        Region separador = new Region();
        separador.setMinWidth(8);
        controles.getChildren().addAll(separador, botonTrafico, botonAyuda, botonMenu);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(identidad, espacio, turno, bloquePuntaje, bloqueFallos, controles);
        barra.getStyleClass().add("barra-superior");
        cambiarVelocidad(0);
        alternarTraficoVisual();
        return barra;
    }

    private Node mapa() {
        Pane contenedor = new Pane(canvas);
        canvas.widthProperty().bind(contenedor.widthProperty());
        canvas.heightProperty().bind(contenedor.heightProperty());
        canvas.setFocusTraversable(true);
        canvas.setOnMouseMoved(this::moverRaton);
        canvas.setOnMousePressed(this::clicMapa);
        contenedor.setMinSize(300, 300);

        Label atajos = new Label("Clic: seleccionar · Unidad + clic en incidente: despachar · Enter: despachar recomendadas · "
                + "C: conducir · Espacio: pausa · Tab: más urgente");
        atajos.getStyleClass().addAll("atajos", "suave", "pequeno");
        atajos.setMouseTransparent(true);
        StackPane.setAlignment(atajos, Pos.BOTTOM_LEFT);
        StackPane.setMargin(atajos, new Insets(0, 0, 10, 10));

        bandaManual.getStyleClass().add("banda-manual");
        bandaManual.setVisible(false);
        bandaManual.setMouseTransparent(true);
        StackPane.setAlignment(bandaManual, Pos.TOP_CENTER);
        StackPane.setMargin(bandaManual, new Insets(10, 0, 0, 0));

        avisos.setAlignment(Pos.BOTTOM_CENTER);
        avisos.setMouseTransparent(true);
        avisos.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(avisos, Pos.BOTTOM_CENTER);
        StackPane.setMargin(avisos, new Insets(0, 0, 46, 0));

        capaMapa.getChildren().addAll(contenedor, atajos, bandaManual, avisos);
        return capaMapa;
    }

    private Node panelLateral() {
        tituloTriage.getStyleClass().add("titulo-seccion");
        VBox triage = seccion(tituloTriage, listaTriage);
        VBox seccionDetalle = seccion(titulo("DETALLE"), detalle);
        VBox flota = seccion(titulo("FLOTA"), listaFlota);
        VBox bitacora = seccion(titulo("BITÁCORA"), listaBitacora);

        VBox contenido = new VBox(triage, seccionDetalle, flota, bitacora);
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFocusTraversable(false);
        scroll.setPrefWidth(372);
        scroll.setMinWidth(340);
        scroll.getStyleClass().add("panel");
        return scroll;
    }

    // =====================================================================
    // Bucle de juego
    // =====================================================================

    private void iniciarBucle() {
        bucle = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                double dt = ultimoNanos == 0 ? 0 : Math.min(0.1, (ahora - ultimoNanos) / 1e9);
                ultimoNanos = ahora;
                fotograma(dt);
            }
        };
        bucle.start();
    }

    /** Detiene el avance de la simulación sin mostrar el velo de pausa (para capturas). */
    void congelar(boolean valor) {
        congelado = valor;
    }

    private void fotograma(double dt) {
        if (!pausado && !congelado && !sim.terminada()) {
            sim.setDireccionManual((derecha ? 1 : 0) - (izquierda ? 1 : 0), (abajo ? 1 : 0) - (arriba ? 1 : 0));
            acumulado += dt * VELOCIDADES[indiceVelocidad];
            int pasos = 0;
            while (acumulado >= Simulacion.PASO && pasos++ < 40) {
                sim.avanzar(Simulacion.PASO);
                acumulado -= Simulacion.PASO;
            }
        }
        if (sel.incidente != null && !sel.incidente.getEstado().activo()) sel.incidente = null;

        Renderizador.Camara cam = camara();
        renderizador.dibujar(canvas.getGraphicsContext2D(), sim, cam, sel, canvas.getWidth(), canvas.getHeight());
        if (pausado) dibujarPausa();

        relojPanel -= dt;
        if (relojPanel <= 0) {
            relojPanel = REFRESCO_PANEL;
            refrescarPanel();
        }
        actualizarBarra();

        if (sim.terminada() && !finMostrado) {
            finMostrado = true;
            refrescarPanel();
            PauseTransition espera = new PauseTransition(Duration.seconds(1.6));
            espera.setOnFinished(e -> app.mostrarInforme(sim));
            espera.play();
        }
    }

    private Renderizador.Camara camara() {
        return Renderizador.Camara.ajustar(canvas.getWidth(), canvas.getHeight(), 14);
    }

    private void dibujarPausa() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.rgb(6, 9, 13, 0.55));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Paleta.TEXTO);
        gc.setFont(Font.font("System", FontWeight.BOLD, 28));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("EN PAUSA", canvas.getWidth() / 2, canvas.getHeight() / 2);
        gc.setFont(Font.font("System", 14));
        gc.setFill(Paleta.TEXTO_SUAVE);
        gc.fillText("Espacio para continuar", canvas.getWidth() / 2, canvas.getHeight() / 2 + 26);
    }

    // =====================================================================
    // Entrada
    // =====================================================================

    private void teclaPresionada(KeyEvent e) {
        if (e.getTarget() instanceof TextInputControl) return;
        KeyCode k = e.getCode();
        if (ayuda != null) {
            if (k == KeyCode.ESCAPE || k == KeyCode.H || k == KeyCode.F1) cerrarAyuda();
            e.consume();
            return;
        }
        switch (k) {
            case W, UP -> arriba = true;
            case S, DOWN -> abajo = true;
            case A, LEFT -> izquierda = true;
            case D, RIGHT -> derecha = true;
            case SPACE -> alternarPausa();
            case DIGIT1, NUMPAD1 -> cambiarVelocidad(0);
            case DIGIT2, NUMPAD2 -> cambiarVelocidad(1);
            case DIGIT3, NUMPAD3 -> cambiarVelocidad(2);
            case T -> alternarTrafico();
            case TAB -> seleccionarMasUrgente();
            case ENTER -> despacharRecomendadas();
            case C -> alternarConduccion();
            case H, F1 -> mostrarAyuda();
            case ESCAPE -> {
                if (sim.getUnidadManual() != null) sim.soltarControl();
                else limpiarSeleccion();
            }
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void teclaSoltada(KeyEvent e) {
        switch (e.getCode()) {
            case W, UP -> arriba = false;
            case S, DOWN -> abajo = false;
            case A, LEFT -> izquierda = false;
            case D, RIGHT -> derecha = false;
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void moverRaton(MouseEvent e) {
        Renderizador.Camara cam = camara();
        double x = cam.mundoX(e.getX());
        double y = cam.mundoY(e.getY());
        sel.unidadBajoCursor = unidadEn(x, y);
        sel.incidenteBajoCursor = sel.unidadBajoCursor == null ? incidenteEn(x, y) : null;
        canvas.setCursor(sel.unidadBajoCursor != null || sel.incidenteBajoCursor != null ? Cursor.HAND : Cursor.DEFAULT);
    }

    private void clicMapa(MouseEvent e) {
        canvas.requestFocus();
        if (e.getButton() == MouseButton.SECONDARY) {
            limpiarSeleccion();
            return;
        }
        Renderizador.Camara cam = camara();
        double x = cam.mundoX(e.getX());
        double y = cam.mundoY(e.getY());
        Unidad u = unidadEn(x, y);
        if (u != null) {
            seleccionarUnidad(u);
            return;
        }
        Incidente inc = incidenteEn(x, y);
        if (inc != null) {
            // Con una unidad seleccionada, clic en un incidente que la necesita = despacharla
            if (sel.unidad != null && inc.getEstado().pendiente() && inc.necesita(sel.unidad.getTipo())
                    && sel.unidad.getEstado() != EstadoUnidad.EN_ESCENA) {
                despachar(sel.unidad, inc);
                sel.unidad = null;
            }
            seleccionarIncidente(inc);
            return;
        }
        sel.unidad = null;
        refrescarPanel();
    }

    private Unidad unidadEn(double x, double y) {
        Unidad mejor = null;
        double mejorDist = 14;
        for (Unidad u : sim.getUnidades()) {
            double[] p = Renderizador.posicion(u);
            double d = Math.hypot(p[0] - x, p[1] - y);
            if (d < mejorDist) {
                mejorDist = d;
                mejor = u;
            }
        }
        return mejor;
    }

    private Incidente incidenteEn(double x, double y) {
        Incidente mejor = null;
        double mejorDist = 20;
        for (Incidente inc : sim.getActivos()) {
            double d = Math.hypot(inc.getX() - x, inc.getY() - y);
            if (d < mejorDist) {
                mejorDist = d;
                mejor = inc;
            }
        }
        return mejor;
    }

    // =====================================================================
    // Acciones
    // =====================================================================

    void seleccionarIncidente(Incidente inc) {
        sel.incidente = inc;
        refrescarPanel();
    }

    void seleccionarUnidad(Unidad u) {
        sel.unidad = sel.unidad == u ? null : u;
        refrescarPanel();
    }

    private void limpiarSeleccion() {
        sel.incidente = null;
        sel.unidad = null;
        refrescarPanel();
    }

    private void seleccionarMasUrgente() {
        ListaEnlazada<Incidente> cola = sim.colaTriage();
        if (cola.estaVacia()) return;
        int i = 0;
        for (Incidente inc : cola) {
            if (inc == sel.incidente) break;
            i++;
        }
        seleccionarIncidente(cola.obtener(i >= cola.tamano() - 1 ? 0 : i + 1));
    }

    void despacharRecomendadas() {
        Incidente inc = sel.incidente;
        if (inc == null) inc = sim.incidenteMasUrgente();
        if (inc == null) {
            avisar("No hay incidentes esperando unidades", false);
            return;
        }
        sel.incidente = inc;
        if (inc.faltantes().estaVacia()) {
            avisar(inc.etiqueta() + " ya tiene todas sus unidades en camino", false);
            return;
        }
        int enviadas = sim.asignarRecomendadas(inc);
        if (enviadas == 0) avisar("No hay unidades disponibles para " + inc.etiqueta(), false);
        else avisar(enviadas + (enviadas == 1 ? " unidad despachada a " : " unidades despachadas a ") + inc.etiqueta(), true);
        refrescarPanel();
    }

    private void despachar(Unidad u, Incidente inc) {
        String error = sim.asignar(u, inc);
        if (error != null) avisar(error, false);
        else avisar(u.getId() + " en camino a " + inc.etiqueta(), true);
        refrescarPanel();
    }

    void alternarConduccion() {
        if (sim.getUnidadManual() != null) {
            sim.soltarControl();
            refrescarPanel();
            return;
        }
        if (sel.unidad == null) {
            avisar("Selecciona una unidad para conducirla", false);
            return;
        }
        String error = sim.tomarControl(sel.unidad);
        if (error != null) avisar(error, false);
        refrescarPanel();
    }

    private void alternarPausa() {
        pausado = !pausado;
        botonPausa.setText(pausado ? "Continuar" : "Pausa");
        botonPausa.getStyleClass().remove("boton-activo");
        if (pausado) botonPausa.getStyleClass().add("boton-activo");
    }

    private void cambiarVelocidad(int indice) {
        indiceVelocidad = indice;
        for (int i = 0; i < botonesVelocidad.length; i++) {
            botonesVelocidad[i].getStyleClass().remove("boton-activo");
            if (i == indice) botonesVelocidad[i].getStyleClass().add("boton-activo");
        }
    }

    private void alternarTrafico() {
        sel.capaTrafico = !sel.capaTrafico;
        alternarTraficoVisual();
    }

    private void alternarTraficoVisual() {
        botonTrafico.getStyleClass().remove("boton-activo");
        if (sel.capaTrafico) botonTrafico.getStyleClass().add("boton-activo");
    }

    private void mostrarAyuda() {
        if (ayuda != null) return;
        pausadoAntesDeAyuda = pausado;
        if (!pausado) alternarPausa();
        ayuda = Ayuda.crear(this::cerrarAyuda);
        capaMapa.getChildren().add(ayuda);
    }

    private void cerrarAyuda() {
        capaMapa.getChildren().remove(ayuda);
        ayuda = null;
        if (pausado && !pausadoAntesDeAyuda) alternarPausa();
        canvas.requestFocus();
    }

    private void avisar(String texto, boolean exito) {
        Label aviso = new Label(texto);
        aviso.getStyleClass().addAll("aviso", exito ? "aviso-exito" : "aviso-error");
        avisos.getChildren().add(aviso);
        if (avisos.getChildren().size() > 3) avisos.getChildren().remove(0);
        PauseTransition vida = new PauseTransition(Duration.seconds(2.6));
        vida.setOnFinished(e -> avisos.getChildren().remove(aviso));
        vida.play();
    }

    // =====================================================================
    // Barra superior y panel
    // =====================================================================

    private void actualizarBarra() {
        reloj.setText(formatoReloj(sim.tiempoRestante()));
        progresoTurno.setProgress(sim.progreso());
        puntaje.setText(String.format("%,d", sim.getPuntaje()).replace(',', '.'));

        int permitidos = sim.getDificultad().getFallosPermitidos();
        if (fallos.getChildren().size() != permitidos) {
            fallos.getChildren().clear();
            for (int i = 0; i < permitidos; i++) fallos.getChildren().add(new Region());
        }
        for (int i = 0; i < permitidos; i++) {
            Region r = (Region) fallos.getChildren().get(i);
            boolean usado = i < sim.getFallos();
            r.setMinSize(12, 12);
            r.setMaxSize(12, 12);
            r.setStyle("-fx-background-radius: 6; -fx-background-color: " + (usado ? "#ff4d5e" : "#253041") + ";");
        }

        Unidad manual = sim.getUnidadManual();
        bandaManual.setVisible(manual != null);
        if (manual != null) {
            String destino = manual.getIncidente() != null ? "rumbo a " + manual.getIncidente().etiqueta() : "sin despacho asignado";
            bandaManual.setText("Conduciendo " + manual.getId() + " · " + destino + " · WASD / flechas · C o Esc para soltar el volante");
        }
    }

    private void refrescarPanel() {
        refrescarTriage();
        refrescarDetalle();
        refrescarFlota();
        refrescarBitacora();
    }

    private void refrescarTriage() {
        ListaEnlazada<Incidente> cola = sim.colaTriage();
        tituloTriage.setText("COLA DE TRIAGE · " + cola.tamano() + (cola.tamano() == 1 ? " PENDIENTE" : " PENDIENTES"));
        listaTriage.getChildren().clear();
        for (Incidente inc : cola) listaTriage.getChildren().add(filaIncidente(inc));
        for (Incidente inc : sim.getActivos()) {
            if (inc.getEstado() == EstadoIncidente.EN_ATENCION) listaTriage.getChildren().add(filaIncidente(inc));
        }
        if (listaTriage.getChildren().isEmpty()) listaTriage.getChildren().add(vacio("Sin incidentes activos. La ciudad está tranquila… por ahora."));
    }

    private Node filaIncidente(Incidente inc) {
        double t = sim.getTiempo();
        Color color = Paleta.incidente(inc.getTipo());
        Region marca = new Region();
        marca.setMinSize(4, 30);
        marca.setStyle("-fx-background-color: " + Paleta.hex(color) + "; -fx-background-radius: 2;");

        Label nombre = new Label(inc.etiqueta());
        nombre.getStyleClass().add("negrita");
        Label info = new Label("Gravedad " + inc.getGravedad() + " · " + resumenUnidades(inc));
        info.getStyleClass().addAll("suave", "pequeno");
        VBox textos = new VBox(1, nombre, info);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Node derecha;
        if (inc.getEstado().pendiente()) {
            double restante = inc.restante(t);
            Label tiempo = new Label(Math.max(0, (int) Math.ceil(restante)) + " s");
            tiempo.getStyleClass().add("negrita");
            tiempo.setStyle("-fx-text-fill: " + Paleta.hex(Paleta.urgencia(inc.urgencia(t))) + ";");
            ProgressBar barra = new ProgressBar(1 - inc.urgencia(t));
            barra.setPrefWidth(54);
            barra.setPrefHeight(5);
            barra.setStyle("-fx-accent: " + Paleta.hex(Paleta.urgencia(inc.urgencia(t))) + ";");
            VBox v = new VBox(3, tiempo, barra);
            v.setAlignment(Pos.CENTER_RIGHT);
            derecha = v;
        } else {
            Label atencion = new Label("En atención " + Math.round(inc.fraccionAtencion() * 100) + " %");
            atencion.getStyleClass().add("pequeno");
            atencion.setStyle("-fx-text-fill: #38d4ff;");
            derecha = atencion;
        }

        HBox fila = new HBox(marca, textos, derecha);
        fila.getStyleClass().add("fila");
        if (inc == sel.incidente) fila.getStyleClass().add("fila-seleccionada");
        fila.setOnMousePressed(e -> seleccionarIncidente(inc));
        return fila;
    }

    private String resumenUnidades(Incidente inc) {
        StringBuilder sb = new StringBuilder();
        ListaEnlazada<TipoUnidad> faltan = inc.faltantes();
        if (faltan.estaVacia()) return inc.getEstado() == EstadoIncidente.EN_ATENCION ? "equipo completo" : "unidades en camino";
        sb.append("falta: ");
        boolean primero = true;
        for (TipoUnidad t : faltan) {
            if (!primero) sb.append(", ");
            sb.append(t.getNombre().toLowerCase());
            primero = false;
        }
        return sb.toString();
    }

    private void refrescarDetalle() {
        detalle.getChildren().clear();
        sel.recomendaciones = new ListaEnlazada<>();
        if (sel.unidad != null) detalle.getChildren().add(detalleUnidad(sel.unidad));
        if (sel.incidente != null) detalle.getChildren().add(detalleIncidente(sel.incidente));
        if (detalle.getChildren().isEmpty()) {
            detalle.getChildren().add(vacio("Selecciona un incidente o una unidad en el mapa o en las listas."));
        }
    }

    private Node detalleIncidente(Incidente inc) {
        VBox caja = new VBox(6);
        caja.getStyleClass().add("tarjeta-detalle");
        Label nombre = new Label(inc.etiqueta());
        nombre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + Paleta.hex(Paleta.incidente(inc.getTipo())) + ";");
        Label estado = new Label(inc.getEstado().getNombre() + " · gravedad " + inc.getGravedad()
                + (inc.isEscalado() ? " (se propagó)" : "")
                + (inc.getTipo().cierraVia() ? " · calle cerrada" : ""));
        estado.getStyleClass().addAll("suave", "pequeno");
        caja.getChildren().addAll(nombre, estado);

        if (inc.getEstado().pendiente()) {
            Label plazo = new Label("Plazo: " + Math.max(0, (int) Math.ceil(inc.restante(sim.getTiempo()))) + " s");
            plazo.getStyleClass().add("negrita");
            caja.getChildren().add(plazo);
        }

        for (Unidad u : inc.getAsignadas()) {
            String texto = u.getEstado().getNombre();
            if (u.getEstado() == EstadoUnidad.EN_CAMINO && !u.isManual()) {
                texto += " · llega en ~" + Math.round(u.distanciaRestante() / u.getTipo().getVelocidad()) + " s";
            }
            caja.getChildren().add(lineaUnidad(u, texto));
        }

        if (inc.getEstado().pendiente() && !inc.faltantes().estaVacia()) {
            sel.recomendaciones = sim.recomendar(inc);
            Label titulo = new Label("RECOMENDADAS (menor ETA con tráfico actual)");
            titulo.getStyleClass().add("titulo-seccion");
            caja.getChildren().add(titulo);
            if (sel.recomendaciones.estaVacia()) {
                caja.getChildren().add(vacio("No hay unidades libres del tipo requerido. Puedes reasignar una que vaya en camino."));
            }
            for (Recomendacion r : sel.recomendaciones) {
                Unidad u = r.unidad();
                Button enviar = new Button("Enviar");
                enviar.getStyleClass().addAll("boton", "boton-mini");
                enviar.setFocusTraversable(false);
                enviar.setOnMousePressed(e -> despachar(u, inc));
                Label texto = new Label(u.getId() + " · ETA " + Math.round(r.eta()) + " s · " + Math.round(r.plan().longitud()) + " px");
                HBox.setHgrow(texto, Priority.ALWAYS);
                texto.setMaxWidth(Double.MAX_VALUE);
                HBox fila = new HBox(8, punto(Paleta.unidad(u.getTipo())), texto, enviar);
                fila.setAlignment(Pos.CENTER_LEFT);
                caja.getChildren().add(fila);
            }
            if (!sel.recomendaciones.estaVacia()) {
                Recomendacion mejor = sel.recomendaciones.primero();
                Simulacion.ComparacionBusqueda c = sim.compararBusqueda(mejor.unidad(), inc);
                if (c.nodosDijkstra() > 0) {
                    Label analisis = new Label("Ruta de " + mejor.unidad().getId() + ": A* expandió " + c.nodosAEstrella()
                            + " nodos, Dijkstra habría expandido " + c.nodosDijkstra() + ".");
                    analisis.getStyleClass().addAll("suave", "pequeno");
                    analisis.setWrapText(true);
                    caja.getChildren().add(analisis);
                }
                Button todas = new Button("Despachar recomendadas (Enter)");
                todas.getStyleClass().addAll("boton", "boton-primario");
                todas.setMaxWidth(Double.MAX_VALUE);
                todas.setFocusTraversable(false);
                todas.setOnMousePressed(e -> despacharRecomendadas());
                caja.getChildren().add(todas);
            }
        }
        return caja;
    }

    private Node detalleUnidad(Unidad u) {
        VBox caja = new VBox(6);
        caja.getStyleClass().add("tarjeta-detalle");
        Label nombre = new Label(u.getId() + " · " + u.getTipo().getNombre());
        nombre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        String destino = u.getIncidente() != null ? " → " + u.getIncidente().etiqueta() : "";
        Label estado = new Label(u.getEstado().getNombre() + destino + (u.isManual() ? " · manual" : ""));
        estado.getStyleClass().addAll("suave", "pequeno");
        Label datos = new Label(u.getDespachos() + " despachos · " + Math.round(u.getDistanciaRecorrida()) + " px recorridos");
        datos.getStyleClass().addAll("suave", "pequeno");
        caja.getChildren().addAll(nombre, estado, datos);

        HBox acciones = new HBox(6);
        if (u.getEstado() != EstadoUnidad.EN_ESCENA) {
            Button conducir = new Button(u.isManual() ? "Soltar volante (C)" : "Conducir (C)");
            conducir.getStyleClass().addAll("boton", "boton-mini");
            conducir.setFocusTraversable(false);
            conducir.setOnMousePressed(e -> alternarConduccion());
            acciones.getChildren().add(conducir);
        }
        if (u.getEstado() == EstadoUnidad.EN_CAMINO) {
            Button regresar = new Button("Cancelar y regresar");
            regresar.getStyleClass().addAll("boton", "boton-mini");
            regresar.setFocusTraversable(false);
            regresar.setOnMousePressed(e -> {
                String error = sim.ordenarRegreso(u);
                if (error != null) avisar(error, false);
                refrescarPanel();
            });
            acciones.getChildren().add(regresar);
        }
        if (!acciones.getChildren().isEmpty()) caja.getChildren().add(acciones);
        if (u.getEstado() != EstadoUnidad.EN_ESCENA) {
            Label pista = new Label("Haz clic en un incidente del mapa para despacharla.");
            pista.getStyleClass().addAll("suave", "pequeno");
            caja.getChildren().add(pista);
        }
        return caja;
    }

    private void refrescarFlota() {
        listaFlota.getChildren().clear();
        for (Estacion est : sim.getCiudad().getEstaciones()) {
            Label cabecera = new Label(est.getNombre() + " · " + est.unidadesEnBase() + "/" + est.getUnidades().tamano() + " en base");
            cabecera.getStyleClass().addAll("suave", "pequeno");
            cabecera.setPadding(new Insets(4, 0, 0, 2));
            listaFlota.getChildren().add(cabecera);
            for (Unidad u : est.getUnidades()) {
                String texto = u.getEstado().getNombre();
                if (u.getIncidente() != null) texto += " · " + u.getIncidente().etiqueta();
                if (u.isManual()) texto += " · manual";
                listaFlota.getChildren().add(lineaUnidad(u, texto));
            }
        }
    }

    private Node lineaUnidad(Unidad u, String texto) {
        Label id = new Label(u.getId());
        id.getStyleClass().add("negrita");
        id.setMinWidth(30);
        Label estado = new Label(texto);
        estado.getStyleClass().add("pequeno");
        estado.setStyle("-fx-text-fill: " + colorEstado(u) + ";");
        HBox fila = new HBox(8, punto(Paleta.unidad(u.getTipo())), id, estado);
        fila.getStyleClass().add("fila");
        fila.setPadding(new Insets(5, 9, 5, 9));
        if (u == sel.unidad) fila.getStyleClass().add("fila-seleccionada");
        fila.setOnMousePressed(e -> seleccionarUnidad(u));
        return fila;
    }

    private static String colorEstado(Unidad u) {
        if (u.isManual()) return "#38d4ff";
        return switch (u.getEstado()) {
            case EN_BASE -> "#3ddc84";
            case EN_CAMINO -> "#ffb020";
            case EN_ESCENA -> "#ff8a5c";
            case REGRESANDO -> "#8a94a6";
        };
    }

    private void refrescarBitacora() {
        listaBitacora.getChildren().clear();
        ListaEnlazada<Evento> eventos = sim.getBitacora();
        int mostrados = 0;
        for (int i = eventos.tamano() - 1; i >= 0 && mostrados < 14; i--, mostrados++) {
            Evento ev = eventos.obtener(i);
            Label hora = new Label(formatoReloj(ev.tiempo()));
            hora.getStyleClass().addAll("suave", "pequeno");
            hora.setMinWidth(38);
            Label texto = new Label(ev.texto());
            texto.getStyleClass().add("pequeno");
            texto.setWrapText(true);
            texto.setStyle("-fx-text-fill: " + switch (ev.nivel()) {
                case INFO -> "#c4ccd8";
                case EXITO -> "#3ddc84";
                case ALERTA -> "#ffb020";
            } + ";");
            HBox fila = new HBox(6, hora, texto);
            listaBitacora.getChildren().add(fila);
        }
    }

    // =====================================================================
    // Utilidades de interfaz
    // =====================================================================

    private static VBox seccion(Label titulo, Node contenido) {
        VBox s = new VBox(titulo, contenido);
        s.getStyleClass().add("seccion");
        return s;
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo-seccion");
        return l;
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta-dato");
        return l;
    }

    private static Label vacio(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("vacio");
        l.setWrapText(true);
        return l;
    }

    private static Region punto(Color color) {
        Region r = new Region();
        r.setMinSize(9, 9);
        r.setMaxSize(9, 9);
        r.setStyle("-fx-background-radius: 5; -fx-background-color: " + Paleta.hex(color) + ";");
        return r;
    }

    static String formatoReloj(double segundos) {
        int s = (int) Math.max(0, Math.floor(segundos));
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
