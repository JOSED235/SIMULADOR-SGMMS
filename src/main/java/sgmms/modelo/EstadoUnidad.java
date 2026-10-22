package sgmms.modelo;

public enum EstadoUnidad {
    EN_BASE("En base"),
    EN_CAMINO("En camino"),
    EN_ESCENA("En escena"),
    REGRESANDO("Regresando");

    private final String nombre;

    EstadoUnidad(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /** Puede recibir un despacho nuevo sin quitársela a otro incidente. */
    public boolean disponible() {
        return this == EN_BASE || this == REGRESANDO;
    }
}
