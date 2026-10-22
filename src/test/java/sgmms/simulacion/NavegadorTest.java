package sgmms.simulacion;

import org.junit.jupiter.api.Test;
import sgmms.grafo.FuncionCosto;
import sgmms.grafo.Grafo;
import sgmms.grafo.Via;
import sgmms.modelo.Plan;
import sgmms.modelo.PuntoVia;
import sgmms.modelo.Tramo;
import sgmms.simulacion.Navegador.Extremo;

import static org.junit.jupiter.api.Assertions.*;

class NavegadorTest {

    private static final double V = 50;
    private static final FuncionCosto COSTO = FuncionCosto.soloDistancia(V);

    private final Ciudad ciudad = new Ciudad();
    private final Grafo grafo = ciudad.getGrafo();

    private Plan planificar(Extremo desde, Extremo hacia) {
        return Navegador.planificar(grafo, desde, hacia, COSTO, 1.0 / V);
    }

    @Test
    void puntoAdelanteEnLaMismaCuadraEsUnSoloTramo() {
        Via v = grafo.via("A1", "A2");
        Plan p = planificar(Extremo.en(new PuntoVia(v, 20)), Extremo.en(new PuntoVia(v, 100)));
        assertEquals(1, p.tramos().tamano());
        Tramo t = p.tramos().primero();
        assertSame(v, t.via());
        assertEquals(20, t.desde(), 1e-9);
        assertEquals(100, t.hasta(), 1e-9);
        assertEquals(80 / V, p.segundosEstimados(), 1e-9);
    }

    @Test
    void puntoAtrasEnLaMismaCuadraDaLaVueltaEnU() {
        Via v = grafo.via("A1", "A2");
        Plan p = planificar(Extremo.en(new PuntoVia(v, 100)), Extremo.en(new PuntoVia(v, 40)));
        assertEquals(1, p.tramos().tamano());
        Tramo t = p.tramos().primero();
        assertSame(v.getOpuesta(), t.via());
        assertEquals(60, t.longitud(), 1e-9);
        assertEquals(60 / V + Navegador.PENALIZACION_GIRO_U, p.segundosEstimados(), 1e-9);
    }

    @Test
    void elPlanEsContinuoYTerminaEnElDestino() {
        Plan p = planificar(Extremo.en(grafo.nodo("D1")), Extremo.en(grafo.nodo("A4")));
        assertNotNull(p);
        Tramo anterior = null;
        for (Tramo t : p.tramos()) {
            if (anterior != null) assertEquals(anterior.via().getDestino(), t.via().getOrigen());
            anterior = t;
        }
        assertEquals("A4", p.tramos().ultimo().via().getDestino().getId());
        assertEquals(p.longitud() / V, p.segundosEstimados(), 1e-6);
    }

    @Test
    void llegaAUnPuntoDentroDeUnaCalleCerrada() {
        Via accidente = grafo.via("B1", "B2");
        grafo.cerrarCalle(accidente);
        PuntoVia punto = new PuntoVia(accidente, accidente.getLongitud() / 2);
        Plan p = planificar(Extremo.en(grafo.nodo("A3")), Extremo.en(punto));
        assertNotNull(p, "la unidad debe poder entrar hasta la escena aunque la calle esté cerrada al tráfico");
        Tramo ultimo = p.tramos().ultimo();
        assertTrue(ultimo.via() == accidente || ultimo.via() == accidente.getOpuesta());
        assertEquals(accidente.getLongitud() / 2, ultimo.hasta(), 1e-9);
        // Las únicas partes en la vía cerrada son el tramo final
        int enCerradas = 0;
        for (Tramo t : p.tramos()) if (t.via().estaCerrada()) enCerradas++;
        assertEquals(1, enCerradas);
    }

    @Test
    void desdeMitadDeCuadraPuedeSeguirODevolverse() {
        Via v = grafo.via("A2", "A3");
        PuntoVia casiAlFinal = new PuntoVia(v, v.getLongitud() - 5);
        Plan adelante = planificar(Extremo.en(casiAlFinal), Extremo.en(grafo.nodo("A3")));
        assertEquals(5, adelante.longitud(), 1e-9);

        // Tan cerca de la esquina conviene llegar a ella y tomar el carril contrario, sin pagar la vuelta en U
        Plan desdeLaEsquina = planificar(Extremo.en(casiAlFinal), Extremo.en(grafo.nodo("A2")));
        assertSame(v, desdeLaEsquina.tramos().primero().via());
        assertSame(v.getOpuesta(), desdeLaEsquina.tramos().ultimo().via());

        // A mitad de cuadra la vuelta en U es más barata que ir hasta la esquina y regresar
        Plan enU = planificar(Extremo.en(new PuntoVia(v, v.getLongitud() / 2)), Extremo.en(grafo.nodo("A2")));
        assertEquals(1, enU.tramos().tamano());
        assertSame(v.getOpuesta(), enU.tramos().primero().via());
    }
}
