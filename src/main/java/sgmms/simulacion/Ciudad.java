package sgmms.simulacion;

import sgmms.estructuras.ListaEnlazada;
import sgmms.grafo.Grafo;
import sgmms.grafo.Nodo;
import sgmms.grafo.Via;
import sgmms.modelo.Estacion;
import sgmms.modelo.PuntoVia;
import sgmms.modelo.TipoUnidad;

/**
 * Trazado de la ciudad: intersecciones, calles, manzanas y estaciones.
 * Las coordenadas salen del mapa de la versión original del proyecto (1088 x 1030 px).
 */
public class Ciudad {

    public static final double ANCHO = 1088;
    public static final double ALTO = 1030;
    /** Media calzada: las calles miden 32 px de ancho. */
    public static final double MEDIO_ANCHO_CALLE = 16;

    public enum TipoManzana { EDIFICIOS, PARQUE, PLAZA }

    public record Manzana(double x1, double y1, double x2, double y2, TipoManzana tipo) {
        public double ancho() {
            return x2 - x1;
        }

        public double alto() {
            return y2 - y1;
        }
    }

    private final Grafo grafo = new Grafo();
    private final ListaEnlazada<Manzana> manzanas = new ListaEnlazada<>();
    private final ListaEnlazada<Estacion> estaciones = new ListaEnlazada<>();
    private final ListaEnlazada<Nodo> salidas = new ListaEnlazada<>();
    /** Un carril por calle (el sentido a→b); sirve para dibujar y para las colisiones. */
    private final ListaEnlazada<Via> calles = new ListaEnlazada<>();

    public Ciudad() {
        crearNodos();
        crearCalles();
        crearManzanas();
        crearEstaciones();
    }

    private void crearNodos() {
        salida("T1", 390, 0);
        salida("T2", 828, 0);

        salida("L1", 0, 229);
        nodo("A1", 240, 229);
        nodo("A2", 390, 229);
        nodo("A3", 561, 229);
        nodo("A4", 828, 229);
        salida("R1", ANCHO, 229);

        salida("L2", 0, 389);
        nodo("B1", 240, 389);
        nodo("B2", 561, 389);

        salida("L3", 0, 560);
        nodo("C1", 240, 560);
        nodo("C2", 561, 560);
        nodo("C3", 806, 560);
        nodo("C4", 945, 560);
        salida("R3", ANCHO, 560);

        nodo("D1", 240, 785);
        nodo("D2", 507, 785);
        nodo("D3", 561, 785);
        nodo("D4", 806, 785);

        salida("E1", 240, ALTO);
        salida("E2", 507, ALTO);
        salida("E3", 806, ALTO);
        salida("E4", 945, ALTO);
    }

    private void crearCalles() {
        // Horizontales
        calle("L1", "A1");
        calle("A1", "A2");
        calle("A2", "A3");
        calle("A3", "A4");
        calle("A4", "R1");
        calle("L2", "B1");
        calle("B1", "B2");
        calle("L3", "C1");
        calle("C1", "C2");
        calle("C2", "C3");
        calle("C3", "C4");
        calle("C4", "R3");
        calle("D1", "D2");
        calle("D2", "D3");
        calle("D3", "D4");
        // Verticales
        calle("T1", "A2");
        calle("T2", "A4");
        calle("A1", "B1");
        calle("B1", "C1");
        calle("C1", "D1");
        calle("D1", "E1");
        calle("A3", "B2");
        calle("B2", "C2");
        calle("C2", "D3");
        calle("D2", "E2");
        calle("C3", "D4");
        calle("D4", "E3");
        calle("C4", "E4");
    }

    private void crearManzanas() {
        manzana(0, 0, 374, 213, TipoManzana.EDIFICIOS);
        manzana(406, 0, 812, 213, TipoManzana.EDIFICIOS);
        manzana(844, 0, ANCHO, 213, TipoManzana.EDIFICIOS);

        manzana(0, 245, 224, 373, TipoManzana.EDIFICIOS);
        manzana(256, 245, 545, 373, TipoManzana.EDIFICIOS);
        manzana(577, 245, ANCHO, 544, TipoManzana.PARQUE);

        manzana(0, 405, 224, 544, TipoManzana.EDIFICIOS);
        manzana(256, 405, 545, 544, TipoManzana.EDIFICIOS);

        manzana(0, 576, 224, ALTO, TipoManzana.EDIFICIOS);
        manzana(256, 576, 545, 769, TipoManzana.PLAZA);
        manzana(577, 576, 790, 769, TipoManzana.EDIFICIOS);
        manzana(822, 576, 929, ALTO, TipoManzana.EDIFICIOS);
        manzana(961, 576, ANCHO, ALTO, TipoManzana.EDIFICIOS);

        manzana(256, 801, 491, ALTO, TipoManzana.EDIFICIOS);
        manzana(523, 801, 790, ALTO, TipoManzana.EDIFICIOS);
    }

    private void crearEstaciones() {
        estaciones.agregar(new Estacion(TipoUnidad.PATRULLA, grafo.nodo("B2"), 497, 337));
        estaciones.agregar(new Estacion(TipoUnidad.AMBULANCIA, grafo.nodo("A4"), 758, 176));
        estaciones.agregar(new Estacion(TipoUnidad.BOMBEROS, grafo.nodo("D1"), 176, 737));
    }

    private void nodo(String id, double x, double y) {
        grafo.agregarNodo(id, x, y, false);
    }

    private void salida(String id, double x, double y) {
        salidas.agregar(grafo.agregarNodo(id, x, y, true));
    }

    private void calle(String a, String b) {
        calles.agregar(grafo.agregarCalle(a, b));
    }

    private void manzana(double x1, double y1, double x2, double y2, TipoManzana tipo) {
        manzanas.agregar(new Manzana(x1, y1, x2, y2, tipo));
    }

    // ---- Consultas geométricas ----

    /** El punto está sobre el asfalto, dejando {@code margen} px hasta el andén. */
    public boolean enAsfalto(double x, double y, double margen) {
        if (x < 0 || y < 0 || x > ANCHO || y > ALTO) return false;
        double medio = MEDIO_ANCHO_CALLE - margen;
        for (Via c : calles) {
            double minX = Math.min(c.getOrigen().getX(), c.getDestino().getX()) - medio;
            double maxX = Math.max(c.getOrigen().getX(), c.getDestino().getX()) + medio;
            double minY = Math.min(c.getOrigen().getY(), c.getDestino().getY()) - medio;
            double maxY = Math.max(c.getOrigen().getY(), c.getDestino().getY()) + medio;
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) return true;
        }
        return false;
    }

    /**
     * Punto de la red más cercano a (x, y). Entre los dos carriles de la calle elige
     * el que va en la dirección del ángulo dado.
     */
    public PuntoVia proyectar(double x, double y, double angulo) {
        Via mejor = null;
        double mejorS = 0;
        double mejorDist = Double.POSITIVE_INFINITY;
        for (Via c : calles) {
            double px = x - c.getOrigen().getX();
            double py = y - c.getOrigen().getY();
            double s = Math.max(0, Math.min(c.getLongitud(), px * c.getDirX() + py * c.getDirY()));
            double d = Math.hypot(x - c.xEn(s), y - c.yEn(s));
            if (d < mejorDist) {
                mejorDist = d;
                mejor = c;
                mejorS = s;
            }
        }
        PuntoVia punto = new PuntoVia(mejor, mejorS);
        double dot = Math.cos(angulo) * mejor.getDirX() + Math.sin(angulo) * mejor.getDirY();
        return dot >= 0 ? punto : punto.enOpuesta();
    }

    public Estacion estacion(TipoUnidad tipo) {
        for (Estacion e : estaciones) {
            if (e.getTipo() == tipo) return e;
        }
        throw new IllegalStateException("Sin estación de " + tipo);
    }

    public Grafo getGrafo() {
        return grafo;
    }

    public ListaEnlazada<Manzana> getManzanas() {
        return manzanas;
    }

    public ListaEnlazada<Estacion> getEstaciones() {
        return estaciones;
    }

    public ListaEnlazada<Nodo> getSalidas() {
        return salidas;
    }

    public ListaEnlazada<Via> getCalles() {
        return calles;
    }
}
