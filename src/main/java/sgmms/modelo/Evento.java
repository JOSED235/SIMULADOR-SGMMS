package sgmms.modelo;

/** Entrada de la bitácora del centro de despacho. */
public record Evento(double tiempo, String texto, Nivel nivel) {

    public enum Nivel { INFO, EXITO, ALERTA }
}
