package sgmms.simulacion;

import sgmms.estructuras.ListaEnlazada;
import sgmms.estructuras.MonticuloMin;
import sgmms.modelo.TipoIncidente;

/** Indicadores operativos del turno: tiempos de respuesta, cumplimiento y evolución. */
public class Metricas {

    /** Foto de la ciudad cada cierto tiempo, para las gráficas del informe. */
    public record Muestra(double tiempo, int incidentesActivos, double congestion, int puntaje) {
    }

    private final int[] generados = new int[TipoIncidente.values().length];
    private final int[] resueltos = new int[TipoIncidente.values().length];
    private final int[] expirados = new int[TipoIncidente.values().length];
    private final double[] sumaRespuesta = new double[TipoIncidente.values().length];
    private final int[] respuestas = new int[TipoIncidente.values().length];
    private final ListaEnlazada<Double> tiemposRespuesta = new ListaEnlazada<>();
    private final ListaEnlazada<Muestra> muestras = new ListaEnlazada<>();
    private int maximoSimultaneos;
    private int reasignaciones;
    private int replanificaciones;

    public void registrarGenerado(TipoIncidente tipo) {
        generados[tipo.ordinal()]++;
    }

    public void registrarRespuesta(TipoIncidente tipo, double segundos) {
        tiemposRespuesta.agregar(segundos);
        sumaRespuesta[tipo.ordinal()] += segundos;
        respuestas[tipo.ordinal()]++;
    }

    public void registrarResuelto(TipoIncidente tipo) {
        resueltos[tipo.ordinal()]++;
    }

    public void registrarExpirado(TipoIncidente tipo) {
        expirados[tipo.ordinal()]++;
    }

    public void registrarSimultaneos(int activos) {
        maximoSimultaneos = Math.max(maximoSimultaneos, activos);
    }

    public void registrarReasignacion() {
        reasignaciones++;
    }

    public void registrarReplanificacion() {
        replanificaciones++;
    }

    public void registrarMuestra(Muestra muestra) {
        muestras.agregar(muestra);
    }

    public int generados() {
        return suma(generados);
    }

    public int resueltos() {
        return suma(resueltos);
    }

    public int expirados() {
        return suma(expirados);
    }

    public int generados(TipoIncidente tipo) {
        return generados[tipo.ordinal()];
    }

    public int resueltos(TipoIncidente tipo) {
        return resueltos[tipo.ordinal()];
    }

    public int expirados(TipoIncidente tipo) {
        return expirados[tipo.ordinal()];
    }

    /** Respuestas a tiempo sobre incidentes cerrados (resueltos + expirados), de 0 a 1. */
    public double cumplimiento() {
        int cerrados = resueltos() + expirados();
        return cerrados == 0 ? 1 : (double) resueltos() / cerrados;
    }

    public double respuestaPromedio() {
        if (tiemposRespuesta.estaVacia()) return 0;
        double suma = 0;
        for (double t : tiemposRespuesta) suma += t;
        return suma / tiemposRespuesta.tamano();
    }

    public double respuestaPromedio(TipoIncidente tipo) {
        int n = cantidadRespuestas(tipo);
        return n == 0 ? 0 : sumaRespuesta[tipo.ordinal()] / n;
    }

    /** Percentil por rango más cercano, ordenando con el montículo propio. */
    public double percentilRespuesta(double percentil) {
        if (tiemposRespuesta.estaVacia()) return 0;
        ListaEnlazada<Double> ordenados = MonticuloMin.ordenar(tiemposRespuesta, Double::compare);
        int rango = (int) Math.ceil(percentil / 100.0 * ordenados.tamano());
        return ordenados.obtener(Math.max(0, rango - 1));
    }

    public ListaEnlazada<Double> getTiemposRespuesta() {
        return tiemposRespuesta;
    }

    public ListaEnlazada<Muestra> getMuestras() {
        return muestras;
    }

    public int getMaximoSimultaneos() {
        return maximoSimultaneos;
    }

    public int getReasignaciones() {
        return reasignaciones;
    }

    public int getReplanificaciones() {
        return replanificaciones;
    }

    private int cantidadRespuestas(TipoIncidente tipo) {
        return respuestas[tipo.ordinal()];
    }

    private static int suma(int[] valores) {
        int total = 0;
        for (int v : valores) total += v;
        return total;
    }
}
