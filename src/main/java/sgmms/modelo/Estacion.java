package sgmms.modelo;

import sgmms.estructuras.ListaEnlazada;
import sgmms.grafo.Nodo;

/** Base de un tipo de unidad: un edificio junto a una intersección. */
public class Estacion {

    private final TipoUnidad tipo;
    private final Nodo nodo;
    private final double x;
    private final double y;
    private final ListaEnlazada<Unidad> unidades = new ListaEnlazada<>();

    /**
     * @param x centro del edificio en el mapa
     * @param y centro del edificio en el mapa
     */
    public Estacion(TipoUnidad tipo, Nodo nodo, double x, double y) {
        this.tipo = tipo;
        this.nodo = nodo;
        this.x = x;
        this.y = y;
    }

    public TipoUnidad getTipo() {
        return tipo;
    }

    public String getNombre() {
        return tipo.getNombreEstacion();
    }

    /** Intersección por la que salen y entran las unidades. */
    public Nodo getNodo() {
        return nodo;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public ListaEnlazada<Unidad> getUnidades() {
        return unidades;
    }

    public int unidadesEnBase() {
        return unidades.contar(u -> u.getEstado() == EstadoUnidad.EN_BASE);
    }
}
