package sgmms.simulacion;

import sgmms.estructuras.ListaEnlazada;
import sgmms.estructuras.TablaHash;
import sgmms.grafo.BuscadorRutas;
import sgmms.grafo.FuncionCosto;
import sgmms.grafo.Grafo;
import sgmms.grafo.Nodo;
import sgmms.grafo.Via;
import sgmms.modelo.VehiculoCivil;

import java.util.Random;

/**
 * Tráfico civil. Cada auto sigue al de adelante en su carril, espera a que haya
 * espacio para entrar a la siguiente cuadra y recalcula su ruta en cada esquina
 * con el estado actual de la red. La ocupación de cada vía es lo que encarece
 * su peso en el grafo, así que los trancones cambian las rutas de todos.
 */
public class Trafico {

    public static final double VELOCIDAD_CIVIL = 40;
    public static final double LARGO_AUTO = 12;
    private static final double DISTANCIA_SEGURIDAD = 7;
    private static final double ACELERACION = 60;
    private static final double ESPERA_MAXIMA_ESQUINA = 2.5;
    private static final double ESPERA_ANTES_DE_DEVOLVERSE = 3.5;
    private static final double INTERVALO_ENTRADA = 0.35;
    private static final int COLORES = 8;

    private final Grafo grafo;
    private final ListaEnlazada<Nodo> salidas;
    private final Random rng;
    private final FuncionCosto costo = FuncionCosto.conTrafico(VELOCIDAD_CIVIL);

    private final ListaEnlazada<VehiculoCivil> autos = new ListaEnlazada<>();
    private final TablaHash<Via, ListaEnlazada<VehiculoCivil>> porVia = new TablaHash<>();
    /** Punto de cada carril donde un incidente bloquea el paso. */
    private final TablaHash<Via, Double> bloqueos = new TablaHash<>();

    private int objetivo;
    private double proximaEntrada;
    private int siguienteId = 1;

    public Trafico(Grafo grafo, ListaEnlazada<Nodo> salidas, Random rng) {
        this.grafo = grafo;
        this.salidas = salidas;
        this.rng = rng;
        for (Via v : grafo.vias()) porVia.poner(v, new ListaEnlazada<>());
    }

    /** Llena la ciudad al iniciar para no arrancar con las calles vacías. */
    public void poblar(int cantidad) {
        objetivo = cantidad;
        ListaEnlazada<Via> vias = grafo.vias();
        int intentos = 0;
        while (autos.tamano() < cantidad && intentos++ < cantidad * 20) {
            Via via = vias.obtener(rng.nextInt(vias.tamano()));
            double s = 10 + rng.nextDouble() * Math.max(1, via.getLongitud() - 20);
            if (hayEspacio(via, s)) crearAuto(via, s);
        }
    }

    public void setObjetivo(int objetivo) {
        this.objetivo = objetivo;
    }

    public void bloquear(Via via, double s) {
        bloqueos.poner(via, s);
    }

    public void desbloquear(Via via) {
        bloqueos.eliminar(via);
    }

    public void actualizar(double dt) {
        proximaEntrada -= dt;
        if (autos.tamano() < objetivo && proximaEntrada <= 0) {
            proximaEntrada = INTERVALO_ENTRADA;
            Nodo entrada = salidas.obtener(rng.nextInt(salidas.tamano()));
            Via via = grafo.salientes(entrada).primero();
            if (!via.estaCerrada() && hayEspacio(via, 0)) crearAuto(via, 0);
        }

        // Se recorre una copia: los autos pueden salir de la ciudad durante la actualización
        ListaEnlazada<VehiculoCivil> copia = new ListaEnlazada<>();
        copia.agregarTodos(autos);
        for (VehiculoCivil auto : copia) mover(auto, dt);
    }

    /** Los autos que van delante de una sirena, a menos de 55 px, se orillan. */
    public void notificarSirena(Via via, double s) {
        for (VehiculoCivil auto : porVia.obtener(via)) {
            if (auto.getS() > s && auto.getS() - s < 55) auto.orillar();
        }
    }

    private void mover(VehiculoCivil auto, double dt) {
        auto.avanzarOrillado(dt);
        Via via = auto.getVia();
        double limite = limiteDelante(auto);
        boolean bloqueadoPorIncidente = false;

        Double bloqueo = bloqueos.obtener(via);
        if (bloqueo != null && auto.getS() < bloqueo) {
            double antesDelBloqueo = bloqueo - LARGO_AUTO - DISTANCIA_SEGURIDAD;
            if (antesDelBloqueo < limite) {
                limite = antesDelBloqueo;
                bloqueadoPorIncidente = true;
            }
        }

        double deseada = auto.getVelocidadLibre() * via.factorCongestion();
        if (auto.getOrillado() > 0) deseada *= 0.5;
        double porEspacio = Math.max(0, limite - auto.getS()) * 3;
        double v = Math.min(Math.min(deseada, porEspacio), auto.getVelocidad() + ACELERACION * dt);
        auto.setVelocidad(v);
        auto.setS(Math.min(limite, auto.getS() + v * dt));

        if (v < 1) auto.sumarEspera(dt);
        else auto.reiniciarEspera();

        if (bloqueadoPorIncidente && auto.getEspera() > ESPERA_ANTES_DE_DEVOLVERSE) {
            devolverse(auto);
            return;
        }
        if (auto.getS() >= via.getLongitud() - 0.01) llegarAEsquina(auto);
    }

    /** Hasta dónde puede avanzar sin chocar con el de adelante (o el final de la cuadra). */
    private double limiteDelante(VehiculoCivil auto) {
        double limite = auto.getVia().getLongitud();
        for (VehiculoCivil otro : porVia.obtener(auto.getVia())) {
            if (otro != auto && otro.getS() > auto.getS()) {
                limite = Math.min(limite, otro.getS() - LARGO_AUTO - DISTANCIA_SEGURIDAD);
            }
        }
        return limite;
    }

    private void llegarAEsquina(VehiculoCivil auto) {
        Nodo esquina = auto.getVia().getDestino();
        if (esquina.equals(auto.getDestino())) {
            retirar(auto);
            return;
        }
        Via siguiente = auto.siguienteVia();
        if (auto.getRutaCalculadaEn() != esquina || siguiente == null || siguiente.estaCerrada()) {
            recalcularRuta(auto, esquina);
            siguiente = auto.siguienteVia();
        }
        if (siguiente == null) {
            retirar(auto); // sin salida posible: se estaciona
            return;
        }
        if (hayEspacio(siguiente, 0) || auto.getEspera() > ESPERA_MAXIMA_ESQUINA) {
            cambiarDeVia(auto, siguiente);
        }
    }

    private void devolverse(VehiculoCivil auto) {
        Via actual = auto.getVia();
        double s = actual.getLongitud() - auto.getS();
        cambiarDeVia(auto, actual.getOpuesta());
        auto.setS(s);
        auto.setVelocidad(0);
        recalcularRuta(auto, actual.getOpuesta().getDestino());
    }

    private void recalcularRuta(VehiculoCivil auto, Nodo desde) {
        BuscadorRutas.Resultado r = BuscadorRutas.aEstrella(grafo, desde, auto.getDestino(), costo, 1.0 / VELOCIDAD_CIVIL);
        auto.nuevaRuta(desde, r.vias());
    }

    private void cambiarDeVia(VehiculoCivil auto, Via siguiente) {
        porVia.obtener(auto.getVia()).eliminar(auto);
        auto.getVia().saleAuto();
        auto.entrarA(siguiente);
        porVia.obtener(siguiente).agregar(auto);
        siguiente.entraAuto();
    }

    private boolean hayEspacio(Via via, double s) {
        for (VehiculoCivil otro : porVia.obtener(via)) {
            if (Math.abs(otro.getS() - s) < LARGO_AUTO + DISTANCIA_SEGURIDAD) return false;
        }
        return true;
    }

    private void crearAuto(Via via, double s) {
        Nodo destino = elegirDestino(via.getOrigen());
        VehiculoCivil auto = new VehiculoCivil(siguienteId++, rng.nextInt(COLORES),
                VELOCIDAD_CIVIL * (0.85 + rng.nextDouble() * 0.3), via, destino);
        auto.setS(s);
        auto.setVelocidad(auto.getVelocidadLibre() * 0.5);
        autos.agregar(auto);
        porVia.obtener(via).agregar(auto);
        via.entraAuto();
        recalcularRuta(auto, via.getDestino());
    }

    /** La mayoría cruza la ciudad de borde a borde; algunos van a una esquina interior. */
    private Nodo elegirDestino(Nodo origen) {
        ListaEnlazada<Nodo> candidatos = rng.nextDouble() < 0.7 ? salidas : grafo.nodos();
        Nodo destino;
        do {
            destino = candidatos.obtener(rng.nextInt(candidatos.tamano()));
        } while (destino.equals(origen));
        return destino;
    }

    private void retirar(VehiculoCivil auto) {
        porVia.obtener(auto.getVia()).eliminar(auto);
        auto.getVia().saleAuto();
        autos.eliminar(auto);
    }

    public ListaEnlazada<VehiculoCivil> getAutos() {
        return autos;
    }

    public ListaEnlazada<VehiculoCivil> autosEn(Via via) {
        return porVia.obtener(via);
    }
}
