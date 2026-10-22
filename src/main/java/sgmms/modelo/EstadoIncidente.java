package sgmms.modelo;

public enum EstadoIncidente {
    REPORTADO("Sin asignar"),
    EN_CAMINO("Unidades en camino"),
    EN_ATENCION("En atención"),
    RESUELTO("Resuelto"),
    EXPIRADO("Expirado");

    private final String nombre;

    EstadoIncidente(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean activo() {
        return this != RESUELTO && this != EXPIRADO;
    }

    /** Todavía corre el plazo de respuesta. */
    public boolean pendiente() {
        return this == REPORTADO || this == EN_CAMINO;
    }
}
