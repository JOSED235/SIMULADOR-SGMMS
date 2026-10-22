package sgmms.simulacion;

import org.junit.jupiter.api.Test;
import sgmms.grafo.Grafo;
import sgmms.grafo.Via;
import sgmms.modelo.VehiculoCivil;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TraficoTest {

    private final Ciudad ciudad = new Ciudad();
    private final Grafo grafo = ciudad.getGrafo();

    private Trafico trafico(int autos) {
        Trafico t = new Trafico(grafo, ciudad.getSalidas(), new Random(4));
        t.poblar(autos);
        return t;
    }

    private void avanzar(Trafico t, double segundos) {
        for (double s = 0; s < segundos; s += 1.0 / 60) t.actualizar(1.0 / 60);
    }

    @Test
    void laOcupacionDeLasViasCoincideConLosAutos() {
        Trafico t = trafico(80);
        avanzar(t, 40);
        int suma = 0;
        for (Via v : grafo.vias()) {
            suma += v.getOcupacion();
            assertEquals(t.autosEn(v).tamano(), v.getOcupacion(), "ocupación de " + v);
        }
        assertEquals(t.getAutos().tamano(), suma);
    }

    @Test
    void losAutosNoSeAdelantanEnElMismoCarril() {
        Trafico t = trafico(90);
        for (int paso = 0; paso < 60 * 30; paso++) {
            t.actualizar(1.0 / 60);
            if (paso % 30 != 0) continue;
            for (Via v : grafo.vias()) {
                for (VehiculoCivil a : t.autosEn(v)) {
                    for (VehiculoCivil b : t.autosEn(v)) {
                        if (a == b) continue;
                        double separacion = Math.abs(a.getS() - b.getS());
                        // Solo se permite el solape breve de quien entra forzado a una esquina trancada
                        if (a.getS() > 1 && b.getS() > 1) assertTrue(separacion > 1, "autos encimados en " + v);
                    }
                }
            }
        }
    }

    @Test
    void ningunAutoCruzaUnBloqueoDeIncidente() {
        Trafico t = trafico(80);
        Via v = grafo.via("C1", "C2");
        double bloqueo = v.getLongitud() / 2;
        grafo.cerrarCalle(v);
        t.bloquear(v, bloqueo);
        t.bloquear(v.getOpuesta(), v.getLongitud() - bloqueo);

        // Los que ya estaban antes del bloqueo nunca lo pasan
        java.util.IdentityHashMap<VehiculoCivil, Boolean> antes = new java.util.IdentityHashMap<>();
        for (VehiculoCivil a : t.autosEn(v)) if (a.getS() < bloqueo) antes.put(a, true);
        for (int paso = 0; paso < 60 * 20; paso++) {
            t.actualizar(1.0 / 60);
            for (VehiculoCivil a : t.autosEn(v)) {
                if (antes.containsKey(a)) assertTrue(a.getS() < bloqueo, "un auto atravesó el accidente");
            }
        }
        // Y ninguno nuevo entra a la calle cerrada
        for (VehiculoCivil a : t.autosEn(v)) assertTrue(antes.containsKey(a));
    }

    @Test
    void mantieneElVolumenObjetivo() {
        Trafico t = trafico(60);
        t.setObjetivo(60);
        avanzar(t, 90);
        assertTrue(t.getAutos().tamano() > 40, "quedaron " + t.getAutos().tamano());
        assertTrue(t.getAutos().tamano() <= 60);
    }
}
