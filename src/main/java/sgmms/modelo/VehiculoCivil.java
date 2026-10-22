package sgmms.modelo;

import sgmms.estructuras.ListaEnlazada;
import sgmms.grafo.Nodo;
import sgmms.grafo.Via;

/** Auto particular que entra por un borde de la ciudad y busca su destino por la ruta más rápida. */
public class VehiculoCivil {

    private final int id;
    private final int color;
    private final double velocidadLibre;
    private final Nodo destino;
    private final ListaEnlazada<Via> ruta = new ListaEnlazada<>();

    private Via via;
    private double s;
    private double velocidad;
    private double espera;
    private double orillado;
    private Nodo rutaCalculadaEn;

    public VehiculoCivil(int id, int color, double velocidadLibre, Via via, Nodo destino) {
        this.id = id;
        this.color = color;
        this.velocidadLibre = velocidadLibre;
        this.via = via;
        this.destino = destino;
    }

    public void nuevaRuta(Nodo desde, ListaEnlazada<Via> vias) {
        ruta.limpiar();
        ruta.agregarTodos(vias);
        rutaCalculadaEn = desde;
    }

    /** Esquina desde la que se calculó la ruta actual. */
    public Nodo getRutaCalculadaEn() {
        return rutaCalculadaEn;
    }

    /** Siguiente vía de la ruta, sin consumirla. */
    public Via siguienteVia() {
        return ruta.estaVacia() ? null : ruta.primero();
    }

    public void entrarA(Via siguiente) {
        if (!ruta.estaVacia() && ruta.primero() == siguiente) ruta.eliminarPrimero();
        via = siguiente;
        s = 0;
        espera = 0;
    }

    public double getX() {
        return via.xEn(s);
    }

    public double getY() {
        return via.yEn(s);
    }

    public int getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    public double getVelocidadLibre() {
        return velocidadLibre;
    }

    public Nodo getDestino() {
        return destino;
    }

    public ListaEnlazada<Via> getRuta() {
        return ruta;
    }

    public Via getVia() {
        return via;
    }

    public double getS() {
        return s;
    }

    public void setS(double s) {
        this.s = s;
    }

    public double getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(double velocidad) {
        this.velocidad = velocidad;
    }

    public double getEspera() {
        return espera;
    }

    public void sumarEspera(double dt) {
        espera += dt;
    }

    public void reiniciarEspera() {
        espera = 0;
    }

    /** Segundos que le quedan orillado para dejar pasar una sirena. */
    public double getOrillado() {
        return orillado;
    }

    public void orillar() {
        orillado = 1.2;
    }

    public void avanzarOrillado(double dt) {
        orillado = Math.max(0, orillado - dt);
    }
}
