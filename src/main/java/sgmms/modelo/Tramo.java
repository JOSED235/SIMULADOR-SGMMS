package sgmms.modelo;

import sgmms.grafo.Via;

/** Parte de un recorrido: avanzar por {@code via} desde {@code desde} hasta {@code hasta}. */
public record Tramo(Via via, double desde, double hasta) {

    public double longitud() {
        return hasta - desde;
    }

    public static Tramo completa(Via via) {
        return new Tramo(via, 0, via.getLongitud());
    }
}
