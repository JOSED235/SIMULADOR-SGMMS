package sgmms.grafo;

import sgmms.estructuras.ListaEnlazada;
import sgmms.estructuras.MonticuloMin;
import sgmms.estructuras.TablaHash;

/**
 * A* (y Dijkstra como caso particular) con varios orígenes y varios destinos.
 *
 * <p>Los orígenes y destinos múltiples aparecen porque los vehículos casi nunca
 * están parados en una intersección: uno que va a mitad de cuadra puede seguir
 * hasta la esquina de adelante o dar la vuelta; un incidente a mitad de cuadra se
 * alcanza desde cualquiera de sus dos esquinas. Cada {@link Semilla} lleva el
 * costo ya pagado (origen) o el que falta pagar (destino).</p>
 *
 * <p>La heurística es la distancia en línea recta por la cota inferior de
 * segundos por píxel, más el menor costo extra de los destinos: nunca sobreestima,
 * así que A* devuelve el mismo costo óptimo que Dijkstra expandiendo menos nodos.</p>
 */
public final class BuscadorRutas {

    public record Semilla(Nodo nodo, double costo) {
    }

    public record Resultado(boolean encontrada, ListaEnlazada<Via> vias, Nodo origen, Nodo destino,
                            double costo, int nodosExpandidos) {

        static Resultado sinRuta(int expandidos) {
            return new Resultado(false, new ListaEnlazada<>(), null, null, Double.POSITIVE_INFINITY, expandidos);
        }
    }

    private record Entrada(Nodo nodo, double g, double f) {
    }

    private BuscadorRutas() {
    }

    /** Búsqueda nodo a nodo con A*. */
    public static Resultado aEstrella(Grafo grafo, Nodo origen, Nodo destino, FuncionCosto costo, double cotaSegundosPorPixel) {
        return buscar(grafo, una(origen), una(destino), costo, cotaSegundosPorPixel);
    }

    /** Búsqueda nodo a nodo con Dijkstra (A* con heurística cero). */
    public static Resultado dijkstra(Grafo grafo, Nodo origen, Nodo destino, FuncionCosto costo) {
        return buscar(grafo, una(origen), una(destino), costo, 0);
    }

    /**
     * @param cotaSegundosPorPixel cota inferior del costo por píxel de cualquier vía; con 0 se comporta como Dijkstra
     */
    public static Resultado buscar(Grafo grafo, ListaEnlazada<Semilla> origenes, ListaEnlazada<Semilla> destinos,
                                   FuncionCosto costo, double cotaSegundosPorPixel) {
        TablaHash<String, Double> mejorG = new TablaHash<>(64);
        TablaHash<String, Via> predecesor = new TablaHash<>(64);
        TablaHash<String, Boolean> cerrados = new TablaHash<>(64);
        TablaHash<String, Double> costoFinal = new TablaHash<>();
        for (Semilla d : destinos) {
            Double previo = costoFinal.obtener(d.nodo().getId());
            if (previo == null || d.costo() < previo) costoFinal.poner(d.nodo().getId(), d.costo());
        }

        MonticuloMin<Entrada> frontera = new MonticuloMin<>((a, b) -> Double.compare(a.f(), b.f()));
        for (Semilla o : origenes) {
            Double previo = mejorG.obtener(o.nodo().getId());
            if (previo != null && previo <= o.costo()) continue;
            mejorG.poner(o.nodo().getId(), o.costo());
            frontera.insertar(new Entrada(o.nodo(), o.costo(), o.costo() + heuristica(o.nodo(), destinos, cotaSegundosPorPixel)));
        }

        double mejorTotal = Double.POSITIVE_INFINITY;
        Nodo mejorDestino = null;
        int expandidos = 0;

        while (!frontera.estaVacio()) {
            Entrada actual = frontera.extraerMinimo();
            if (actual.f() >= mejorTotal) break; // ningún camino pendiente puede mejorar el encontrado
            String id = actual.nodo().getId();
            if (cerrados.contieneClave(id)) continue;
            cerrados.poner(id, Boolean.TRUE);
            expandidos++;

            Double extra = costoFinal.obtener(id);
            if (extra != null && actual.g() + extra < mejorTotal) {
                mejorTotal = actual.g() + extra;
                mejorDestino = actual.nodo();
            }

            for (Via via : grafo.salientes(actual.nodo())) {
                if (!costo.transitable(via)) continue;
                Nodo vecino = via.getDestino();
                if (cerrados.contieneClave(vecino.getId())) continue;
                double g = actual.g() + costo.costo(via);
                Double conocido = mejorG.obtener(vecino.getId());
                if (conocido == null || g < conocido) {
                    mejorG.poner(vecino.getId(), g);
                    predecesor.poner(vecino.getId(), via);
                    frontera.insertar(new Entrada(vecino, g, g + heuristica(vecino, destinos, cotaSegundosPorPixel)));
                }
            }
        }

        if (mejorDestino == null) return Resultado.sinRuta(expandidos);

        // Solo tienen predecesor los nodos alcanzados por una vía; la cadena termina en la semilla usada
        ListaEnlazada<Via> camino = new ListaEnlazada<>();
        Nodo paso = mejorDestino;
        for (Via via = predecesor.obtener(paso.getId()); via != null; via = predecesor.obtener(paso.getId())) {
            camino.agregarAlInicio(via);
            paso = via.getOrigen();
        }
        return new Resultado(true, camino, paso, mejorDestino, mejorTotal, expandidos);
    }

    private static double heuristica(Nodo nodo, ListaEnlazada<Semilla> destinos, double cota) {
        if (cota <= 0) return 0;
        double mejor = Double.POSITIVE_INFINITY;
        for (Semilla d : destinos) {
            mejor = Math.min(mejor, nodo.distancia(d.nodo()) * cota + d.costo());
        }
        return mejor;
    }

    private static ListaEnlazada<Semilla> una(Nodo nodo) {
        ListaEnlazada<Semilla> lista = new ListaEnlazada<>();
        lista.agregar(new Semilla(nodo, 0));
        return lista;
    }
}
