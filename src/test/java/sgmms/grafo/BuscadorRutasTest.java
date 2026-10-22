package sgmms.grafo;

import org.junit.jupiter.api.Test;
import sgmms.estructuras.ListaEnlazada;
import sgmms.grafo.BuscadorRutas.Semilla;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BuscadorRutasTest {

    private static final double VELOCIDAD = 50;
    private static final FuncionCosto COSTO = FuncionCosto.conTrafico(VELOCIDAD);
    private static final double COTA = 1.0 / VELOCIDAD;

    /** Cuadrícula n x n con 100 px entre intersecciones y tráfico aleatorio en cada vía. */
    private static Grafo cuadricula(int n, long semilla) {
        Grafo g = new Grafo();
        for (int f = 0; f < n; f++) {
            for (int c = 0; c < n; c++) g.agregarNodo(id(f, c), c * 100, f * 100, false);
        }
        for (int f = 0; f < n; f++) {
            for (int c = 0; c < n; c++) {
                if (c + 1 < n) g.agregarCalle(id(f, c), id(f, c + 1));
                if (f + 1 < n) g.agregarCalle(id(f, c), id(f + 1, c));
            }
        }
        Random rng = new Random(semilla);
        for (Via v : g.vias()) {
            int autos = rng.nextInt(6);
            for (int i = 0; i < autos; i++) v.entraAuto();
        }
        return g;
    }

    private static void vaciar(Grafo g) {
        for (Via v : g.vias()) {
            while (v.getOcupacion() > 0) v.saleAuto();
        }
    }

    private static String id(int f, int c) {
        return f + "," + c;
    }

    private static double costoDe(ListaEnlazada<Via> vias) {
        double total = 0;
        for (Via v : vias) total += COSTO.costo(v);
        return total;
    }

    @Test
    void aEstrellaEncuentraElMismoCostoOptimoQueDijkstraExpandiendoMenos() {
        Grafo g = cuadricula(8, 1);
        Random rng = new Random(5);
        int expandidosA = 0;
        int expandidosD = 0;
        for (int i = 0; i < 200; i++) {
            Nodo origen = g.nodo(id(rng.nextInt(8), rng.nextInt(8)));
            Nodo destino = g.nodo(id(rng.nextInt(8), rng.nextInt(8)));
            BuscadorRutas.Resultado a = BuscadorRutas.aEstrella(g, origen, destino, COSTO, COTA);
            BuscadorRutas.Resultado d = BuscadorRutas.dijkstra(g, origen, destino, COSTO);
            assertTrue(a.encontrada());
            assertEquals(d.costo(), a.costo(), 1e-9);
            assertEquals(a.costo(), costoDe(a.vias()), 1e-9, "el costo reportado debe ser el del camino devuelto");
            expandidosA += a.nodosExpandidos();
            expandidosD += d.nodosExpandidos();
        }
        assertTrue(expandidosA < expandidosD, "A* expandió " + expandidosA + " vs Dijkstra " + expandidosD);
    }

    @Test
    void elCaminoEsContinuoYVaDelOrigenAlDestino() {
        Grafo g = cuadricula(5, 2);
        BuscadorRutas.Resultado r = BuscadorRutas.aEstrella(g, g.nodo("0,0"), g.nodo("4,4"), COSTO, COTA);
        Nodo actual = g.nodo("0,0");
        for (Via v : r.vias()) {
            assertEquals(actual, v.getOrigen());
            actual = v.getDestino();
        }
        assertEquals(g.nodo("4,4"), actual);
    }

    @Test
    void evitaLasCallesCerradasYLasUsaAlReabrirse() {
        Grafo g = cuadricula(3, 3);
        // Se cierran las dos calles que salen de la esquina 0,0 menos una
        g.cerrarCalle(g.via("0,0", "0,1"));
        BuscadorRutas.Resultado r = BuscadorRutas.aEstrella(g, g.nodo("0,0"), g.nodo("0,2"), COSTO, COTA);
        assertTrue(r.encontrada());
        assertEquals("1,0", r.vias().primero().getDestino().getId());
        for (Via v : r.vias()) assertFalse(v.estaCerrada());

        int version = g.getVersionCierres();
        g.abrirCalle(g.via("0,0", "0,1"));
        assertNotEquals(version, g.getVersionCierres());
        assertFalse(g.via("0,1", "0,0").estaCerrada(), "el cierre afecta ambos sentidos");
    }

    @Test
    void sinCaminoPosibleNoEncuentraRuta() {
        Grafo g = cuadricula(2, 4);
        g.cerrarCalle(g.via("0,0", "0,1"));
        g.cerrarCalle(g.via("0,0", "1,0"));
        BuscadorRutas.Resultado r = BuscadorRutas.aEstrella(g, g.nodo("0,0"), g.nodo("1,1"), COSTO, COTA);
        assertFalse(r.encontrada());
        assertTrue(r.vias().estaVacia());
    }

    @Test
    void conVariosOrigenesYDestinosEligeLaCombinacionMasBarata() {
        Grafo g = cuadricula(4, 0);
        vaciar(g);
        ListaEnlazada<Semilla> origenes = new ListaEnlazada<>();
        origenes.agregar(new Semilla(g.nodo("0,0"), 0));
        origenes.agregar(new Semilla(g.nodo("3,3"), 50)); // muy cerca del destino pero con costo previo alto
        ListaEnlazada<Semilla> destinos = new ListaEnlazada<>();
        destinos.agregar(new Semilla(g.nodo("3,2"), 0));
        destinos.agregar(new Semilla(g.nodo("0,1"), 1));

        BuscadorRutas.Resultado r = BuscadorRutas.buscar(g, origenes, destinos, COSTO, COTA);
        assertEquals("0,0", r.origen().getId());
        assertEquals("0,1", r.destino().getId());
        assertEquals(COSTO.costo(g.via("0,0", "0,1")) + 1, r.costo(), 1e-9);
    }

    @Test
    void elTraficoEncareceLaViaYDesviaLaRuta() {
        Grafo g = cuadricula(2, 0);
        vaciar(g);
        Via directa = g.via("0,0", "0,1");
        BuscadorRutas.Resultado libre = BuscadorRutas.aEstrella(g, g.nodo("0,0"), g.nodo("0,1"), COSTO, COTA);
        assertEquals(1, libre.vias().tamano());

        for (int i = 0; i < 6; i++) directa.entraAuto(); // trancón: la vía queda llena
        assertTrue(directa.factorCongestion() < 0.5);
        BuscadorRutas.Resultado desvio = BuscadorRutas.aEstrella(g, g.nodo("0,0"), g.nodo("0,1"), COSTO, COTA);
        assertEquals(3, desvio.vias().tamano(), "con la vía llena conviene dar la vuelta a la manzana");
    }
}
