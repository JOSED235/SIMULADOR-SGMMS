package sgmms.modelo;

import sgmms.estructuras.ListaEnlazada;

/** Tipos de incidente: qué unidades exigen, cuánto tarda atenderlos y si cierran la calle. */
public enum TipoIncidente {
    ROBO("Robo", 6, false, TipoUnidad.PATRULLA),
    EMERGENCIA_MEDICA("Emergencia médica", 8, false, TipoUnidad.AMBULANCIA),
    INCENDIO("Incendio", 12, false, TipoUnidad.BOMBEROS),
    ACCIDENTE("Accidente de tránsito", 10, true, TipoUnidad.PATRULLA, TipoUnidad.AMBULANCIA);

    private final String nombre;
    private final double atencionBase;
    private final boolean cierraVia;
    private final TipoUnidad[] requeridas;

    TipoIncidente(String nombre, double atencionBase, boolean cierraVia, TipoUnidad... requeridas) {
        this.nombre = nombre;
        this.atencionBase = atencionBase;
        this.cierraVia = cierraVia;
        this.requeridas = requeridas;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean cierraVia() {
        return cierraVia;
    }

    /** Segundos de trabajo en escena una vez llegan todas las unidades. */
    public double tiempoAtencion(int gravedad) {
        return atencionBase * (0.8 + 0.2 * gravedad);
    }

    /** Unidades necesarias; un incendio de gravedad 3 exige dos camiones. */
    public ListaEnlazada<TipoUnidad> requeridas(int gravedad) {
        ListaEnlazada<TipoUnidad> lista = new ListaEnlazada<>();
        for (TipoUnidad t : requeridas) lista.agregar(t);
        if (this == INCENDIO && gravedad >= 3) lista.agregar(TipoUnidad.BOMBEROS);
        return lista;
    }
}
