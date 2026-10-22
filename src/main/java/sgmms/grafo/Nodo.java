package sgmms.grafo;

/** Intersección (o salida de la ciudad) con coordenadas en píxeles del mundo. */
public class Nodo {

    private final String id;
    private final double x;
    private final double y;
    private final boolean salida;

    public Nodo(String id, double x, double y, boolean salida) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.salida = salida;
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /** Los nodos de salida están en el borde del mapa: allí entran y salen los autos civiles. */
    public boolean esSalida() {
        return salida;
    }

    public double distancia(Nodo otro) {
        return Math.hypot(x - otro.x, y - otro.y);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Nodo n && n.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
