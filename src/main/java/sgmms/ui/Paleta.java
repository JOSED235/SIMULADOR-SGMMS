package sgmms.ui;

import javafx.scene.paint.Color;
import sgmms.modelo.TipoIncidente;
import sgmms.modelo.TipoUnidad;

/** Colores de la consola de despacho. */
final class Paleta {

    static final Color FONDO = Color.web("#0b0e13");
    static final Color ANDEN = Color.web("#1c222c");
    static final Color MANZANA = Color.web("#141921");
    static final Color ASFALTO = Color.web("#232934");
    static final Color LINEA_CARRIL = Color.web("#46505f");
    static final Color CEBRA = Color.web("#323a47");
    static final Color PARQUE = Color.web("#13261c");
    static final Color PARQUE_BORDE = Color.web("#1c3a29");
    static final Color SENDERO = Color.web("#253a2e");
    static final Color ARBOL = Color.web("#1f4d31");
    static final Color ARBOL_LUZ = Color.web("#2b6a43");
    static final Color AGUA = Color.web("#12324a");
    static final Color AGUA_BORDE = Color.web("#1f4d6e");
    static final Color PLAZA = Color.web("#1d222b");

    static final Color TEXTO = Color.web("#e6ebf2");
    static final Color TEXTO_SUAVE = Color.web("#8a94a6");
    static final Color ACENTO = Color.web("#38d4ff");
    static final Color EXITO = Color.web("#3ddc84");
    static final Color ALERTA = Color.web("#ffb020");
    static final Color PELIGRO = Color.web("#ff4d5e");

    static final Color[] AUTOS = {
            Color.web("#7d8597"), Color.web("#a3a9b5"), Color.web("#5b6475"), Color.web("#8f7b66"),
            Color.web("#6f8a8f"), Color.web("#9a8c74"), Color.web("#5e6f86"), Color.web("#857a8f"),
    };

    private static final Color[] EDIFICIOS = {
            Color.web("#1f2632"), Color.web("#232b38"), Color.web("#27303e"), Color.web("#1c2330"), Color.web("#2a3443"),
    };

    private Paleta() {
    }

    static Color edificio(int i) {
        return EDIFICIOS[Math.floorMod(i, EDIFICIOS.length)];
    }

    static Color unidad(TipoUnidad tipo) {
        return switch (tipo) {
            case PATRULLA -> Color.web("#4c8dff");
            case AMBULANCIA -> Color.web("#f2f5fa");
            case BOMBEROS -> Color.web("#ff5a3c");
        };
    }

    static Color incidente(TipoIncidente tipo) {
        return switch (tipo) {
            case ROBO -> Color.web("#4c8dff");
            case EMERGENCIA_MEDICA -> Color.web("#3ddc84");
            case INCENDIO -> Color.web("#ff6a3d");
            case ACCIDENTE -> Color.web("#ffb020");
        };
    }

    /** Verde, ámbar o rojo según qué tan cerca está el plazo. */
    static Color urgencia(double fraccionConsumida) {
        if (fraccionConsumida < 0.5) return EXITO;
        if (fraccionConsumida < 0.8) return ALERTA;
        return PELIGRO;
    }

    static String hex(Color c) {
        return String.format("#%02x%02x%02x", (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }
}
