package sgmms.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import sgmms.estructuras.ListaEnlazada;
import sgmms.modelo.Dificultad;
import sgmms.modelo.EstadoIncidente;
import sgmms.modelo.Incidente;
import sgmms.modelo.TipoUnidad;
import sgmms.modelo.Unidad;
import sgmms.simulacion.Simulacion;

import java.io.File;

/**
 * Genera las capturas del README recorriendo la aplicación de forma automática.
 * Se activa con {@code -Dsgmms.capturas=<carpeta>}; no forma parte del juego normal.
 */
final class Capturas {

    private static final long SEMILLA = 2024;

    private final App app;
    private final File carpeta;
    private final ListaEnlazada<Runnable> pasos = new ListaEnlazada<>();
    private VistaJuego juego;

    Capturas(App app, File carpeta) {
        this.app = app;
        this.carpeta = carpeta;
    }

    void ejecutar() {
        pasos.agregar(app::mostrarMenu);
        pasos.agregar(() -> capturar("menu.png"));
        pasos.agregar(this::prepararDespacho);
        pasos.agregar(() -> capturar("despacho.png"));
        pasos.agregar(this::prepararConduccion);
        pasos.agregar(() -> capturar("conduccion.png"));
        pasos.agregar(this::terminarTurno);
        pasos.agregar(() -> capturar("informe.png"));
        pasos.agregar(Platform::exit);
        siguiente();
    }

    private void siguiente() {
        if (pasos.estaVacia()) return;
        Runnable paso = pasos.eliminarPrimero();
        PauseTransition espera = new PauseTransition(Duration.seconds(2.2));
        espera.setOnFinished(e -> {
            paso.run();
            siguiente();
        });
        espera.play();
    }

    private void capturar(String nombre) {
        File archivo = app.guardarCaptura(carpeta, nombre);
        System.out.println("Captura: " + archivo);
    }

    /** Juega dos minutos con despacho automático y deja un incidente nuevo esperando decisión. */
    private void prepararDespacho() {
        juego = app.iniciarTurno(Dificultad.NORMAL, SEMILLA);
        Simulacion sim = juego.getSimulacion();
        sim.setDespachoAutomatico(true);
        avanzar(sim, 125);
        sim.setDespachoAutomatico(false);
        Incidente objetivo = null;
        for (int i = 0; i < 60 * 60 && objetivo == null; i++) {
            sim.avanzar(Simulacion.PASO);
            for (Incidente inc : sim.colaTriage()) {
                if (inc.getEstado() == EstadoIncidente.REPORTADO && !sim.recomendar(inc).estaVacia()) objetivo = inc;
            }
        }
        avanzar(sim, 3);
        if (objetivo != null && objetivo.getEstado().pendiente()) juego.seleccionarIncidente(objetivo);
        juego.congelar(true);
    }

    /** Toma el volante de una patrulla despachada al incidente seleccionado (o a otro que la necesite). */
    private void prepararConduccion() {
        Simulacion sim = juego.getSimulacion();
        juego.congelar(false);
        juego.despacharRecomendadas();
        avanzar(sim, 6);
        Unidad elegida = null;
        for (Unidad u : sim.getUnidades()) {
            if (u.getTipo() == TipoUnidad.PATRULLA && u.getEstado().disponible()) elegida = u;
        }
        if (elegida != null) {
            for (Incidente inc : sim.colaTriage()) {
                if (inc.necesita(TipoUnidad.PATRULLA) && sim.asignar(elegida, inc) == null) break;
            }
            juego.seleccionarUnidad(elegida);
            sim.tomarControl(elegida);
        }
        avanzar(sim, 0.5);
        juego.congelar(true);
    }

    private void terminarTurno() {
        Simulacion sim = juego.getSimulacion();
        sim.soltarControl();
        sim.setDespachoAutomatico(true);
        while (!sim.terminada()) sim.avanzar(Simulacion.PASO);
        app.mostrarInforme(sim);
    }

    private static void avanzar(Simulacion sim, double segundos) {
        for (double t = 0; t < segundos && !sim.terminada(); t += Simulacion.PASO) sim.avanzar(Simulacion.PASO);
    }
}
