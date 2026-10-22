package sgmms.simulacion;

import org.junit.jupiter.api.Test;
import sgmms.grafo.Via;
import sgmms.modelo.Dificultad;
import sgmms.modelo.EstadoUnidad;
import sgmms.modelo.Incidente;
import sgmms.modelo.PuntoVia;
import sgmms.modelo.TipoIncidente;
import sgmms.modelo.TipoUnidad;
import sgmms.modelo.Unidad;

import static org.junit.jupiter.api.Assertions.*;

class MetricasTest {

    @Test
    void percentilPorRangoMasCercano() {
        Metricas m = new Metricas();
        for (int i = 10; i >= 1; i--) m.registrarRespuesta(TipoIncidente.ROBO, i);
        assertEquals(5.5, m.respuestaPromedio(), 1e-9);
        assertEquals(9, m.percentilRespuesta(90), 1e-9);
        assertEquals(5, m.percentilRespuesta(50), 1e-9);
        assertEquals(10, m.percentilRespuesta(100), 1e-9);
        assertEquals(1, m.percentilRespuesta(1), 1e-9);
    }

    @Test
    void cumplimientoYConteosPorTipo() {
        Metricas m = new Metricas();
        assertEquals(1, m.cumplimiento(), 1e-9, "sin incidentes cerrados no hay incumplimiento");
        m.registrarResuelto(TipoIncidente.INCENDIO);
        m.registrarResuelto(TipoIncidente.INCENDIO);
        m.registrarResuelto(TipoIncidente.ROBO);
        m.registrarExpirado(TipoIncidente.ACCIDENTE);
        assertEquals(0.75, m.cumplimiento(), 1e-9);
        assertEquals(2, m.resueltos(TipoIncidente.INCENDIO));
        assertEquals(1, m.expirados());
    }

    /** Con el tráfico fluctuando, una unidad no debe cambiar de ruta a cada rato. */
    @Test
    void laHisteresisEvitaReplanificacionesConstantes() {
        Simulacion sim = new Simulacion(Dificultad.DIFICIL, 8);
        sim.setGeneracionAutomatica(false);
        for (int i = 0; i < 60 * 30; i++) sim.avanzar(Simulacion.PASO); // deja que el tráfico se forme
        Via v = sim.getCiudad().getGrafo().via("D3", "D4");
        Incidente inc = sim.reportar(TipoIncidente.ROBO, 1, new PuntoVia(v, v.getLongitud() / 2));
        Unidad p = null;
        for (Unidad u : sim.getUnidades()) if (u.getTipo() == TipoUnidad.PATRULLA) p = u;
        assertNull(sim.asignar(p, inc));
        Unidad patrulla = p;
        for (int i = 0; i < 60 * 60 && patrulla.getEstado() != EstadoUnidad.EN_ESCENA; i++) sim.avanzar(Simulacion.PASO);
        assertEquals(EstadoUnidad.EN_ESCENA, patrulla.getEstado());
        assertTrue(sim.getMetricas().getReplanificaciones() <= 3,
                "se replanificó " + sim.getMetricas().getReplanificaciones() + " veces en un solo viaje");
    }
}
