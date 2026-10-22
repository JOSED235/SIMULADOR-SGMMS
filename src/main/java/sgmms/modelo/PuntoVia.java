package sgmms.modelo;

import sgmms.grafo.Via;

/** Un punto sobre un carril: la vía y la distancia recorrida desde su origen. */
public record PuntoVia(Via via, double s) {

    public double x() {
        return via.xEn(s);
    }

    public double y() {
        return via.yEn(s);
    }

    /** El mismo punto físico visto desde el carril contrario. */
    public PuntoVia enOpuesta() {
        return new PuntoVia(via.getOpuesta(), via.getLongitud() - s);
    }
}
