package sgmms.modelo;

import sgmms.estructuras.ListaEnlazada;

/** Emergencia reportada en un punto de la red vial. */
public class Incidente {

    /** Orden de la cola de triage: primero el plazo más cercano (EDF), desempate por id. */
    public record ClaveTriage(double plazo, int id) implements Comparable<ClaveTriage> {
        @Override
        public int compareTo(ClaveTriage o) {
            int c = Double.compare(plazo, o.plazo);
            return c != 0 ? c : Integer.compare(id, o.id);
        }
    }

    private final int id;
    private final TipoIncidente tipo;
    private int gravedad;
    private final PuntoVia ubicacion;
    private final double tiempoReporte;
    private final double plazo;
    private final ClaveTriage clave;

    private EstadoIncidente estado = EstadoIncidente.REPORTADO;
    private final ListaEnlazada<Unidad> asignadas = new ListaEnlazada<>();
    private double progreso;
    private double tiempoLlegada = -1;
    private boolean escalado;
    private int puntos;

    public Incidente(int id, TipoIncidente tipo, int gravedad, PuntoVia ubicacion, double tiempoReporte, double plazo) {
        this.id = id;
        this.tipo = tipo;
        this.gravedad = gravedad;
        this.ubicacion = ubicacion;
        this.tiempoReporte = tiempoReporte;
        this.plazo = plazo;
        this.clave = new ClaveTriage(plazo, id);
    }

    public ListaEnlazada<TipoUnidad> requeridas() {
        return tipo.requeridas(gravedad);
    }

    /** Tipos de unidad que todavía no tienen una unidad asignada. */
    public ListaEnlazada<TipoUnidad> faltantes() {
        ListaEnlazada<TipoUnidad> faltan = requeridas();
        for (Unidad u : asignadas) faltan.eliminar(u.getTipo());
        return faltan;
    }

    /** Ya están en escena todas las unidades requeridas. */
    public boolean equipoCompletoEnEscena() {
        ListaEnlazada<TipoUnidad> faltan = requeridas();
        for (Unidad u : asignadas) {
            if (u.getEstado() == EstadoUnidad.EN_ESCENA) faltan.eliminar(u.getTipo());
        }
        return faltan.estaVacia();
    }

    public boolean necesita(TipoUnidad tipoUnidad) {
        return faltantes().contiene(tipoUnidad);
    }

    public double tiempoAtencion() {
        return tipo.tiempoAtencion(gravedad);
    }

    public double restante(double ahora) {
        return plazo - ahora;
    }

    /** Fracción del plazo ya consumida (0 a 1). */
    public double urgencia(double ahora) {
        double total = plazo - tiempoReporte;
        return Math.max(0, Math.min(1, (ahora - tiempoReporte) / total));
    }

    public double tiempoRespuesta() {
        return tiempoLlegada < 0 ? -1 : tiempoLlegada - tiempoReporte;
    }

    public double getX() {
        return ubicacion.x();
    }

    public double getY() {
        return ubicacion.y();
    }

    public void escalar() {
        if (gravedad < 3) gravedad++;
        escalado = true;
    }

    public void avanzarAtencion(double dt) {
        progreso += dt;
    }

    // ---- Accesores ----

    public int getId() {
        return id;
    }

    public TipoIncidente getTipo() {
        return tipo;
    }

    public int getGravedad() {
        return gravedad;
    }

    public PuntoVia getUbicacion() {
        return ubicacion;
    }

    public double getTiempoReporte() {
        return tiempoReporte;
    }

    public double getPlazo() {
        return plazo;
    }

    public ClaveTriage getClave() {
        return clave;
    }

    public EstadoIncidente getEstado() {
        return estado;
    }

    public void setEstado(EstadoIncidente estado) {
        this.estado = estado;
    }

    public ListaEnlazada<Unidad> getAsignadas() {
        return asignadas;
    }

    public double getProgreso() {
        return progreso;
    }

    /** Fracción de la atención en escena completada (0 a 1). */
    public double fraccionAtencion() {
        return Math.min(1, progreso / tiempoAtencion());
    }

    public double getTiempoLlegada() {
        return tiempoLlegada;
    }

    public void setTiempoLlegada(double tiempoLlegada) {
        this.tiempoLlegada = tiempoLlegada;
    }

    public boolean isEscalado() {
        return escalado;
    }

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public String etiqueta() {
        return "#" + id + " " + tipo.getNombre();
    }

    @Override
    public String toString() {
        return etiqueta();
    }
}
