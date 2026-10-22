package sgmms.simulacion;

import sgmms.grafo.BuscadorRutas;
import sgmms.grafo.FuncionCosto;
import sgmms.grafo.Grafo;
import sgmms.grafo.Nodo;
import sgmms.modelo.Dificultad;
import sgmms.modelo.TipoUnidad;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

/**
 * Corre turnos completos sin interfaz, con despacho automático, y compara las
 * dos estrategias sobre las mismas semillas (mismos incidentes y mismo tráfico inicial).
 *
 * <pre>java -cp target/classes sgmms.simulacion.Benchmark [turnos] [dificultad]</pre>
 */
public final class Benchmark {

    public record Resumen(Estrategia estrategia, int turnos, double respuestaPromedio, double p90,
                          double cumplimiento, double expiradosPorTurno, double puntaje, double replanificaciones) {
    }

    private Benchmark() {
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        int turnos = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        Dificultad dificultad = args.length > 1 ? Dificultad.valueOf(args[1].toUpperCase(Locale.ROOT)) : Dificultad.NORMAL;

        System.out.printf("Benchmark · %d turnos · dificultad %s · despacho automático%n%n", turnos, dificultad.getNombre());
        System.out.println("| Estrategia | Respuesta prom. | P90 | Cumplimiento | Expirados/turno | Puntaje prom. |");
        System.out.println("|---|---|---|---|---|---|");
        Resumen base = null;
        for (Estrategia e : new Estrategia[]{Estrategia.ESTATICA, Estrategia.DINAMICA}) {
            Resumen r = correr(e, dificultad, turnos);
            System.out.printf(Locale.ROOT, "| %s | %.1f s | %.1f s | %.1f %% | %.2f | %.0f |%n", e.getDescripcion(),
                    r.respuestaPromedio(), r.p90(), r.cumplimiento() * 100, r.expiradosPorTurno(), r.puntaje());
            if (base == null) base = r;
            else {
                System.out.printf(Locale.ROOT, "%nDiferencia en respuesta promedio (dinámica vs. estática): %+.1f %%.%n",
                        (r.respuestaPromedio() / base.respuestaPromedio() - 1) * 100);
            }
        }

        BusquedaComparada b = compararBusquedas(dificultad, 2000);
        System.out.printf(Locale.ROOT, "%nA* vs. Dijkstra en %d rutas con tráfico real a mitad de turno: %.1f vs. %.1f nodos expandidos "
                        + "en promedio (%.0f %% menos), mismo costo en el %.1f %% de los casos.%n",
                b.rutas(), b.nodosAEstrella(), b.nodosDijkstra(), (1 - b.nodosAEstrella() / b.nodosDijkstra()) * 100,
                b.mismoCosto() * 100);
    }

    public record BusquedaComparada(int rutas, double nodosAEstrella, double nodosDijkstra, double mismoCosto) {
    }

    /**
     * Toma la red a mitad de un turno (con su tráfico y cierres reales) y calcula
     * rutas entre pares de intersecciones al azar con ambos algoritmos.
     */
    public static BusquedaComparada compararBusquedas(Dificultad dificultad, int rutas) {
        Simulacion sim = new Simulacion(dificultad, 1);
        sim.setDespachoAutomatico(true);
        sim.setIgnorarFallos(true);
        while (sim.progreso() < 0.5) sim.avanzar(Simulacion.PASO);

        Grafo grafo = sim.getCiudad().getGrafo();
        double velocidad = TipoUnidad.PATRULLA.getVelocidad();
        FuncionCosto costo = FuncionCosto.conSirena(velocidad);
        Random rng = new Random(1);
        long expandidosA = 0;
        long expandidosD = 0;
        int iguales = 0;
        int n = grafo.nodos().tamano();
        for (int i = 0; i < rutas; i++) {
            Nodo origen = grafo.nodos().obtener(rng.nextInt(n));
            Nodo destino = grafo.nodos().obtener(rng.nextInt(n));
            BuscadorRutas.Resultado a = BuscadorRutas.aEstrella(grafo, origen, destino, costo, 1 / velocidad);
            BuscadorRutas.Resultado d = BuscadorRutas.dijkstra(grafo, origen, destino, costo);
            expandidosA += a.nodosExpandidos();
            expandidosD += d.nodosExpandidos();
            if (Math.abs(a.costo() - d.costo()) < 1e-9 || (!a.encontrada() && !d.encontrada())) iguales++;
        }
        return new BusquedaComparada(rutas, (double) expandidosA / rutas, (double) expandidosD / rutas, (double) iguales / rutas);
    }

    /** Corre {@code turnos} turnos con semillas 1..turnos y promedia los indicadores. */
    public static Resumen correr(Estrategia estrategia, Dificultad dificultad, int turnos) {
        double respuesta = 0;
        double p90 = 0;
        double cumplimiento = 0;
        double expirados = 0;
        double puntaje = 0;
        double replanificaciones = 0;
        for (int semilla = 1; semilla <= turnos; semilla++) {
            Simulacion sim = correrTurno(estrategia, dificultad, semilla);
            Metricas m = sim.getMetricas();
            respuesta += m.respuestaPromedio();
            p90 += m.percentilRespuesta(90);
            cumplimiento += m.cumplimiento();
            expirados += m.expirados();
            puntaje += sim.getPuntaje();
            replanificaciones += m.getReplanificaciones();
        }
        return new Resumen(estrategia, turnos, respuesta / turnos, p90 / turnos, cumplimiento / turnos,
                expirados / turnos, puntaje / turnos, replanificaciones / turnos);
    }

    /**
     * Un turno sin límite de fallos se juega completo, para que las dos estrategias
     * se midan durante el mismo tiempo.
     */
    public static Simulacion correrTurno(Estrategia estrategia, Dificultad dificultad, long semilla) {
        Simulacion sim = new Simulacion(dificultad, semilla, estrategia);
        sim.setDespachoAutomatico(true);
        sim.setIgnorarFallos(true);
        while (!sim.terminada()) sim.avanzar(Simulacion.PASO);
        return sim;
    }
}
