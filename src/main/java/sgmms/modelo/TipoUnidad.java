package sgmms.modelo;

/** Tipos de unidad de emergencia y su velocidad libre en píxeles por segundo. */
public enum TipoUnidad {
    PATRULLA("Patrulla", "Policía", 56),
    AMBULANCIA("Ambulancia", "Hospital", 51),
    BOMBEROS("Bomberos", "Bomberos", 45);

    private final String nombre;
    private final String nombreEstacion;
    private final double velocidad;

    TipoUnidad(String nombre, String nombreEstacion, double velocidad) {
        this.nombre = nombre;
        this.nombreEstacion = nombreEstacion;
        this.velocidad = velocidad;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreEstacion() {
        return nombreEstacion;
    }

    public double getVelocidad() {
        return velocidad;
    }

    public char inicial() {
        return nombre.charAt(0);
    }
}
