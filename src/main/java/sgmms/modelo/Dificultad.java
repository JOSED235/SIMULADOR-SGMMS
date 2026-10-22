package sgmms.modelo;

/** Parámetros de un turno. */
public enum Dificultad {
    FACIL("Fácil", 240, 15, 9, 55, 3, 3, 2, 5, 1.25),
    NORMAL("Normal", 300, 12, 6.5, 80, 3, 3, 2, 4, 1.0),
    DIFICIL("Difícil", 360, 10, 5.5, 100, 3, 2, 2, 4, 0.9);

    private final String nombre;
    private final double duracionTurno;
    private final double intervaloInicial;
    private final double intervaloFinal;
    private final int autosCiviles;
    private final int patrullas;
    private final int ambulancias;
    private final int bomberos;
    private final int fallosPermitidos;
    private final double factorPlazo;

    Dificultad(String nombre, double duracionTurno, double intervaloInicial, double intervaloFinal, int autosCiviles,
               int patrullas, int ambulancias, int bomberos, int fallosPermitidos, double factorPlazo) {
        this.nombre = nombre;
        this.duracionTurno = duracionTurno;
        this.intervaloInicial = intervaloInicial;
        this.intervaloFinal = intervaloFinal;
        this.autosCiviles = autosCiviles;
        this.patrullas = patrullas;
        this.ambulancias = ambulancias;
        this.bomberos = bomberos;
        this.fallosPermitidos = fallosPermitidos;
        this.factorPlazo = factorPlazo;
    }

    public String getNombre() {
        return nombre;
    }

    public double getDuracionTurno() {
        return duracionTurno;
    }

    /** Segundos entre incidentes: baja linealmente a lo largo del turno. */
    public double intervaloIncidentes(double progreso) {
        return intervaloInicial + (intervaloFinal - intervaloInicial) * Math.min(1, Math.max(0, progreso));
    }

    public int getAutosCiviles() {
        return autosCiviles;
    }

    public int unidades(TipoUnidad tipo) {
        return switch (tipo) {
            case PATRULLA -> patrullas;
            case AMBULANCIA -> ambulancias;
            case BOMBEROS -> bomberos;
        };
    }

    public int getFallosPermitidos() {
        return fallosPermitidos;
    }

    /** Plazo de respuesta en segundos según la gravedad (1 a 3). */
    public double plazo(int gravedad) {
        double base = switch (gravedad) {
            case 1 -> 55;
            case 2 -> 45;
            default -> 35;
        };
        return base * factorPlazo;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
