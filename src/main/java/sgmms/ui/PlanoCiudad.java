package sgmms.ui;

import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import sgmms.grafo.Nodo;
import sgmms.grafo.Via;
import sgmms.modelo.Estacion;
import sgmms.simulacion.Ciudad;
import sgmms.simulacion.Ciudad.Manzana;

import java.util.Random;

/**
 * Dibuja una sola vez la parte fija de la ciudad (calles, manzanas, parques y
 * estaciones) en una imagen al doble de resolución; cada fotograma solo la copia.
 */
final class PlanoCiudad {

    private static final double RESOLUCION = 2;
    static final double ANCHO_ESTACION = 84;
    static final double ALTO_ESTACION = 56;
    private static final double ANDEN = 6;

    private PlanoCiudad() {
    }

    static Image generar(Ciudad ciudad) {
        Canvas lienzo = new Canvas(Ciudad.ANCHO * RESOLUCION, Ciudad.ALTO * RESOLUCION);
        GraphicsContext gc = lienzo.getGraphicsContext2D();
        gc.scale(RESOLUCION, RESOLUCION);

        gc.setFill(Paleta.ASFALTO);
        gc.fillRect(0, 0, Ciudad.ANCHO, Ciudad.ALTO);

        Random rng = new Random(2025);
        for (Manzana m : ciudad.getManzanas()) dibujarManzana(gc, m, ciudad, rng);
        dibujarMarcasViales(gc, ciudad);
        for (Estacion e : ciudad.getEstaciones()) dibujarEstacion(gc, e);

        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setFill(Color.TRANSPARENT);
        return lienzo.snapshot(parametros, null);
    }

    private static void dibujarManzana(GraphicsContext gc, Manzana m, Ciudad ciudad, Random rng) {
        gc.setFill(Paleta.ANDEN);
        gc.fillRoundRect(m.x1(), m.y1(), m.ancho(), m.alto(), 10, 10);
        double x = m.x1() + ANDEN;
        double y = m.y1() + ANDEN;
        double w = m.ancho() - 2 * ANDEN;
        double h = m.alto() - 2 * ANDEN;

        switch (m.tipo()) {
            case PARQUE -> dibujarParque(gc, x, y, w, h, rng);
            case PLAZA -> dibujarPlaza(gc, x, y, w, h);
            case EDIFICIOS -> {
                gc.setFill(Paleta.MANZANA);
                gc.fillRoundRect(x, y, w, h, 6, 6);
                lotes(gc, x, y, w, h, ciudad, rng, 0);
            }
        }
    }

    /** Subdivide la manzana en lotes de forma recursiva y pone un edificio en cada uno. */
    private static void lotes(GraphicsContext gc, double x, double y, double w, double h, Ciudad ciudad, Random rng, int nivel) {
        boolean partir = (w > 78 || h > 78) && nivel < 7;
        if (partir) {
            double r = 0.35 + rng.nextDouble() * 0.3;
            if (w >= h) {
                lotes(gc, x, y, w * r, h, ciudad, rng, nivel + 1);
                lotes(gc, x + w * r, y, w * (1 - r), h, ciudad, rng, nivel + 1);
            } else {
                lotes(gc, x, y, w, h * r, ciudad, rng, nivel + 1);
                lotes(gc, x, y + h * r, w, h * (1 - r), ciudad, rng, nivel + 1);
            }
            return;
        }
        if (tocaEstacion(x, y, w, h, ciudad)) return;

        double bx = x + 3;
        double by = y + 3;
        double bw = w - 6;
        double bh = h - 6;
        if (bw < 8 || bh < 8) return;

        if (rng.nextDouble() < 0.1) {
            // Lote vacío con árboles
            gc.setFill(Paleta.PARQUE);
            gc.fillRoundRect(bx, by, bw, bh, 4, 4);
            arboles(gc, bx, by, bw, bh, rng, (int) (bw * bh / 500) + 1);
            return;
        }

        Color techo = Paleta.edificio(rng.nextInt(5));
        gc.setFill(Color.rgb(0, 0, 0, 0.35));
        gc.fillRect(bx + 2.5, by + 2.5, bw, bh);
        gc.setFill(techo);
        gc.fillRect(bx, by, bw, bh);
        gc.setStroke(techo.brighter().deriveColor(0, 1, 0.85, 1));
        gc.setLineWidth(1);
        gc.strokeRect(bx + 0.5, by + 0.5, bw - 1, bh - 1);

        // Detalles de azotea
        int detalles = rng.nextInt(3);
        gc.setFill(techo.deriveColor(0, 1, 1.35, 1));
        for (int i = 0; i < detalles; i++) {
            double dw = 4 + rng.nextDouble() * Math.min(10, bw / 3);
            double dh = 4 + rng.nextDouble() * Math.min(8, bh / 3);
            gc.fillRect(bx + 3 + rng.nextDouble() * (bw - dw - 6), by + 3 + rng.nextDouble() * (bh - dh - 6), dw, dh);
        }
        if (bw > 30 && bh > 30 && rng.nextBoolean()) {
            gc.setStroke(techo.deriveColor(0, 1, 0.75, 1));
            gc.strokeRect(bx + 5, by + 5, bw - 10, bh - 10);
        }
    }

    private static boolean tocaEstacion(double x, double y, double w, double h, Ciudad ciudad) {
        for (Estacion e : ciudad.getEstaciones()) {
            double ex = e.getX() - ANCHO_ESTACION / 2 - 8;
            double ey = e.getY() - ALTO_ESTACION / 2 - 8;
            if (x < ex + ANCHO_ESTACION + 16 && x + w > ex && y < ey + ALTO_ESTACION + 16 && y + h > ey) return true;
        }
        return false;
    }

    private static void dibujarParque(GraphicsContext gc, double x, double y, double w, double h, Random rng) {
        gc.setFill(Paleta.PARQUE);
        gc.fillRoundRect(x, y, w, h, 8, 8);
        gc.setStroke(Paleta.PARQUE_BORDE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 4, y + 4, w - 8, h - 8, 8, 8);

        // Senderos en cruz y glorieta central
        double cx = x + w * 0.55;
        double cy = y + h * 0.5;
        gc.setStroke(Paleta.SENDERO);
        gc.setLineWidth(7);
        gc.strokeLine(x + 6, cy, x + w - 6, cy);
        gc.strokeLine(cx, y + 6, cx, y + h - 6);
        gc.setLineWidth(5);
        gc.strokeLine(x + 10, y + 10, cx, cy);
        gc.strokeLine(x + w - 10, y + h - 10, cx, cy);
        gc.setFill(Paleta.AGUA);
        gc.fillOval(cx - 34, cy - 34, 68, 68);
        gc.setStroke(Paleta.AGUA_BORDE);
        gc.setLineWidth(3);
        gc.strokeOval(cx - 34, cy - 34, 68, 68);
        gc.setFill(Paleta.AGUA_BORDE);
        gc.fillOval(cx - 8, cy - 8, 16, 16);

        arboles(gc, x + 8, y + 8, w - 16, h - 16, rng, 70);
    }

    private static void arboles(GraphicsContext gc, double x, double y, double w, double h, Random rng, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            double r = 4 + rng.nextDouble() * 5;
            double ax = x + r + rng.nextDouble() * Math.max(1, w - 2 * r);
            double ay = y + r + rng.nextDouble() * Math.max(1, h - 2 * r);
            gc.setFill(Color.rgb(0, 0, 0, 0.3));
            gc.fillOval(ax - r + 1.5, ay - r + 1.5, 2 * r, 2 * r);
            gc.setFill(Paleta.ARBOL);
            gc.fillOval(ax - r, ay - r, 2 * r, 2 * r);
            gc.setFill(Paleta.ARBOL_LUZ);
            gc.fillOval(ax - r * 0.6, ay - r * 0.7, r, r);
        }
    }

    /** Plaza con dos estanques, como la manzana de piscinas del mapa original. */
    private static void dibujarPlaza(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Paleta.PLAZA);
        gc.fillRoundRect(x, y, w, h, 6, 6);
        gc.setStroke(Paleta.ANDEN);
        gc.setLineWidth(1);
        for (double gx = x + 12; gx < x + w; gx += 12) gc.strokeLine(gx, y, gx, y + h);
        for (double gy = y + 12; gy < y + h; gy += 12) gc.strokeLine(x, gy, x + w, gy);

        double margen = 14;
        double ancho = (w - 3 * margen) / 2;
        for (int i = 0; i < 2; i++) {
            double px = x + margen + i * (ancho + margen);
            double py = y + margen;
            double ph = h - 2 * margen;
            gc.setFill(Paleta.AGUA);
            gc.fillRoundRect(px, py, ancho, ph, 10, 10);
            gc.setStroke(Paleta.AGUA_BORDE);
            gc.setLineWidth(3);
            gc.strokeRoundRect(px, py, ancho, ph, 10, 10);
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(px + 18, py + 18, ancho - 36, ph - 36, 6, 6);
            gc.strokeRect(px + ancho / 2 - 10, py + ph / 2 - 10, 20, 20);
        }
    }

    private static void dibujarMarcasViales(GraphicsContext gc, Ciudad ciudad) {
        gc.setStroke(Paleta.LINEA_CARRIL);
        gc.setLineWidth(1.4);
        gc.setLineDashes(9, 9);
        for (Via c : ciudad.getCalles()) {
            double recorteInicio = c.getOrigen().esSalida() ? 0 : Ciudad.MEDIO_ANCHO_CALLE + 14;
            double recorteFin = c.getDestino().esSalida() ? 0 : Ciudad.MEDIO_ANCHO_CALLE + 14;
            double largo = c.getLongitud() - recorteInicio - recorteFin;
            if (largo <= 0) continue;
            gc.strokeLine(c.xEn(recorteInicio), c.yEn(recorteInicio), c.xEn(recorteInicio + largo), c.yEn(recorteInicio + largo));
        }
        gc.setLineDashes((double[]) null);

        // Pasos de cebra en cada acceso a una intersección
        gc.setFill(Paleta.CEBRA);
        for (Nodo n : ciudad.getGrafo().nodos()) {
            if (n.esSalida()) continue;
            for (Via v : ciudad.getGrafo().salientes(n)) {
                gc.save();
                gc.translate(v.xEn(Ciudad.MEDIO_ANCHO_CALLE + 6), v.yEn(Ciudad.MEDIO_ANCHO_CALLE + 6));
                gc.rotate(Math.toDegrees(Math.atan2(v.getDirY(), v.getDirX())));
                for (double d = -Ciudad.MEDIO_ANCHO_CALLE + 3; d < Ciudad.MEDIO_ANCHO_CALLE - 2; d += 5) {
                    gc.fillRect(-3, d, 6, 2.6);
                }
                gc.restore();
            }
        }
    }

    private static void dibujarEstacion(GraphicsContext gc, Estacion e) {
        Color color = Paleta.unidad(e.getTipo());
        double x = e.getX() - ANCHO_ESTACION / 2;
        double y = e.getY() - ALTO_ESTACION / 2;

        gc.setFill(Color.rgb(0, 0, 0, 0.4));
        gc.fillRoundRect(x + 3, y + 3, ANCHO_ESTACION, ALTO_ESTACION, 8, 8);
        gc.setFill(Color.web("#1a2130"));
        gc.fillRoundRect(x, y, ANCHO_ESTACION, ALTO_ESTACION, 8, 8);
        gc.setStroke(color.deriveColor(0, 1, 1, 0.85));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 1, y + 1, ANCHO_ESTACION - 2, ALTO_ESTACION - 2, 8, 8);
        gc.setFill(color.deriveColor(0, 1, 1, 0.18));
        gc.fillRoundRect(x + 4, y + 4, ANCHO_ESTACION - 8, 16, 4, 4);

        Iconos.estacion(gc, e.getTipo(), x + 14, y + 12, 11, color);
        gc.setFill(Paleta.TEXTO);
        gc.setFont(Font.font("System", FontWeight.BOLD, 9.5));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(e.getNombre().toUpperCase(), x + 24, y + 12.5);

        // Bahías de estacionamiento
        gc.setStroke(Color.web("#2e3747"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double bx = x + 10 + i * 16;
            gc.strokeLine(bx, y + 26, bx, y + ALTO_ESTACION - 6);
        }
    }

    /** Posición de estacionamiento de la i-ésima unidad dentro de su estación. */
    static double[] bahia(Estacion e, int indice) {
        double x = e.getX() - ANCHO_ESTACION / 2 + 18 + indice * 16;
        double y = e.getY() + 11;
        return new double[]{x, y};
    }
}
