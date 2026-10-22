package sgmms.ui;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import sgmms.estructuras.ListaEnlazada;
import sgmms.grafo.Via;
import sgmms.modelo.Efecto;
import sgmms.modelo.EstadoIncidente;
import sgmms.modelo.EstadoUnidad;
import sgmms.modelo.Estacion;
import sgmms.modelo.Incidente;
import sgmms.modelo.Plan;
import sgmms.modelo.PuntoVia;
import sgmms.modelo.TipoUnidad;
import sgmms.modelo.Tramo;
import sgmms.modelo.Unidad;
import sgmms.modelo.VehiculoCivil;
import sgmms.simulacion.Ciudad;
import sgmms.simulacion.Simulacion;

/** Dibuja el estado de una simulación sobre un canvas, en coordenadas del mundo. */
final class Renderizador {

    /** Distancia del centro de la calle al centro de cada carril. */
    static final double CARRIL = 7;

    /** Escala y desplazamiento para encajar el mundo en el canvas. */
    record Camara(double escala, double ox, double oy) {
        static Camara ajustar(double ancho, double alto, double margen) {
            double escala = Math.min((ancho - 2 * margen) / Ciudad.ANCHO, (alto - 2 * margen) / Ciudad.ALTO);
            escala = Math.max(0.1, escala);
            return new Camara(escala, (ancho - Ciudad.ANCHO * escala) / 2, (alto - Ciudad.ALTO * escala) / 2);
        }

        double mundoX(double pantallaX) {
            return (pantallaX - ox) / escala;
        }

        double mundoY(double pantallaY) {
            return (pantallaY - oy) / escala;
        }
    }

    /** Lo que la vista quiere resaltar en este fotograma. */
    static final class Seleccion {
        Incidente incidente;
        Unidad unidad;
        Incidente incidenteBajoCursor;
        Unidad unidadBajoCursor;
        boolean capaTrafico = true;
        ListaEnlazada<Simulacion.Recomendacion> recomendaciones = new ListaEnlazada<>();
    }

    private final Image plano;

    Renderizador(Ciudad ciudad) {
        plano = PlanoCiudad.generar(ciudad);
    }

    void dibujar(GraphicsContext gc, Simulacion sim, Camara cam, Seleccion sel, double ancho, double alto) {
        gc.setFill(Paleta.FONDO);
        gc.fillRect(0, 0, ancho, alto);

        gc.save();
        gc.translate(cam.ox(), cam.oy());
        gc.scale(cam.escala(), cam.escala());
        gc.drawImage(plano, 0, 0, Ciudad.ANCHO, Ciudad.ALTO);

        double t = sim.getTiempo();
        if (sel.capaTrafico) capaTrafico(gc, sim);
        cierres(gc, sim);
        for (VehiculoCivil auto : sim.getTrafico().getAutos()) autoCivil(gc, auto);

        rutas(gc, sim, sel);
        for (Estacion e : sim.getCiudad().getEstaciones()) cupoEstacion(gc, e);
        for (Incidente inc : sim.getActivos()) incidente(gc, inc, t, inc == sel.incidente, inc == sel.incidenteBajoCursor);
        for (Unidad u : sim.getUnidades()) unidad(gc, u, t, u == sel.unidad || u == sel.unidadBajoCursor);
        gps(gc, sim);
        for (Efecto e : sim.getEfectos()) efecto(gc, e);
        gc.restore();
    }

    // ---- Capas ----

    private void capaTrafico(GraphicsContext gc, Simulacion sim) {
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(3.2);
        for (Via v : sim.getCiudad().getGrafo().vias()) {
            if (v.estaCerrada()) continue;
            double densidad = v.densidad();
            if (densidad < 0.12) continue;
            double f = v.factorCongestion();
            // El flujo libre se insinúa; lo que importa (lento o trancado) resalta
            Color c = f > 0.7 ? Paleta.EXITO.deriveColor(0, 1, 1, 0.22)
                    : f > 0.45 ? Paleta.ALERTA.deriveColor(0, 1, 1, 0.7) : Paleta.PELIGRO.deriveColor(0, 1, 1, 0.8);
            gc.setStroke(c);
            double ini = Math.min(Ciudad.MEDIO_ANCHO_CALLE, v.getLongitud() / 2);
            double fin = v.getLongitud() - ini;
            double nx = -v.getDirY() * CARRIL;
            double ny = v.getDirX() * CARRIL;
            gc.strokeLine(v.xEn(ini) + nx, v.yEn(ini) + ny, v.xEn(fin) + nx, v.yEn(fin) + ny);
        }
    }

    private void cierres(GraphicsContext gc, Simulacion sim) {
        for (Incidente inc : sim.getActivos()) {
            if (!inc.getTipo().cierraVia()) continue;
            Via v = inc.getUbicacion().via();
            gc.setStroke(Paleta.PELIGRO.deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(2.5);
            gc.setLineDashes(6, 6);
            double nx = -v.getDirY() * Ciudad.MEDIO_ANCHO_CALLE;
            double ny = v.getDirX() * Ciudad.MEDIO_ANCHO_CALLE;
            for (int lado = -1; lado <= 1; lado += 2) {
                gc.strokeLine(v.getOrigen().getX() + nx * lado, v.getOrigen().getY() + ny * lado,
                        v.getDestino().getX() + nx * lado, v.getDestino().getY() + ny * lado);
            }
            gc.setLineDashes((double[]) null);
            double angulo = Math.atan2(v.getDirY(), v.getDirX());
            double s = inc.getUbicacion().s();
            Iconos.barrera(gc, v.xEn(s - 26), v.yEn(s - 26), angulo, 30);
            Iconos.barrera(gc, v.xEn(s + 26), v.yEn(s + 26), angulo, 30);
        }
    }

    private void autoCivil(GraphicsContext gc, VehiculoCivil auto) {
        Via v = auto.getVia();
        double desvio = CARRIL + (auto.getOrillado() > 0 ? 3.5 : 0);
        double x = auto.getX() - v.getDirY() * desvio;
        double y = auto.getY() + v.getDirX() * desvio;
        Color color = Paleta.AUTOS[auto.getColor()];
        vehiculo(gc, x, y, Math.atan2(v.getDirY(), v.getDirX()), 12, 6.4, color, color.brighter());
        if (auto.getVelocidad() < 2) { // luces de freno
            gc.save();
            gc.translate(x, y);
            gc.rotate(Math.toDegrees(Math.atan2(v.getDirY(), v.getDirX())));
            gc.setFill(Color.web("#ff3b3b"));
            gc.fillRect(-6.4, -3, 1.4, 1.6);
            gc.fillRect(-6.4, 1.4, 1.4, 1.6);
            gc.restore();
        }
    }

    private void rutas(GraphicsContext gc, Simulacion sim, Seleccion sel) {
        // Vista previa de las rutas recomendadas para el incidente seleccionado
        for (Simulacion.Recomendacion r : sel.recomendaciones) {
            double[] p = posicion(r.unidad());
            linea(gc, p[0], p[1], r.plan().tramos(), Paleta.unidad(r.unidad().getTipo()).deriveColor(0, 1, 1, 0.55), 2, true);
        }
        for (Unidad u : sim.getUnidades()) {
            if (u.isManual() || u.getEstado() == EstadoUnidad.EN_BASE || u.getEstado() == EstadoUnidad.EN_ESCENA) continue;
            boolean destacada = u == sel.unidad || (sel.incidente != null && u.getIncidente() == sel.incidente);
            if (!destacada && u.getEstado() != EstadoUnidad.EN_CAMINO) continue;
            Color c = Paleta.unidad(u.getTipo()).deriveColor(0, 1, 1, destacada ? 0.9 : 0.28);
            double[] p = posicion(u);
            linea(gc, p[0], p[1], u.recorridoRestante(), c, destacada ? 2.6 : 1.6, !destacada);
        }
    }

    private void gps(GraphicsContext gc, Simulacion sim) {
        Unidad u = sim.getUnidadManual();
        Plan plan = sim.getGps();
        if (u == null || plan == null || plan.vacio()) return;
        gc.setStroke(Paleta.ACENTO.deriveColor(0, 1, 1, 0.25));
        gc.setLineWidth(9);
        trazar(gc, u.getX(), u.getY(), plan.tramos());
        linea(gc, u.getX(), u.getY(), plan.tramos(), Paleta.ACENTO, 3, false);

        // Flechas cada 46 px a lo largo de la ruta
        double acumulado = 0;
        for (Tramo tr : plan.tramos()) {
            Via v = tr.via();
            double angulo = Math.atan2(v.getDirY(), v.getDirX());
            for (double s = tr.desde(); s < tr.hasta(); s += 4) {
                acumulado += 4;
                if (acumulado % 46 < 4) {
                    Iconos.chevron(gc, v.xEn(s) - v.getDirY() * CARRIL, v.yEn(s) + v.getDirX() * CARRIL, angulo, 7, Paleta.TEXTO);
                }
            }
        }
    }

    private void linea(GraphicsContext gc, double x0, double y0, ListaEnlazada<Tramo> tramos, Color color, double ancho, boolean discontinua) {
        gc.setStroke(color);
        gc.setLineWidth(ancho);
        if (discontinua) gc.setLineDashes(5, 5);
        trazar(gc, x0, y0, tramos);
        gc.setLineDashes((double[]) null);
    }

    private void trazar(GraphicsContext gc, double x0, double y0, ListaEnlazada<Tramo> tramos) {
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.beginPath();
        gc.moveTo(x0, y0);
        for (Tramo tr : tramos) {
            Via v = tr.via();
            double nx = -v.getDirY() * CARRIL;
            double ny = v.getDirX() * CARRIL;
            gc.lineTo(v.xEn(tr.desde()) + nx, v.yEn(tr.desde()) + ny);
            gc.lineTo(v.xEn(tr.hasta()) + nx, v.yEn(tr.hasta()) + ny);
        }
        gc.stroke();
    }

    private void cupoEstacion(GraphicsContext gc, Estacion e) {
        int enBase = e.unidadesEnBase();
        int total = e.getUnidades().tamano();
        double x = e.getX();
        double y = e.getY() + PlanoCiudad.ALTO_ESTACION / 2 + 9;
        String texto = enBase + "/" + total + " en base";
        gc.setFill(Color.rgb(11, 14, 19, 0.85));
        gc.fillRoundRect(x - 26, y - 7, 52, 14, 7, 7);
        gc.setFont(Font.font("System", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(enBase == 0 ? Paleta.PELIGRO : Paleta.TEXTO_SUAVE);
        gc.fillText(texto, x, y);
    }

    private void incidente(GraphicsContext gc, Incidente inc, double t, boolean seleccionado, boolean bajoCursor) {
        double x = inc.getX();
        double y = inc.getY();
        Color c = Paleta.incidente(inc.getTipo());
        double pulso = (Math.sin(t * 4 + inc.getId()) + 1) / 2;
        boolean pendiente = inc.getEstado().pendiente();

        if (pendiente) {
            gc.setFill(c.deriveColor(0, 1, 1, 0.12 + 0.12 * pulso));
            double r = 19 + 5 * pulso;
            gc.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        if (seleccionado || bajoCursor) {
            gc.setStroke(seleccionado ? Paleta.TEXTO : Paleta.TEXTO_SUAVE);
            gc.setLineWidth(1.6);
            gc.setLineDashes(4, 3);
            gc.strokeOval(x - 22, y - 22, 44, 44);
            gc.setLineDashes((double[]) null);
        }

        gc.setFill(Color.web("#0d1219"));
        gc.fillOval(x - 12, y - 12, 24, 24);

        // Anillo: plazo restante mientras espera, progreso mientras se atiende
        gc.setLineWidth(3);
        gc.setLineCap(StrokeLineCap.BUTT);
        if (pendiente) {
            double urgencia = inc.urgencia(t);
            gc.setStroke(Color.web("#2a3240"));
            gc.strokeOval(x - 14, y - 14, 28, 28);
            gc.setStroke(Paleta.urgencia(urgencia));
            gc.strokeArc(x - 14, y - 14, 28, 28, 90, 360 * (1 - urgencia), ArcType.OPEN);
        } else if (inc.getEstado() == EstadoIncidente.EN_ATENCION) {
            gc.setStroke(Color.web("#2a3240"));
            gc.strokeOval(x - 14, y - 14, 28, 28);
            gc.setStroke(Paleta.ACENTO);
            gc.strokeArc(x - 14, y - 14, 28, 28, 90, -360 * inc.fraccionAtencion(), ArcType.OPEN);
        }
        gc.setStroke(c);
        gc.setLineWidth(1.6);
        gc.strokeOval(x - 11, y - 11, 22, 22);
        Iconos.incidente(gc, inc.getTipo(), x, y, 13, c);

        // Gravedad: tres puntos, encendidos según el nivel
        for (int i = 0; i < 3; i++) {
            gc.setFill(i < inc.getGravedad() ? c : Color.web("#2a3240"));
            gc.fillOval(x - 7.3 + i * 5.5, y + 17, 3.6, 3.6);
        }
        gc.setFill(Paleta.TEXTO);
        gc.setFont(Font.font("System", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("#" + inc.getId(), x + 14, y - 13);
    }

    private void unidad(GraphicsContext gc, Unidad u, double t, boolean destacada) {
        double[] p = posicion(u);
        double x = p[0];
        double y = p[1];
        double angulo = p[2];
        TipoUnidad tipo = u.getTipo();
        double largo = tipo == TipoUnidad.BOMBEROS ? 23 : 19;

        if (u.getEstado() == EstadoUnidad.EN_CAMINO || u.isManual()) {
            Color brillo = u.isManual() ? Paleta.ACENTO : Paleta.unidad(tipo);
            gc.setFill(brillo.deriveColor(0, 1, 1, 0.16));
            gc.fillOval(x - 15, y - 15, 30, 30);
        }
        if (destacada || u.isManual()) {
            gc.setStroke(u.isManual() ? Paleta.ACENTO : Paleta.TEXTO);
            gc.setLineWidth(1.5);
            gc.strokeOval(x - 15, y - 15, 30, 30);
        }

        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angulo));
        double w = 9.5;
        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillRoundRect(-largo / 2 + 1.5, -w / 2 + 1.5, largo, w, 3, 3);
        switch (tipo) {
            case PATRULLA -> {
                gc.setFill(Color.web("#1d3f8c"));
                gc.fillRoundRect(-largo / 2, -w / 2, largo, w, 3, 3);
                gc.setFill(Color.web("#eef2f8"));
                gc.fillRect(-3.5, -w / 2 + 1, 8, w - 2);
            }
            case AMBULANCIA -> {
                gc.setFill(Color.web("#eef2f8"));
                gc.fillRoundRect(-largo / 2, -w / 2, largo, w, 3, 3);
                gc.setFill(Color.web("#e5484d"));
                gc.fillRect(-largo / 2 + 2, -0.9, largo - 6, 1.8);
                Iconos.cruz(gc, -2.5, 0, 5.5, Color.web("#e5484d"));
            }
            case BOMBEROS -> {
                gc.setFill(Color.web("#d63a2a"));
                gc.fillRoundRect(-largo / 2, -w / 2, largo, w, 3, 3);
                gc.setStroke(Color.web("#c9ced8"));
                gc.setLineWidth(1);
                gc.strokeLine(-largo / 2 + 2, -1.8, largo / 2 - 7, -1.8);
                gc.strokeLine(-largo / 2 + 2, 1.8, largo / 2 - 7, 1.8);
                for (double s = -largo / 2 + 3; s < largo / 2 - 7; s += 3) gc.strokeLine(s, -1.8, s, 1.8);
            }
        }
        // Parabrisas
        gc.setFill(Color.web("#0d1420"));
        gc.fillRect(largo / 2 - 5, -w / 2 + 1.2, 2.2, w - 2.4);

        // Barra de luces
        if (u.getEstado() == EstadoUnidad.EN_CAMINO) {
            boolean fase = ((int) (t * 6)) % 2 == 0;
            Color a = tipo == TipoUnidad.PATRULLA ? Color.web("#3d7bff") : Color.web("#ffffff");
            Color b = Color.web("#ff2d3d");
            gc.setFill(fase ? a : b);
            gc.fillRect(largo / 2 - 8, -w / 2, 2, w / 2);
            gc.setFill(fase ? b : a);
            gc.fillRect(largo / 2 - 8, 0, 2, w / 2);
        }
        gc.restore();

        if (destacada || u.isManual()) {
            gc.setFill(Paleta.TEXTO);
            gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(u.isManual() ? u.getId() + " · MANUAL" : u.getId(), x, y - 20);
        }
    }

    private void efecto(GraphicsContext gc, Efecto e) {
        double subida = (1 - e.opacidad()) * 26;
        gc.setFont(Font.font("System", FontWeight.BOLD, 15));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        Color c = e.esPositivo() ? Paleta.EXITO : Paleta.PELIGRO;
        gc.setFill(Color.rgb(0, 0, 0, 0.6 * e.opacidad()));
        gc.fillText(e.getTexto(), e.getX() + 1, e.getY() - 28 - subida + 1);
        gc.setFill(c.deriveColor(0, 1, 1, e.opacidad()));
        gc.fillText(e.getTexto(), e.getX(), e.getY() - 28 - subida);
    }

    static void vehiculo(GraphicsContext gc, double x, double y, double angulo, double largo, double ancho, Color carroceria, Color techo) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angulo));
        gc.setFill(carroceria);
        gc.fillRoundRect(-largo / 2, -ancho / 2, largo, ancho, 2.5, 2.5);
        gc.setFill(techo.deriveColor(0, 1, 1, 0.6));
        gc.fillRect(-largo / 2 + 3, -ancho / 2 + 1, largo * 0.42, ancho - 2);
        gc.setFill(Color.web("#10161f"));
        gc.fillRect(largo / 2 - 4, -ancho / 2 + 0.8, 1.8, ancho - 1.6);
        gc.restore();
    }

    // ---- Posiciones de dibujo (compartidas con la detección de clics) ----

    /** Dónde se dibuja una unidad: bahía de su estación, alrededor de la escena o sobre su carril. */
    static double[] posicion(Unidad u) {
        if (u.getEstado() == EstadoUnidad.EN_BASE && !u.isManual()) {
            int indice = 0;
            for (Unidad otra : u.getEstacion().getUnidades()) {
                if (otra == u) break;
                indice++;
            }
            double[] b = PlanoCiudad.bahia(u.getEstacion(), indice);
            return new double[]{b[0], b[1], -Math.PI / 2};
        }
        if (u.isManual()) return new double[]{u.getX(), u.getY(), u.getAngulo()};

        PuntoVia p = u.puntoActual();
        if (p == null) return new double[]{u.getX(), u.getY(), u.getAngulo()};
        Via v = p.via();
        double angulo = Math.atan2(v.getDirY(), v.getDirX());
        double desvio = CARRIL;
        double avance = 0;
        if (u.getEstado() == EstadoUnidad.EN_ESCENA && u.getIncidente() != null) {
            int indice = 0;
            for (Unidad otra : u.getIncidente().getAsignadas()) {
                if (otra == u) break;
                indice++;
            }
            avance = indice % 2 == 0 ? -20 - 12 * (indice / 2) : 20 + 12 * (indice / 2);
        }
        double x = p.x() + v.getDirX() * avance - v.getDirY() * desvio;
        double y = p.y() + v.getDirY() * avance + v.getDirX() * desvio;
        return new double[]{x, y, angulo};
    }
}
