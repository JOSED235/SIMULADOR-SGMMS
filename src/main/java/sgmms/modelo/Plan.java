package sgmms.modelo;

import sgmms.estructuras.ListaEnlazada;

/** Recorrido calculado por el navegador, con su costo estimado y lo que costó encontrarlo. */
public record Plan(ListaEnlazada<Tramo> tramos, double segundosEstimados, int nodosExpandidos) {

    public double longitud() {
        double total = 0;
        for (Tramo t : tramos) total += t.longitud();
        return total;
    }

    public boolean vacio() {
        return tramos.estaVacia();
    }
}
