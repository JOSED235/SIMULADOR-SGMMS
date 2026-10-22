package sgmms.ui;

import sgmms.modelo.Dificultad;

import java.util.prefs.Preferences;

/** Mejor puntaje por dificultad, guardado en las preferencias del usuario. */
final class Records {

    private static final Preferences PREFERENCIAS = Preferences.userNodeForPackage(Records.class);

    private Records() {
    }

    static int mejor(Dificultad dificultad) {
        return PREFERENCIAS.getInt(clave(dificultad), 0);
    }

    /** Guarda el puntaje si supera el récord. Devuelve true si es un récord nuevo. */
    static boolean registrar(Dificultad dificultad, int puntaje) {
        // Las capturas automáticas juegan con despacho automático: no deben contar como récord
        if (System.getProperty("sgmms.capturas") != null) return false;
        if (puntaje <= mejor(dificultad)) return false;
        PREFERENCIAS.putInt(clave(dificultad), puntaje);
        return true;
    }

    private static String clave(Dificultad dificultad) {
        return "record." + dificultad.name();
    }
}
