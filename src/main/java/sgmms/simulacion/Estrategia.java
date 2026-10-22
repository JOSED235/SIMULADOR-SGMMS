package sgmms.simulacion;

import sgmms.grafo.FuncionCosto;
import sgmms.modelo.TipoUnidad;

/**
 * Cómo elige y enruta unidades el centro de despacho. El juego usa siempre
 * {@link #DINAMICA}; {@link #ESTATICA} existe para medir, con el benchmark,
 * cuánto aporta tener en cuenta el tráfico en vivo.
 */
public enum Estrategia {

    /** Elige la unidad con menor ETA según A* con tráfico actual y replanifica en ruta. */
    DINAMICA("Menor ETA con tráfico en vivo (A* + replanificación)"),
    /** Elige la unidad más cercana en línea recta y planifica solo por distancia. */
    ESTATICA("Más cercana en línea recta, ruta por distancia");

    private final String descripcion;

    Estrategia(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public FuncionCosto costo(TipoUnidad tipo) {
        return this == DINAMICA ? FuncionCosto.conSirena(tipo.getVelocidad()) : FuncionCosto.soloDistancia(tipo.getVelocidad());
    }

    /** Cada cuántos segundos se recalcula la ruta de una unidad en movimiento (aparte de los cierres de vía). */
    public double intervaloReplanificacion() {
        return this == DINAMICA ? 2.5 : Double.POSITIVE_INFINITY;
    }
}
