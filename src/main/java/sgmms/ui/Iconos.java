package sgmms.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import sgmms.modelo.TipoIncidente;
import sgmms.modelo.TipoUnidad;

/** Íconos vectoriales simples, centrados en (x, y) y de tamaño aproximado {@code t}. */
final class Iconos {

    private Iconos() {
    }

    static void incidente(GraphicsContext gc, TipoIncidente tipo, double x, double y, double t, Color color) {
        switch (tipo) {
            case ROBO -> antifaz(gc, x, y, t, color);
            case EMERGENCIA_MEDICA -> cruz(gc, x, y, t, color);
            case INCENDIO -> llama(gc, x, y, t, color);
            case ACCIDENTE -> choque(gc, x, y, t, color);
        }
    }

    static void estacion(GraphicsContext gc, TipoUnidad tipo, double x, double y, double t, Color color) {
        switch (tipo) {
            case PATRULLA -> escudo(gc, x, y, t, color);
            case AMBULANCIA -> cruz(gc, x, y, t, color);
            case BOMBEROS -> llama(gc, x, y, t, color);
        }
    }

    static void cruz(GraphicsContext gc, double x, double y, double t, Color color) {
        double brazo = t * 0.32;
        double largo = t;
        gc.setFill(color);
        gc.fillRect(x - brazo / 2, y - largo / 2, brazo, largo);
        gc.fillRect(x - largo / 2, y - brazo / 2, largo, brazo);
    }

    static void llama(GraphicsContext gc, double x, double y, double t, Color color) {
        double h = t * 0.62;
        double w = t * 0.42;
        gc.setFill(color);
        gc.beginPath();
        gc.moveTo(x, y - h);
        gc.bezierCurveTo(x + w * 0.4, y - h * 0.4, x + w * 1.1, y - h * 0.1, x + w * 0.9, y + h * 0.35);
        gc.bezierCurveTo(x + w * 0.7, y + h * 0.85, x - w * 0.7, y + h * 0.85, x - w * 0.9, y + h * 0.35);
        gc.bezierCurveTo(x - w * 1.0, y, x - w * 0.5, y - h * 0.2, x, y - h);
        gc.closePath();
        gc.fill();
    }

    static void antifaz(GraphicsContext gc, double x, double y, double t, Color color) {
        double w = t * 0.95;
        double h = t * 0.42;
        gc.setFill(color);
        gc.fillRoundRect(x - w / 2, y - h / 2, w, h, h, h);
        gc.setFill(Color.rgb(10, 14, 20));
        double ojo = h * 0.5;
        gc.fillOval(x - w * 0.3 - ojo / 2, y - ojo / 2, ojo * 1.3, ojo);
        gc.fillOval(x + w * 0.3 - ojo * 0.8, y - ojo / 2, ojo * 1.3, ojo);
    }

    static void choque(GraphicsContext gc, double x, double y, double t, Color color) {
        double r = t * 0.48;
        gc.setFill(color);
        gc.beginPath();
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16 - Math.PI / 2;
            double radio = i % 2 == 0 ? r : r * 0.48;
            double px = x + Math.cos(a) * radio;
            double py = y + Math.sin(a) * radio;
            if (i == 0) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.closePath();
        gc.fill();
    }

    static void escudo(GraphicsContext gc, double x, double y, double t, Color color) {
        double w = t * 0.8;
        double h = t * 0.95;
        gc.setFill(color);
        gc.beginPath();
        gc.moveTo(x - w / 2, y - h / 2);
        gc.lineTo(x + w / 2, y - h / 2);
        gc.lineTo(x + w / 2, y);
        gc.quadraticCurveTo(x + w / 2, y + h * 0.35, x, y + h / 2);
        gc.quadraticCurveTo(x - w / 2, y + h * 0.35, x - w / 2, y);
        gc.closePath();
        gc.fill();
    }

    /** Barrera de cierre de vía, orientada según el ángulo de la calle. */
    static void barrera(GraphicsContext gc, double x, double y, double angulo, double largo) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angulo) + 90);
        gc.setFill(Color.web("#ff4d5e"));
        gc.fillRect(-largo / 2, -2.2, largo, 4.4);
        gc.setFill(Color.web("#f5f5f5"));
        for (double s = -largo / 2 + 2; s < largo / 2 - 2; s += 7) gc.fillRect(s, -2.2, 3, 4.4);
        gc.restore();
    }

    /** Flecha de dirección para la línea del GPS. */
    static void chevron(GraphicsContext gc, double x, double y, double angulo, double t, Color color) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angulo));
        gc.setStroke(color);
        gc.setLineWidth(t * 0.35);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(-t * 0.4, -t * 0.5, t * 0.3, 0);
        gc.strokeLine(-t * 0.4, t * 0.5, t * 0.3, 0);
        gc.restore();
    }
}
