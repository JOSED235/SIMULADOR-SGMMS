package sgmms.simulacion;

import sgmms.estructuras.ArbolAVL;
import sgmms.estructuras.ListaEnlazada;
import sgmms.grafo.FuncionCosto;
import sgmms.grafo.Grafo;
import sgmms.grafo.Via;
import sgmms.modelo.Dificultad;
import sgmms.modelo.Efecto;
import sgmms.modelo.EstadoIncidente;
import sgmms.modelo.EstadoUnidad;
import sgmms.modelo.Estacion;
import sgmms.modelo.Evento;
import sgmms.modelo.Incidente;
import sgmms.modelo.Plan;
import sgmms.modelo.PuntoVia;
import sgmms.modelo.TipoIncidente;
import sgmms.modelo.TipoUnidad;
import sgmms.modelo.Tramo;
import sgmms.modelo.Unidad;
import sgmms.modelo.VehiculoCivil;
import sgmms.simulacion.Navegador.Extremo;

import java.util.Random;

/**
 * Motor del turno. Avanza en pasos fijos ({@link #PASO}) y no conoce JavaFX:
 * la interfaz solo lee su estado y le envía órdenes, por eso el mismo motor corre
 * en el juego, en las pruebas y en el benchmark sin cambios. Con la misma semilla
 * produce exactamente el mismo turno.
 */
public class Simulacion {

    public static final double PASO = 1.0 / 60;

    private static final double RADIO_LLEGADA_MANUAL = 22;
    private static final double MARGEN_ASFALTO = 5;
    private static final double FACTOR_VELOCIDAD_MANUAL = 1.15;
    private static final int MAX_INCIDENTES_ACTIVOS = 12;
    private static final int MAX_BITACORA = 80;
    private static final int PENALIZACION_EXPIRADO = 75;
    private static final double DISTANCIA_MINIMA_ENTRE_INCIDENTES = 70;
    private static final double MARGEN_MINIMO_DESVIO = 1.5;
    private static final double FRACCION_MINIMA_DESVIO = 0.12;

    public enum Resultado { EN_CURSO, TURNO_COMPLETO, TURNO_FALLIDO }

    /** Unidad sugerida para un incidente con la ruta que seguiría. */
    public record Recomendacion(Unidad unidad, Plan plan) {
        public double eta() {
            return plan.segundosEstimados();
        }
    }

    private final Dificultad dificultad;
    private final long semilla;
    private final Estrategia estrategia;
    private final Random rng;
    private final Ciudad ciudad;
    private final Grafo grafo;
    private final Trafico trafico;
    private final Metricas metricas = new Metricas();

    private final ListaEnlazada<Unidad> unidades = new ListaEnlazada<>();
    private final ListaEnlazada<Incidente> activos = new ListaEnlazada<>();
    private final ListaEnlazada<Incidente> cerrados = new ListaEnlazada<>();
    private final ArbolAVL<Incidente.ClaveTriage, Incidente> triage = new ArbolAVL<>();
    private final ListaEnlazada<Evento> bitacora = new ListaEnlazada<>();
    private final ListaEnlazada<Efecto> efectos = new ListaEnlazada<>();

    private double tiempo;
    private double proximoIncidente = 3;
    private double relojAutomatico;
    private double relojMuestra;
    private double relojGps;
    private int siguienteIdIncidente = 1;
    private int puntaje;
    private int fallos;
    private Resultado resultado = Resultado.EN_CURSO;
    private boolean despachoAutomatico;
    private boolean ignorarFallos;
    private boolean generacionAutomatica = true;

    private Unidad unidadManual;
    private boolean manualSalioDeBase;
    private double entradaX;
    private double entradaY;
    private Plan gps;

    public Simulacion(Dificultad dificultad, long semilla) {
        this(dificultad, semilla, Estrategia.DINAMICA);
    }

    public Simulacion(Dificultad dificultad, long semilla, Estrategia estrategia) {
        this.dificultad = dificultad;
        this.semilla = semilla;
        this.estrategia = estrategia;
        // Generadores separados: lo que haga el tráfico no altera la secuencia de incidentes
        this.rng = new Random(semilla);
        this.ciudad = new Ciudad();
        this.grafo = ciudad.getGrafo();
        this.trafico = new Trafico(grafo, ciudad.getSalidas(), new Random(semilla * 31 + 7));
        trafico.poblar(objetivoTrafico());

        for (Estacion estacion : ciudad.getEstaciones()) {
            TipoUnidad tipo = estacion.getTipo();
            for (int i = 1; i <= dificultad.unidades(tipo); i++) {
                Unidad u = new Unidad(tipo.inicial() + "-" + i, tipo, estacion);
                estacion.getUnidades().agregar(u);
                unidades.agregar(u);
            }
        }
        registrar("Inicio de turno · dificultad " + dificultad.getNombre() + " · semilla " + semilla, Evento.Nivel.INFO);
    }

    // =====================================================================
    // Ciclo principal
    // =====================================================================

    public void avanzar(double dt) {
        if (resultado != Resultado.EN_CURSO) return;
        tiempo += dt;

        trafico.setObjetivo(objetivoTrafico());
        if (generacionAutomatica && tiempo >= proximoIncidente) {
            generarIncidente();
            proximoIncidente = tiempo + dificultad.intervaloIncidentes(progreso()) * (0.75 + rng.nextDouble() * 0.5);
        }

        trafico.actualizar(dt);
        for (Unidad u : unidades) actualizarUnidad(u, dt);
        actualizarIncidentes(dt);

        for (Efecto e : efectos) e.avanzar(dt);
        efectos.eliminarSi(Efecto::terminado);

        if (despachoAutomatico && (relojAutomatico -= dt) <= 0) {
            relojAutomatico = 0.5;
            despacharAutomatico();
        }
        if (unidadManual != null && (relojGps -= dt) <= 0) {
            relojGps = 0.4;
            recalcularGps();
        }
        if ((relojMuestra -= dt) <= 0) {
            relojMuestra = 5;
            metricas.registrarMuestra(new Metricas.Muestra(tiempo, activos.tamano(), grafo.densidadPromedio(), puntaje));
        }

        if (!ignorarFallos && fallos >= dificultad.getFallosPermitidos()) terminar(Resultado.TURNO_FALLIDO);
        else if (tiempo >= dificultad.getDuracionTurno()) terminar(Resultado.TURNO_COMPLETO);
    }

    /** Hora pico a mitad de turno: hasta 30 % más autos que el valor base. */
    private int objetivoTrafico() {
        return (int) Math.round(dificultad.getAutosCiviles() * (0.8 + 0.5 * Math.sin(Math.PI * progreso())));
    }

    private void terminar(Resultado final_) {
        soltarControl();
        resultado = final_;
        metricas.registrarMuestra(new Metricas.Muestra(tiempo, activos.tamano(), grafo.densidadPromedio(), puntaje));
        registrar(final_ == Resultado.TURNO_COMPLETO ? "Turno completado" : "Turno fallido: demasiados incidentes sin atender",
                final_ == Resultado.TURNO_COMPLETO ? Evento.Nivel.EXITO : Evento.Nivel.ALERTA);
    }

    // =====================================================================
    // Incidentes
    // =====================================================================

    private void generarIncidente() {
        if (activos.tamano() >= MAX_INCIDENTES_ACTIVOS) return;
        PuntoVia punto = elegirPuntoLibre();
        if (punto == null) return;
        reportar(elegirTipo(), elegirGravedad(), punto);
    }

    /** Registra un incidente en un punto concreto (lo usan la generación aleatoria y las pruebas). */
    public Incidente reportar(TipoIncidente tipo, int gravedad, PuntoVia punto) {
        Incidente inc = new Incidente(siguienteIdIncidente++, tipo, gravedad, punto, tiempo, tiempo + dificultad.plazo(gravedad));
        activos.agregar(inc);
        triage.insertar(inc.getClave(), inc);
        metricas.registrarGenerado(tipo);
        metricas.registrarSimultaneos(activos.tamano());

        if (tipo.cierraVia()) {
            Via via = punto.via();
            grafo.cerrarCalle(via);
            trafico.bloquear(via, punto.s());
            trafico.bloquear(via.getOpuesta(), via.getLongitud() - punto.s());
        }
        registrar("Nuevo " + inc.etiqueta() + " · gravedad " + gravedad
                + (tipo.cierraVia() ? " · calle cerrada" : ""), Evento.Nivel.ALERTA);
        return inc;
    }

    private PuntoVia elegirPuntoLibre() {
        ListaEnlazada<Via> calles = ciudad.getCalles();
        for (int intento = 0; intento < 30; intento++) {
            Via via = calles.obtener(rng.nextInt(calles.tamano()));
            if (rng.nextBoolean()) via = via.getOpuesta();
            if (via.estaCerrada() || via.getLongitud() < 90) continue;
            PuntoVia p = new PuntoVia(via, via.getLongitud() * (0.25 + rng.nextDouble() * 0.5));
            if (p.x() < 60 || p.y() < 60 || p.x() > Ciudad.ANCHO - 60 || p.y() > Ciudad.ALTO - 60) continue;
            boolean libre = true;
            for (Incidente otro : activos) {
                if (Math.hypot(otro.getX() - p.x(), otro.getY() - p.y()) < DISTANCIA_MINIMA_ENTRE_INCIDENTES) {
                    libre = false;
                    break;
                }
            }
            if (libre) return p;
        }
        return null;
    }

    private TipoIncidente elegirTipo() {
        double r = rng.nextDouble();
        if (r < 0.28) return TipoIncidente.ROBO;
        if (r < 0.58) return TipoIncidente.EMERGENCIA_MEDICA;
        if (r < 0.78) return TipoIncidente.INCENDIO;
        return TipoIncidente.ACCIDENTE;
    }

    private int elegirGravedad() {
        double r = rng.nextDouble();
        return r < 0.45 ? 1 : r < 0.83 ? 2 : 3;
    }

    private void actualizarIncidentes(double dt) {
        ListaEnlazada<Incidente> copia = new ListaEnlazada<>();
        copia.agregarTodos(activos);
        for (Incidente inc : copia) {
            if (inc.getEstado().pendiente()) {
                if (inc.equipoCompletoEnEscena()) iniciarAtencion(inc);
                else if (tiempo >= inc.getPlazo()) expirar(inc);
                else if (debePropagarse(inc)) propagar(inc);
            } else if (inc.getEstado() == EstadoIncidente.EN_ATENCION) {
                inc.avanzarAtencion(dt);
                if (inc.getProgreso() >= inc.tiempoAtencion()) resolver(inc);
            }
        }
    }

    /** Un incendio que pasa la mitad de su plazo sin bomberos en escena crece. */
    private boolean debePropagarse(Incidente inc) {
        if (inc.getTipo() != TipoIncidente.INCENDIO || inc.isEscalado() || inc.getGravedad() >= 3) return false;
        if (inc.urgencia(tiempo) < 0.5) return false;
        for (Unidad u : inc.getAsignadas()) {
            if (u.getEstado() == EstadoUnidad.EN_ESCENA) return false;
        }
        return true;
    }

    private void propagar(Incidente inc) {
        inc.escalar();
        registrar("El fuego de " + inc.etiqueta() + " se propagó: gravedad " + inc.getGravedad()
                + (inc.getGravedad() == 3 ? ", ahora requiere 2 camiones" : ""), Evento.Nivel.ALERTA);
    }

    private void iniciarAtencion(Incidente inc) {
        inc.setEstado(EstadoIncidente.EN_ATENCION);
        inc.setTiempoLlegada(tiempo);
        triage.eliminar(inc.getClave());
        metricas.registrarRespuesta(inc.getTipo(), inc.tiempoRespuesta());
        registrar("En atención " + inc.etiqueta() + " · respuesta " + Math.round(inc.tiempoRespuesta()) + " s", Evento.Nivel.INFO);
    }

    private void resolver(Incidente inc) {
        inc.setEstado(EstadoIncidente.RESUELTO);
        int base = 100 * inc.getGravedad();
        int bono = (int) Math.round(base * 0.5 * (1 - inc.urgencia(inc.getTiempoLlegada())));
        inc.setPuntos(base + bono);
        puntaje += inc.getPuntos();
        metricas.registrarResuelto(inc.getTipo());
        efectos.agregar(new Efecto(inc.getX(), inc.getY(), "+" + inc.getPuntos(), true));
        registrar("Resuelto " + inc.etiqueta() + " · +" + inc.getPuntos() + " pts", Evento.Nivel.EXITO);
        cerrar(inc);
    }

    private void expirar(Incidente inc) {
        inc.setEstado(EstadoIncidente.EXPIRADO);
        inc.setPuntos(-PENALIZACION_EXPIRADO);
        puntaje -= PENALIZACION_EXPIRADO;
        fallos++;
        triage.eliminar(inc.getClave());
        metricas.registrarExpirado(inc.getTipo());
        efectos.agregar(new Efecto(inc.getX(), inc.getY(), "-" + PENALIZACION_EXPIRADO, false));
        registrar("Expiró " + inc.etiqueta() + " sin atención · fallo " + fallos + "/" + dificultad.getFallosPermitidos(),
                Evento.Nivel.ALERTA);
        cerrar(inc);
    }

    private void cerrar(Incidente inc) {
        ListaEnlazada<Unidad> asignadas = new ListaEnlazada<>();
        asignadas.agregarTodos(inc.getAsignadas());
        inc.getAsignadas().limpiar();
        for (Unidad u : asignadas) enviarABase(u);

        if (inc.getTipo().cierraVia()) {
            Via via = inc.getUbicacion().via();
            grafo.abrirCalle(via);
            trafico.desbloquear(via);
            trafico.desbloquear(via.getOpuesta());
        }
        activos.eliminar(inc);
        cerrados.agregar(inc);
    }

    // =====================================================================
    // Despacho
    // =====================================================================

    /**
     * Envía una unidad a un incidente. Si iba hacia otro, se la quita (reasignación).
     *
     * @return null si se pudo; si no, el motivo para mostrarlo al operador
     */
    public String asignar(Unidad u, Incidente inc) {
        if (resultado != Resultado.EN_CURSO) return "El turno terminó";
        if (!inc.getEstado().pendiente()) return inc.etiqueta() + " ya no espera unidades";
        if (u.getEstado() == EstadoUnidad.EN_ESCENA) return u.getId() + " está trabajando en otra escena";
        if (u.getIncidente() == inc) return u.getId() + " ya va hacia " + inc.etiqueta();
        if (!inc.necesita(u.getTipo())) return inc.etiqueta() + " no necesita más unidades de " + u.getTipo().getNombre();

        Plan plan = null;
        if (!u.isManual()) {
            plan = planificar(u, origen(u), Extremo.en(inc.getUbicacion()));
            if (plan == null) return "No hay ruta disponible para " + u.getId();
        }

        Incidente anterior = u.getIncidente();
        if (anterior != null) {
            anterior.getAsignadas().eliminar(u);
            if (anterior.getAsignadas().estaVacia()) anterior.setEstado(EstadoIncidente.REPORTADO);
            metricas.registrarReasignacion();
            registrar(u.getId() + " reasignada de " + anterior.etiqueta() + " a " + inc.etiqueta(), Evento.Nivel.INFO);
        }

        if (plan != null) u.seguir(plan, grafo.getVersionCierres());
        u.setIncidente(inc);
        u.setEstado(EstadoUnidad.EN_CAMINO);
        u.contarDespacho();
        inc.getAsignadas().agregar(u);
        inc.setEstado(EstadoIncidente.EN_CAMINO);
        if (u.isManual()) recalcularGps();
        registrar(u.getId() + " despachada a " + inc.etiqueta()
                + (plan != null ? " · ETA " + Math.round(plan.segundosEstimados()) + " s" : " · conducción manual"), Evento.Nivel.INFO);
        return null;
    }

    /** Mejor unidad disponible para cada tipo que le falta al incidente, según la estrategia. */
    public ListaEnlazada<Recomendacion> recomendar(Incidente inc) {
        ListaEnlazada<Recomendacion> resultado = new ListaEnlazada<>();
        if (!inc.getEstado().pendiente()) return resultado;
        ListaEnlazada<Unidad> elegidas = new ListaEnlazada<>();
        Extremo destino = Extremo.en(inc.getUbicacion());

        for (TipoUnidad tipo : inc.faltantes()) {
            Unidad mejor = null;
            Plan mejorPlan = null;
            double mejorClave = Double.POSITIVE_INFINITY;
            for (Unidad u : unidades) {
                if (u.getTipo() != tipo || !u.getEstado().disponible() || u.isManual() || elegidas.contiene(u)) continue;
                if (estrategia == Estrategia.DINAMICA) {
                    Plan plan = planificar(u, origen(u), destino);
                    if (plan != null && plan.segundosEstimados() < mejorClave) {
                        mejorClave = plan.segundosEstimados();
                        mejor = u;
                        mejorPlan = plan;
                    }
                } else {
                    double distancia = Math.hypot(u.getX() - inc.getX(), u.getY() - inc.getY());
                    if (distancia < mejorClave) {
                        mejorClave = distancia;
                        mejor = u;
                    }
                }
            }
            if (mejor != null && mejorPlan == null) mejorPlan = planificar(mejor, origen(mejor), destino);
            if (mejor != null && mejorPlan != null) {
                elegidas.agregar(mejor);
                resultado.agregar(new Recomendacion(mejor, mejorPlan));
            }
        }
        return resultado;
    }

    /** Cancela el despacho de una unidad en camino y la manda de vuelta a su base. */
    public String ordenarRegreso(Unidad u) {
        if (u.getEstado() != EstadoUnidad.EN_CAMINO) return u.getId() + " no va hacia ningún incidente";
        Incidente inc = u.getIncidente();
        inc.getAsignadas().eliminar(u);
        if (inc.getAsignadas().estaVacia()) inc.setEstado(EstadoIncidente.REPORTADO);
        registrar(u.getId() + " cancela su ruta a " + inc.etiqueta() + " y regresa", Evento.Nivel.INFO);
        enviarABase(u);
        return null;
    }

    /** Nodos que expanden A* y Dijkstra para la misma ruta: muestra cuánto trabajo ahorra la heurística. */
    public record ComparacionBusqueda(int nodosAEstrella, int nodosDijkstra) {
    }

    public ComparacionBusqueda compararBusqueda(Unidad u, Incidente inc) {
        FuncionCosto costo = estrategia.costo(u.getTipo());
        Extremo desde = origen(u);
        Extremo hacia = Extremo.en(inc.getUbicacion());
        Plan aEstrella = Navegador.planificar(grafo, desde, hacia, costo, 1.0 / u.getTipo().getVelocidad());
        Plan dijkstra = Navegador.planificar(grafo, desde, hacia, costo, 0);
        return new ComparacionBusqueda(aEstrella == null ? 0 : aEstrella.nodosExpandidos(),
                dijkstra == null ? 0 : dijkstra.nodosExpandidos());
    }

    /** Asigna todas las unidades recomendadas. Devuelve cuántas salieron. */
    public int asignarRecomendadas(Incidente inc) {
        int enviadas = 0;
        for (Recomendacion r : recomendar(inc)) {
            if (asignar(r.unidad(), inc) == null) enviadas++;
        }
        return enviadas;
    }

    /** Recorre la cola de triage del más urgente al menos urgente y cubre lo que pueda. */
    private void despacharAutomatico() {
        for (Incidente inc : triage.enOrden()) asignarRecomendadas(inc);
    }

    // =====================================================================
    // Unidades
    // =====================================================================

    private void actualizarUnidad(Unidad u, double dt) {
        if (u.isManual()) {
            moverManual(u, dt);
            return;
        }
        if (u.getEstado() != EstadoUnidad.EN_CAMINO && u.getEstado() != EstadoUnidad.REGRESANDO) return;

        u.sumarTiempoPlan(dt);
        boolean cambioCierres = u.getVersionCierresPlan() != grafo.getVersionCierres();
        if (cambioCierres || u.getSegundosDesdePlan() >= estrategia.intervaloReplanificacion()) replanificar(u, cambioCierres);

        PuntoVia p = u.puntoActual();
        if (p == null) return;
        boolean llego = u.avanzar(u.getTipo().getVelocidad() * p.via().factorSirena() * dt);
        if (u.getEstado() == EstadoUnidad.EN_CAMINO) {
            PuntoVia ahora = u.puntoActual();
            if (ahora != null) trafico.notificarSirena(ahora.via(), ahora.s());
        }
        if (llego) {
            if (u.getEstado() == EstadoUnidad.EN_CAMINO) llegar(u);
            else {
                u.setEstado(EstadoUnidad.EN_BASE);
                u.ubicarEnBase();
            }
        }
    }

    /**
     * Recalcula la ruta desde la posición actual. Si la ruta vigente no pasa por
     * una calle cerrada, solo se cambia cuando la nueva ahorra un margen claro:
     * sin esa histéresis las unidades cambian de idea con cada fluctuación del tráfico.
     */
    private void replanificar(Unidad u, boolean porCierre) {
        PuntoVia p = u.puntoActual();
        if (p == null) return;
        Extremo destino = u.getEstado() == EstadoUnidad.EN_CAMINO && u.getIncidente() != null
                ? Extremo.en(u.getIncidente().getUbicacion())
                : Extremo.en(u.getEstacion().getNodo());
        Plan plan = planificar(u, Extremo.en(p), destino);
        if (plan == null) return;

        ListaEnlazada<Tramo> vigente = u.recorridoRestante();
        boolean bloqueada = pasaPorCierre(vigente);
        if (!bloqueada) {
            double costoVigente = costoActual(u, vigente);
            double margen = Math.max(MARGEN_MINIMO_DESVIO, costoVigente * FRACCION_MINIMA_DESVIO);
            if (plan.segundosEstimados() > costoVigente - margen) {
                u.seguir(new Plan(vigente, costoVigente, 0), grafo.getVersionCierres());
                return;
            }
        }
        u.seguir(plan, grafo.getVersionCierres());
        metricas.registrarReplanificacion();
        if (porCierre && bloqueada) registrar(u.getId() + " tomó un desvío por un cierre de vía", Evento.Nivel.INFO);
    }

    /** La ruta atraviesa una calle cerrada (el último tramo puede estar en la calle del incidente). */
    private static boolean pasaPorCierre(ListaEnlazada<Tramo> tramos) {
        int i = 0;
        for (Tramo t : tramos) {
            boolean ultimo = ++i == tramos.tamano();
            if (!ultimo && t.via().estaCerrada() && t.longitud() > 0) return true;
        }
        return false;
    }

    private double costoActual(Unidad u, ListaEnlazada<Tramo> tramos) {
        FuncionCosto costo = estrategia.costo(u.getTipo());
        double total = 0;
        for (Tramo t : tramos) total += t.longitud() * costo.segundosPorPixel(t.via());
        return total;
    }

    private void llegar(Unidad u) {
        u.setEstado(EstadoUnidad.EN_ESCENA);
        registrar(u.getId() + " llegó a " + u.getIncidente().etiqueta(), Evento.Nivel.INFO);
    }

    private void enviarABase(Unidad u) {
        u.setIncidente(null);
        u.setEstado(EstadoUnidad.REGRESANDO);
        if (u.isManual()) {
            recalcularGps();
            return;
        }
        PuntoVia p = u.puntoActual();
        Plan plan = p == null ? null : planificar(u, Extremo.en(p), Extremo.en(u.getEstacion().getNodo()));
        if (plan == null || plan.vacio()) {
            u.setEstado(EstadoUnidad.EN_BASE);
            u.ubicarEnBase();
            return;
        }
        u.seguir(plan, grafo.getVersionCierres());
    }

    private Extremo origen(Unidad u) {
        PuntoVia p = u.puntoActual();
        return u.getEstado() == EstadoUnidad.EN_BASE || p == null ? Extremo.en(u.getEstacion().getNodo()) : Extremo.en(p);
    }

    private Plan planificar(Unidad u, Extremo desde, Extremo hacia) {
        FuncionCosto costo = estrategia.costo(u.getTipo());
        return Navegador.planificar(grafo, desde, hacia, costo, 1.0 / u.getTipo().getVelocidad());
    }

    // =====================================================================
    // Conducción manual
    // =====================================================================

    /** El operador toma el volante de una unidad. */
    public String tomarControl(Unidad u) {
        if (resultado != Resultado.EN_CURSO) return "El turno terminó";
        if (u.getEstado() == EstadoUnidad.EN_ESCENA) return u.getId() + " está trabajando en una escena";
        if (unidadManual != null) soltarControl();
        if (u.getEstado() == EstadoUnidad.EN_BASE) u.setEstado(EstadoUnidad.REGRESANDO);
        u.detener();
        u.setManual(true);
        unidadManual = u;
        manualSalioDeBase = false;
        entradaX = entradaY = 0;
        recalcularGps();
        registrar("Conduciendo " + u.getId(), Evento.Nivel.INFO);
        return null;
    }

    /** Devuelve la unidad al piloto automático desde el punto de la red más cercano. */
    public void soltarControl() {
        Unidad u = unidadManual;
        if (u == null) return;
        unidadManual = null;
        gps = null;
        u.setManual(false);
        u.ubicarEn(ciudad.proyectar(u.getX(), u.getY(), u.getAngulo()), grafo.getVersionCierres());
        if (u.getEstado() == EstadoUnidad.EN_CAMINO) replanificar(u, false);
        else if (u.getEstado() == EstadoUnidad.REGRESANDO) enviarABase(u);
    }

    /** Dirección deseada (-1..1 en cada eje) leída del teclado. */
    public void setDireccionManual(double dx, double dy) {
        entradaX = dx;
        entradaY = dy;
    }

    private void moverManual(Unidad u, double dt) {
        double largo = Math.hypot(entradaX, entradaY);
        if (largo > 0) {
            double vx = entradaX / largo;
            double vy = entradaY / largo;
            double velocidad = u.getTipo().getVelocidad() * FACTOR_VELOCIDAD_MANUAL;
            for (VehiculoCivil auto : trafico.getAutos()) {
                if (Math.hypot(auto.getX() - u.getX(), auto.getY() - u.getY()) < 13) {
                    velocidad *= 0.35; // roce con un auto: se frena
                    break;
                }
            }
            double nx = u.getX() + vx * velocidad * dt;
            double ny = u.getY() + vy * velocidad * dt;
            double angulo = Math.atan2(vy, vx);
            if (ciudad.enAsfalto(nx, ny, MARGEN_ASFALTO)) u.moverLibre(nx, ny, angulo);
            else if (ciudad.enAsfalto(nx, u.getY(), MARGEN_ASFALTO)) u.moverLibre(nx, u.getY(), angulo);
            else if (ciudad.enAsfalto(u.getX(), ny, MARGEN_ASFALTO)) u.moverLibre(u.getX(), ny, angulo);
        }

        Incidente inc = u.getIncidente();
        if (u.getEstado() == EstadoUnidad.EN_CAMINO && inc != null
                && Math.hypot(inc.getX() - u.getX(), inc.getY() - u.getY()) < RADIO_LLEGADA_MANUAL) {
            unidadManual = null;
            gps = null;
            u.setManual(false);
            u.ubicarEn(inc.getUbicacion(), grafo.getVersionCierres());
            llegar(u);
            return;
        }

        double aBase = Math.hypot(u.getEstacion().getNodo().getX() - u.getX(), u.getEstacion().getNodo().getY() - u.getY());
        if (aBase > 40) manualSalioDeBase = true;
        if (u.getEstado() == EstadoUnidad.REGRESANDO && manualSalioDeBase && aBase < 18) {
            unidadManual = null;
            gps = null;
            u.setManual(false);
            u.setEstado(EstadoUnidad.EN_BASE);
            u.ubicarEnBase();
            registrar(u.getId() + " volvió a la base", Evento.Nivel.INFO);
        }
    }

    private void recalcularGps() {
        Unidad u = unidadManual;
        if (u == null) {
            gps = null;
            return;
        }
        Extremo destino = u.getIncidente() != null
                ? Extremo.en(u.getIncidente().getUbicacion())
                : Extremo.en(u.getEstacion().getNodo());
        PuntoVia aqui = ciudad.proyectar(u.getX(), u.getY(), u.getAngulo());
        gps = planificar(u, Extremo.en(aqui), destino);
    }

    // =====================================================================
    // Utilidades y consultas
    // =====================================================================

    private void registrar(String texto, Evento.Nivel nivel) {
        bitacora.agregar(new Evento(tiempo, texto, nivel));
        if (bitacora.tamano() > MAX_BITACORA) bitacora.eliminarPrimero();
    }

    /** Fracción del turno transcurrida (0 a 1). */
    public double progreso() {
        return Math.min(1, tiempo / dificultad.getDuracionTurno());
    }

    public void setDespachoAutomatico(boolean activo) {
        despachoAutomatico = activo;
    }

    public boolean isDespachoAutomatico() {
        return despachoAutomatico;
    }

    /** Con false no aparecen incidentes aleatorios; solo los que se registren con {@link #reportar}. */
    public void setGeneracionAutomatica(boolean activa) {
        generacionAutomatica = activa;
    }

    /** El turno corre completo aunque se superen los fallos permitidos (benchmark y fondo del menú). */
    public void setIgnorarFallos(boolean ignorar) {
        ignorarFallos = ignorar;
    }

    /** Incidentes que esperan unidades, del más urgente al menos urgente (recorrido en orden del AVL). */
    public ListaEnlazada<Incidente> colaTriage() {
        return triage.enOrden();
    }

    public Incidente incidenteMasUrgente() {
        return triage.estaVacio() ? null : triage.minimo();
    }

    public double tiempoRestante() {
        return Math.max(0, dificultad.getDuracionTurno() - tiempo);
    }

    public Dificultad getDificultad() {
        return dificultad;
    }

    public long getSemilla() {
        return semilla;
    }

    public Estrategia getEstrategia() {
        return estrategia;
    }

    public Ciudad getCiudad() {
        return ciudad;
    }

    public Trafico getTrafico() {
        return trafico;
    }

    public Metricas getMetricas() {
        return metricas;
    }

    public ListaEnlazada<Unidad> getUnidades() {
        return unidades;
    }

    public ListaEnlazada<Incidente> getActivos() {
        return activos;
    }

    public ListaEnlazada<Incidente> getCerrados() {
        return cerrados;
    }

    public ListaEnlazada<Evento> getBitacora() {
        return bitacora;
    }

    public ListaEnlazada<Efecto> getEfectos() {
        return efectos;
    }

    public double getTiempo() {
        return tiempo;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public int getFallos() {
        return fallos;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public boolean terminada() {
        return resultado != Resultado.EN_CURSO;
    }

    public Unidad getUnidadManual() {
        return unidadManual;
    }

    /** Ruta sugerida a la unidad que se está manejando, o null. */
    public Plan getGps() {
        return gps;
    }
}
