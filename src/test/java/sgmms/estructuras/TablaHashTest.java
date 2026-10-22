package sgmms.estructuras;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TablaHashTest {

    @Test
    void ponerObtenerYReemplazar() {
        TablaHash<String, Integer> tabla = new TablaHash<>();
        assertNull(tabla.poner("a", 1));
        assertEquals(1, tabla.poner("a", 2));
        assertEquals(2, tabla.obtener("a"));
        assertEquals(1, tabla.tamano());
        assertNull(tabla.obtener("b"));
        assertEquals(7, tabla.obtenerOPorDefecto("b", 7));
    }

    @Test
    void creceAlSuperarElFactorDeCargaSinPerderDatos() {
        TablaHash<Integer, Integer> tabla = new TablaHash<>(4);
        int capacidadInicial = tabla.capacidad();
        for (int i = 0; i < 1000; i++) tabla.poner(i, i * i);
        assertTrue(tabla.capacidad() > capacidadInicial);
        assertEquals(1000, tabla.tamano());
        for (int i = 0; i < 1000; i++) assertEquals(i * i, tabla.obtener(i));
    }

    /** Claves con el mismo hashCode caen en la misma cubeta y deben convivir. */
    @Test
    void manejaColisiones() {
        record Clave(int id) {
            @Override
            public int hashCode() {
                return 42;
            }
        }
        TablaHash<Clave, String> tabla = new TablaHash<>();
        for (int i = 0; i < 20; i++) tabla.poner(new Clave(i), "v" + i);
        assertEquals(20, tabla.tamano());
        assertEquals("v13", tabla.obtener(new Clave(13)));
        assertEquals("v0", tabla.eliminar(new Clave(0)));
        assertEquals("v19", tabla.eliminar(new Clave(19)));
        assertFalse(tabla.contieneClave(new Clave(0)));
        assertEquals(18, tabla.tamano());
    }

    @Test
    void coincideConHashMapEnOperacionesAleatorias() {
        TablaHash<Integer, Integer> tabla = new TablaHash<>();
        Map<Integer, Integer> referencia = new HashMap<>();
        Random rng = new Random(7);
        for (int i = 0; i < 5000; i++) {
            int clave = rng.nextInt(300);
            if (rng.nextInt(3) == 0) assertEquals(referencia.remove(clave), tabla.eliminar(clave));
            else assertEquals(referencia.put(clave, i), tabla.poner(clave, i));
        }
        assertEquals(referencia.size(), tabla.tamano());
        assertEquals(referencia.size(), tabla.claves().tamano());
        for (Map.Entry<Integer, Integer> e : referencia.entrySet()) assertEquals(e.getValue(), tabla.obtener(e.getKey()));
    }

    @Test
    void rechazaClaveNula() {
        assertThrows(IllegalArgumentException.class, () -> new TablaHash<String, Integer>().poner(null, 1));
    }
}
