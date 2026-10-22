package sgmms.grafo;

/**
 * Arista dirigida del grafo: un carril de una calle de doble sentido.
 * Su peso no es fijo: depende de cuántos autos civiles la ocupan en este momento
 * y de si un incidente la tiene cerrada.
 */
public class Via {

    /** Espacio que ocupa un auto en la vía (largo + distancia de seguridad). */
    public static final double ESPACIO_POR_AUTO = 20;

    private final Nodo origen;
    private final Nodo destino;
    private final double longitud;
    private final double dirX;
    private final double dirY;
    private Via opuesta;

    private int ocupacion;
    private int cierres;

    public Via(Nodo origen, Nodo destino) {
        this.origen = origen;
        this.destino = destino;
        this.longitud = origen.distancia(destino);
        this.dirX = (destino.getX() - origen.getX()) / longitud;
        this.dirY = (destino.getY() - origen.getY()) / longitud;
    }

    public String getId() {
        return origen.getId() + "->" + destino.getId();
    }

    public Nodo getOrigen() {
        return origen;
    }

    public Nodo getDestino() {
        return destino;
    }

    public double getLongitud() {
        return longitud;
    }

    public double getDirX() {
        return dirX;
    }

    public double getDirY() {
        return dirY;
    }

    /** Carril contrario de la misma calle. */
    public Via getOpuesta() {
        return opuesta;
    }

    void setOpuesta(Via opuesta) {
        this.opuesta = opuesta;
    }

    public double xEn(double s) {
        return origen.getX() + dirX * s;
    }

    public double yEn(double s) {
        return origen.getY() + dirY * s;
    }

    // ---- Tráfico ----

    public int getOcupacion() {
        return ocupacion;
    }

    public void entraAuto() {
        ocupacion++;
    }

    public void saleAuto() {
        if (ocupacion > 0) ocupacion--;
    }

    public double capacidad() {
        return Math.max(1, longitud / ESPACIO_POR_AUTO);
    }

    /** Ocupación relativa: 0 = vacía, 1 = llena de defensa a defensa. */
    public double densidad() {
        return Math.min(1.5, ocupacion / capacidad());
    }

    /** Fracción de la velocidad libre a la que avanza un auto civil. */
    public double factorCongestion() {
        return Math.max(0.15, 1 - 0.85 * densidad());
    }

    /** Las unidades con sirena se abren paso, pero el tráfico denso igual las frena. */
    public double factorSirena() {
        return Math.max(0.3, 1 - 0.8 * densidad());
    }

    // ---- Cierres ----

    public boolean estaCerrada() {
        return cierres > 0;
    }

    void cerrar() {
        cierres++;
    }

    void abrir() {
        if (cierres > 0) cierres--;
    }

    @Override
    public String toString() {
        return getId();
    }
}
