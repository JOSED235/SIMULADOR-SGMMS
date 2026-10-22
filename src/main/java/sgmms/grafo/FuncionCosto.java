package sgmms.grafo;

/**
 * Cuánto cuesta, en segundos por píxel, recorrer una vía para un vehículo dado.
 * Así el mismo buscador sirve para autos civiles, unidades con sirena o una
 * estrategia que ignora el tráfico.
 */
@FunctionalInterface
public interface FuncionCosto {

    double segundosPorPixel(Via via);

    default boolean transitable(Via via) {
        return !via.estaCerrada();
    }

    default double costo(Via via) {
        return via.getLongitud() * segundosPorPixel(via);
    }

    /** Tiempo con el tráfico actual para un vehículo de velocidad libre {@code velocidad} px/s. */
    static FuncionCosto conTrafico(double velocidad) {
        return via -> 1.0 / (velocidad * via.factorCongestion());
    }

    /** Las unidades con sirena sufren menos la congestión. */
    static FuncionCosto conSirena(double velocidad) {
        return via -> 1.0 / (velocidad * via.factorSirena());
    }

    /** Solo distancia: la vía vale lo mismo esté vacía o trancada. */
    static FuncionCosto soloDistancia(double velocidad) {
        return via -> 1.0 / velocidad;
    }
}
