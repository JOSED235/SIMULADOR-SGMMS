package sgmms.simulacion;

import org.junit.jupiter.api.Test;
import sgmms.grafo.Via;
import sgmms.modelo.Dificultad;
import sgmms.modelo.EstadoIncidente;
import sgmms.modelo.EstadoUnidad;
import sgmms.modelo.Incidente;
import sgmms.modelo.Plan;
import sgmms.modelo.PuntoVia;
import sgmms.modelo.TipoIncidente;
import sgmms.modelo.TipoUnidad;
import sgmms.modelo.Tramo;
import sgmms.modelo.Unidad;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class SimulacionTest {

    /** Simulación sin incidentes aleatorios: cada prueba arma su propio escenario. */
    private static Simulacion controlada() {
        Simulacion sim = new Simulacion(Dificultad.NORMAL, 1);
        sim.setGeneracionAutomatica(false);
        return sim;
    }

    private static PuntoVia mitad(Simulacion sim, String a, String b) {
        Via v = sim.getCiudad().getGrafo().via(a, b);
        return new PuntoVia(v, v.getLongitud() / 2);
    }

    private static Unidad unidad(Simulacion sim, String id) {
        for (Unidad u : sim.getUnidades()) if (u.getId().equals(id)) return u;
        throw new IllegalArgumentException(id);
    }

    /** Avanza hasta que se cumpla la condición; falla si no ocurre en el tiempo dado. */
    private static void avanzarHasta(Simulacion sim, BooleanSupplier condicion, double maxSegundos) {
        for (double t = 0; t < maxSegundos; t += Simulacion.PASO) {
            if (condicion.getAsBoolean()) return;
            sim.avanzar(Simulacion.PASO);
        }
        assertTrue(condicion.getAsBoolean(), "la condición no se cumplió en " + maxSegundos + " s");
    }

    @Test
    void unRoboSeResuelveConUnaPatrullaYSumaPuntos() {
        Simulacion sim = controlada();
        Incidente robo = sim.reportar(TipoIncidente.ROBO, 2, mitad(sim, "B1", "B2"));
        assertEquals(1, sim.asignarRecomendadas(robo));
        assertEquals(EstadoIncidente.EN_CAMINO, robo.getEstado());

        avanzarHasta(sim, () -> robo.getEstado() == EstadoIncidente.RESUELTO, 60);
        assertTrue(robo.getPuntos() >= 200, "base de 100 por nivel de gravedad más el bono");
        assertEquals(robo.getPuntos(), sim.getPuntaje());
        assertEquals(1, sim.getMetricas().resueltos());
        assertTrue(robo.tiempoRespuesta() > 0);
    }

    @Test
    void lasUnidadesRegresanALaBaseDespuesDeResolver() {
        Simulacion sim = controlada();
        Incidente robo = sim.reportar(TipoIncidente.ROBO, 1, mitad(sim, "C1", "C2"));
        sim.asignarRecomendadas(robo);
        Unidad u = robo.getAsignadas().primero();
        avanzarHasta(sim, () -> robo.getEstado() == EstadoIncidente.RESUELTO, 60);
        assertEquals(EstadoUnidad.REGRESANDO, u.getEstado());
        avanzarHasta(sim, () -> u.getEstado() == EstadoUnidad.EN_BASE, 60);
        assertNull(u.getIncidente());
    }

    @Test
    void unAccidenteCierraLaCalleYExigePatrullaYAmbulancia() {
        Simulacion sim = controlada();
        PuntoVia punto = mitad(sim, "C2", "C3");
        Incidente choque = sim.reportar(TipoIncidente.ACCIDENTE, 1, punto);
        assertTrue(punto.via().estaCerrada());
        assertTrue(punto.via().getOpuesta().estaCerrada());

        // Solo la patrulla: al llegar no basta para empezar la atención
        Unidad patrulla = unidad(sim, "P-1");
        assertNull(sim.asignar(patrulla, choque));
        avanzarHasta(sim, () -> patrulla.getEstado() == EstadoUnidad.EN_ESCENA, 60);
        sim.avanzar(1);
        assertEquals(EstadoIncidente.EN_CAMINO, choque.getEstado());
        assertEquals(1, choque.faltantes().tamano());
        assertEquals(TipoUnidad.AMBULANCIA, choque.faltantes().primero());

        Unidad ambulancia = unidad(sim, "A-1");
        assertNull(sim.asignar(ambulancia, choque));
        avanzarHasta(sim, () -> choque.getEstado() == EstadoIncidente.EN_ATENCION, 60);
        avanzarHasta(sim, () -> choque.getEstado() == EstadoIncidente.RESUELTO, 30);
        assertFalse(punto.via().estaCerrada(), "al resolverse se reabre la calle");
    }

    @Test
    void sinAtencionElIncidenteExpiraYCuentaComoFallo() {
        Simulacion sim = controlada();
        Incidente robo = sim.reportar(TipoIncidente.ROBO, 3, mitad(sim, "B1", "B2"));
        avanzarHasta(sim, () -> robo.getEstado() == EstadoIncidente.EXPIRADO, 60);
        assertEquals(1, sim.getFallos());
        assertEquals(-75, sim.getPuntaje());
        assertTrue(sim.colaTriage().estaVacia());
        assertTrue(sim.getActivos().estaVacia());
    }

    @Test
    void unIncendioDesatendidoSePropagaYPideOtroCamion() {
        Simulacion sim = controlada();
        Incidente fuego = sim.reportar(TipoIncidente.INCENDIO, 2, mitad(sim, "C1", "C2"));
        assertEquals(1, fuego.requeridas().tamano());
        avanzarHasta(sim, fuego::isEscalado, 40);
        assertEquals(3, fuego.getGravedad());
        assertEquals(2, fuego.requeridas().contar(t -> t == TipoUnidad.BOMBEROS));
    }

    @Test
    void unaUnidadEnCaminoSePuedeReasignar() {
        Simulacion sim = controlada();
        Incidente primero = sim.reportar(TipoIncidente.ROBO, 1, mitad(sim, "D1", "D2"));
        Incidente segundo = sim.reportar(TipoIncidente.ROBO, 3, mitad(sim, "A2", "A3"));
        Unidad p = unidad(sim, "P-1");
        assertNull(sim.asignar(p, primero));
        sim.avanzar(1);
        assertNull(sim.asignar(p, segundo));
        assertEquals(EstadoIncidente.REPORTADO, primero.getEstado());
        assertTrue(segundo.getAsignadas().contiene(p));
        assertEquals(1, sim.getMetricas().getReasignaciones());
    }

    @Test
    void rechazaDespachosQueNoTienenSentido() {
        Simulacion sim = controlada();
        Incidente robo = sim.reportar(TipoIncidente.ROBO, 1, mitad(sim, "B1", "B2"));
        assertNotNull(sim.asignar(unidad(sim, "B-1"), robo), "un robo no necesita bomberos");
        assertNull(sim.asignar(unidad(sim, "P-1"), robo));
        assertNotNull(sim.asignar(unidad(sim, "P-2"), robo), "ya tiene su patrulla");
        assertNotNull(sim.asignar(unidad(sim, "P-1"), robo), "no se puede asignar dos veces la misma");
    }

    @Test
    void laColaDeTriageOrdenaPorPlazo() {
        Simulacion sim = controlada();
        Incidente leve = sim.reportar(TipoIncidente.ROBO, 1, mitad(sim, "B1", "B2"));
        Incidente grave = sim.reportar(TipoIncidente.EMERGENCIA_MEDICA, 3, mitad(sim, "C1", "C2"));
        Incidente medio = sim.reportar(TipoIncidente.INCENDIO, 2, mitad(sim, "D2", "D3"));
        assertEquals("[" + grave + ", " + medio + ", " + leve + "]", sim.colaTriage().toString());
        assertSame(grave, sim.incidenteMasUrgente());
    }

    @Test
    void unCierreEnLaRutaObligaADesviarse() {
        Simulacion sim = controlada();
        Incidente robo = sim.reportar(TipoIncidente.ROBO, 1, mitad(sim, "D3", "D4"));
        Unidad p = unidad(sim, "P-1");
        assertNull(sim.asignar(p, robo));
        sim.avanzar(Simulacion.PASO);

        // Se cierra una calle intermedia de la ruta actual (no la del incidente)
        Via enRuta = null;
        boolean primero = true;
        for (Tramo t : p.recorridoRestante()) {
            Via v = t.via();
            boolean esDelIncidente = v == robo.getUbicacion().via() || v == robo.getUbicacion().via().getOpuesta();
            if (!primero && t.longitud() > 50 && !esDelIncidente) {
                enRuta = v;
                break;
            }
            primero = false;
        }
        assertNotNull(enRuta, "la ruta debe tener al menos una calle intermedia");
        sim.reportar(TipoIncidente.ACCIDENTE, 1, new PuntoVia(enRuta, enRuta.getLongitud() / 2));
        sim.avanzar(Simulacion.PASO);

        for (Tramo t : p.recorridoRestante()) {
            assertFalse(t.via() == enRuta || t.via() == enRuta.getOpuesta(), "la unidad sigue pasando por la calle cerrada");
        }
        avanzarHasta(sim, () -> p.getEstado() == EstadoUnidad.EN_ESCENA, 90);
    }

    @Test
    void conduccionManualSiguiendoElGpsLlegaALaEscena() {
        Simulacion sim = controlada();
        Incidente robo = sim.reportar(TipoIncidente.ROBO, 1, mitad(sim, "B1", "B2"));
        Unidad p = unidad(sim, "P-1");
        assertNull(sim.tomarControl(p));
        assertNull(sim.asignar(p, robo));
        assertNotNull(sim.getGps());

        // "Piloto": apunta al final del primer tramo del GPS que todavía no alcanzó
        for (double t = 0; t < 60 && p.getEstado() != EstadoUnidad.EN_ESCENA; t += Simulacion.PASO) {
            Plan gps = sim.getGps();
            double objetivoX = robo.getX();
            double objetivoY = robo.getY();
            if (gps != null) {
                for (Tramo tr : gps.tramos()) {
                    double x = tr.via().xEn(tr.hasta());
                    double y = tr.via().yEn(tr.hasta());
                    if (Math.hypot(x - p.getX(), y - p.getY()) > 4) {
                        objetivoX = x;
                        objetivoY = y;
                        break;
                    }
                }
            }
            double dx = objetivoX - p.getX();
            double dy = objetivoY - p.getY();
            sim.setDireccionManual(Math.abs(dx) > 2 ? Math.signum(dx) : 0, Math.abs(dy) > 2 ? Math.signum(dy) : 0);
            sim.avanzar(Simulacion.PASO);
        }
        assertEquals(EstadoUnidad.EN_ESCENA, p.getEstado());
        assertFalse(p.isManual(), "al llegar se suelta el volante");
        assertNull(sim.getUnidadManual());
        avanzarHasta(sim, () -> robo.getEstado() == EstadoIncidente.RESUELTO, 20);
    }

    @Test
    void alSoltarElVolanteElPilotoAutomaticoTerminaElRecorrido() {
        Simulacion sim = controlada();
        Incidente robo = sim.reportar(TipoIncidente.ROBO, 1, mitad(sim, "C2", "C3"));
        Unidad p = unidad(sim, "P-2");
        assertNull(sim.asignar(p, robo));
        sim.avanzar(2);
        assertNull(sim.tomarControl(p));
        sim.setDireccionManual(0, 0);
        sim.avanzar(Simulacion.PASO);
        sim.soltarControl();
        assertTrue(p.tienePlan());
        avanzarHasta(sim, () -> p.getEstado() == EstadoUnidad.EN_ESCENA, 90);
    }

    @Test
    void lamismaSemillaProduceElMismoTurno() {
        Simulacion a = Benchmark.correrTurno(Estrategia.DINAMICA, Dificultad.FACIL, 99);
        Simulacion b = Benchmark.correrTurno(Estrategia.DINAMICA, Dificultad.FACIL, 99);
        assertEquals(a.getPuntaje(), b.getPuntaje());
        assertEquals(a.getMetricas().generados(), b.getMetricas().generados());
        assertEquals(a.getMetricas().respuestaPromedio(), b.getMetricas().respuestaPromedio(), 1e-12);
        assertEquals(a.getBitacora().toString(), b.getBitacora().toString());
    }

    @Test
    void elTurnoTerminaAlCumplirseLaDuracion() {
        Simulacion sim = new Simulacion(Dificultad.FACIL, 5);
        sim.setDespachoAutomatico(true);
        avanzarHasta(sim, sim::terminada, Dificultad.FACIL.getDuracionTurno() + 1);
        assertEquals(Simulacion.Resultado.TURNO_COMPLETO, sim.getResultado());
        assertTrue(sim.getMetricas().resueltos() > 0);
        assertFalse(sim.getMetricas().getMuestras().estaVacia());
    }

    @Test
    void sinDespachosElTurnoFallaAlAgotarLosFallos() {
        Simulacion sim = new Simulacion(Dificultad.NORMAL, 5);
        avanzarHasta(sim, sim::terminada, Dificultad.NORMAL.getDuracionTurno());
        assertEquals(Simulacion.Resultado.TURNO_FALLIDO, sim.getResultado());
        assertEquals(Dificultad.NORMAL.getFallosPermitidos(), sim.getFallos());
    }

    @Test
    void todasLasDificultadesCorrenUnTurnoCompletoSinErrores() {
        for (Dificultad d : Dificultad.values()) {
            for (Estrategia e : Estrategia.values()) {
                Simulacion sim = Benchmark.correrTurno(e, d, 3);
                assertTrue(sim.terminada());
                assertTrue(sim.getMetricas().generados() > 10, d + "/" + e);
            }
        }
    }
}
