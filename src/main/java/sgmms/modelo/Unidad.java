package sgmms.modelo;

import sgmms.estructuras.ListaEnlazada;

/**
 * Vehículo de emergencia. Mientras lo maneja el piloto automático avanza tramo
 * a tramo por su {@link Plan}; cuando el jugador toma el volante se mueve libre
 * por el asfalto y solo se usa su posición (x, y).
 */
public class Unidad {

    private final String id;
    private final TipoUnidad tipo;
    private final Estacion estacion;

    private EstadoUnidad estado = EstadoUnidad.EN_BASE;
    private Incidente incidente;

    private Plan plan;
    private final ListaEnlazada<Tramo> tramosPendientes = new ListaEnlazada<>();
    private Tramo tramoActual;
    private double s;

    private double x;
    private double y;
    private double angulo;
    private boolean manual;

    private double distanciaRecorrida;
    private int despachos;
    private double segundosDesdePlan;
    private int versionCierresPlan;

    public Unidad(String id, TipoUnidad tipo, Estacion estacion) {
        this.id = id;
        this.tipo = tipo;
        this.estacion = estacion;
        this.x = estacion.getNodo().getX();
        this.y = estacion.getNodo().getY();
    }

    /** Reemplaza el recorrido actual por uno nuevo. */
    public void seguir(Plan nuevo, int versionCierres) {
        plan = nuevo;
        tramosPendientes.limpiar();
        for (Tramo t : nuevo.tramos()) tramosPendientes.agregar(t);
        tramoActual = tramosPendientes.estaVacia() ? null : tramosPendientes.eliminarPrimero();
        s = tramoActual == null ? 0 : tramoActual.desde();
        segundosDesdePlan = 0;
        versionCierresPlan = versionCierres;
    }

    public void detener() {
        plan = null;
        tramosPendientes.limpiar();
        tramoActual = null;
    }

    /** Avanza {@code distancia} píxeles por el plan. Devuelve true si llegó al final. */
    public boolean avanzar(double distancia) {
        while (tramoActual != null && distancia > 0) {
            double resto = tramoActual.hasta() - s;
            if (distancia < resto) {
                s += distancia;
                distanciaRecorrida += distancia;
                distancia = 0;
            } else {
                s = tramoActual.hasta();
                distanciaRecorrida += resto;
                distancia -= resto;
                if (tramosPendientes.estaVacia()) {
                    actualizarPosicion();
                    return true;
                }
                tramoActual = tramosPendientes.eliminarPrimero();
                s = tramoActual.desde();
            }
        }
        actualizarPosicion();
        return tramoActual == null;
    }

    private void actualizarPosicion() {
        if (tramoActual == null) return;
        x = tramoActual.via().xEn(s);
        y = tramoActual.via().yEn(s);
        angulo = Math.atan2(tramoActual.via().getDirY(), tramoActual.via().getDirX());
    }

    /** Punto actual sobre la red, o null si está en base o sin plan. */
    public PuntoVia puntoActual() {
        return tramoActual == null ? null : new PuntoVia(tramoActual.via(), s);
    }

    /** Tramos que faltan, empezando por el actual recortado en la posición de la unidad. */
    public ListaEnlazada<Tramo> recorridoRestante() {
        ListaEnlazada<Tramo> resto = new ListaEnlazada<>();
        if (tramoActual == null) return resto;
        resto.agregar(new Tramo(tramoActual.via(), s, tramoActual.hasta()));
        resto.agregarTodos(tramosPendientes);
        return resto;
    }

    public double distanciaRestante() {
        double total = 0;
        for (Tramo t : recorridoRestante()) total += t.longitud();
        return total;
    }

    public void sumarTiempoPlan(double dt) {
        segundosDesdePlan += dt;
    }

    public void moverLibre(double nuevoX, double nuevoY, double nuevoAngulo) {
        distanciaRecorrida += Math.hypot(nuevoX - x, nuevoY - y);
        x = nuevoX;
        y = nuevoY;
        angulo = nuevoAngulo;
    }

    /** Deja la unidad quieta sobre un punto de la red (al llegar manejando o al soltar el volante). */
    public void ubicarEn(PuntoVia punto, int versionCierres) {
        ListaEnlazada<Tramo> quieto = new ListaEnlazada<>();
        quieto.agregar(new Tramo(punto.via(), punto.s(), punto.s()));
        seguir(new Plan(quieto, 0, 0), versionCierres);
        actualizarPosicion();
    }

    public void ubicarEnBase() {
        detener();
        x = estacion.getNodo().getX();
        y = estacion.getNodo().getY();
    }

    public void contarDespacho() {
        despachos++;
    }

    // ---- Accesores ----

    public String getId() {
        return id;
    }

    public TipoUnidad getTipo() {
        return tipo;
    }

    public Estacion getEstacion() {
        return estacion;
    }

    public EstadoUnidad getEstado() {
        return estado;
    }

    public void setEstado(EstadoUnidad estado) {
        this.estado = estado;
    }

    public Incidente getIncidente() {
        return incidente;
    }

    public void setIncidente(Incidente incidente) {
        this.incidente = incidente;
    }

    public Plan getPlan() {
        return plan;
    }

    public boolean tienePlan() {
        return tramoActual != null;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngulo() {
        return angulo;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public double getDistanciaRecorrida() {
        return distanciaRecorrida;
    }

    public int getDespachos() {
        return despachos;
    }

    public double getSegundosDesdePlan() {
        return segundosDesdePlan;
    }

    public int getVersionCierresPlan() {
        return versionCierresPlan;
    }

    @Override
    public String toString() {
        return id;
    }
}
