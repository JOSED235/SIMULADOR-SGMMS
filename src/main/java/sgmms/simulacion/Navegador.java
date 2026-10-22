package sgmms.simulacion;

import sgmms.estructuras.ListaEnlazada;
import sgmms.estructuras.TablaHash;
import sgmms.grafo.BuscadorRutas;
import sgmms.grafo.BuscadorRutas.Semilla;
import sgmms.grafo.FuncionCosto;
import sgmms.grafo.Grafo;
import sgmms.grafo.Nodo;
import sgmms.grafo.Via;
import sgmms.modelo.Plan;
import sgmms.modelo.PuntoVia;
import sgmms.modelo.Tramo;

/**
 * Convierte "voy de aquí a allá" en un {@link Plan} de tramos. Los extremos pueden
 * ser intersecciones o puntos a mitad de cuadra; para estos últimos se le dan al
 * buscador las dos esquinas como semillas, con el costo del pedazo de calle que falta.
 */
public final class Navegador {

    /** Segundos que cuesta dar la vuelta en U a mitad de cuadra. */
    public static final double PENALIZACION_GIRO_U = 2.0;

    /** Intersección o punto sobre un carril. */
    public record Extremo(Nodo nodo, PuntoVia punto) {
        public static Extremo en(Nodo nodo) {
            return new Extremo(nodo, null);
        }

        public static Extremo en(PuntoVia punto) {
            return new Extremo(null, punto);
        }
    }

    private Navegador() {
    }

    /** @return el plan más barato, o null si no hay forma de llegar */
    public static Plan planificar(Grafo grafo, Extremo desde, Extremo hacia, FuncionCosto costo, double cotaSegundosPorPixel) {
        ListaEnlazada<Semilla> origenes = new ListaEnlazada<>();
        TablaHash<String, Tramo> prefijos = new TablaHash<>();
        if (desde.nodo() != null) {
            origenes.agregar(new Semilla(desde.nodo(), 0));
        } else {
            PuntoVia p = desde.punto();
            Via v = p.via();
            Via opuesta = v.getOpuesta();
            origenes.agregar(new Semilla(v.getDestino(), (v.getLongitud() - p.s()) * costo.segundosPorPixel(v)));
            prefijos.poner(v.getDestino().getId(), new Tramo(v, p.s(), v.getLongitud()));
            origenes.agregar(new Semilla(v.getOrigen(), p.s() * costo.segundosPorPixel(opuesta) + PENALIZACION_GIRO_U));
            prefijos.poner(v.getOrigen().getId(), new Tramo(opuesta, v.getLongitud() - p.s(), v.getLongitud()));
        }

        ListaEnlazada<Semilla> destinos = new ListaEnlazada<>();
        TablaHash<String, Tramo> sufijos = new TablaHash<>();
        if (hacia.nodo() != null) {
            destinos.agregar(new Semilla(hacia.nodo(), 0));
        } else {
            PuntoVia p = hacia.punto();
            Via v = p.via();
            Via opuesta = v.getOpuesta();
            destinos.agregar(new Semilla(v.getOrigen(), p.s() * costo.segundosPorPixel(v)));
            sufijos.poner(v.getOrigen().getId(), new Tramo(v, 0, p.s()));
            destinos.agregar(new Semilla(v.getDestino(), (v.getLongitud() - p.s()) * costo.segundosPorPixel(opuesta)));
            sufijos.poner(v.getDestino().getId(), new Tramo(opuesta, 0, v.getLongitud() - p.s()));
        }

        Plan porGrafo = null;
        BuscadorRutas.Resultado r = BuscadorRutas.buscar(grafo, origenes, destinos, costo, cotaSegundosPorPixel);
        if (r.encontrada()) {
            ListaEnlazada<Tramo> tramos = new ListaEnlazada<>();
            Tramo prefijo = prefijos.obtener(r.origen().getId());
            if (prefijo != null && prefijo.longitud() > 0) tramos.agregar(prefijo);
            for (Via via : r.vias()) tramos.agregar(Tramo.completa(via));
            Tramo sufijo = sufijos.obtener(r.destino().getId());
            if (sufijo != null && sufijo.longitud() > 0) tramos.agregar(sufijo);
            porGrafo = new Plan(tramos, r.costo(), r.nodosExpandidos());
        }

        Plan directo = directoMismaCalle(desde, hacia, costo);
        if (directo != null && (porGrafo == null || directo.segundosEstimados() <= porGrafo.segundosEstimados())) {
            return directo;
        }
        return porGrafo;
    }

    /** Origen y destino en la misma cuadra: se puede ir derecho sin pasar por ninguna esquina. */
    private static Plan directoMismaCalle(Extremo desde, Extremo hacia, FuncionCosto costo) {
        if (desde.punto() == null || hacia.punto() == null) return null;
        Via v = desde.punto().via();
        Via destino = hacia.punto().via();
        if (destino != v && destino != v.getOpuesta()) return null;

        double s = desde.punto().s();
        double sDestino = destino == v ? hacia.punto().s() : v.getLongitud() - hacia.punto().s();
        ListaEnlazada<Tramo> tramos = new ListaEnlazada<>();
        double segundos;
        if (sDestino >= s) {
            tramos.agregar(new Tramo(v, s, sDestino));
            segundos = (sDestino - s) * costo.segundosPorPixel(v);
        } else {
            Via opuesta = v.getOpuesta();
            tramos.agregar(new Tramo(opuesta, v.getLongitud() - s, v.getLongitud() - sDestino));
            segundos = (s - sDestino) * costo.segundosPorPixel(opuesta) + PENALIZACION_GIRO_U;
        }
        return new Plan(tramos, segundos, 0);
    }
}
