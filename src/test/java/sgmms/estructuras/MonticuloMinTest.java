package sgmms.estructuras;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MonticuloMinTest {

    @Test
    void extraeSiempreElMinimo() {
        MonticuloMin<Integer> m = new MonticuloMin<>(Integer::compare);
        for (int v : new int[]{5, 3, 8, 1, 9, 2, 7}) m.insertar(v);
        assertEquals(1, m.verMinimo());
        int anterior = Integer.MIN_VALUE;
        while (!m.estaVacio()) {
            int actual = m.extraerMinimo();
            assertTrue(actual >= anterior);
            anterior = actual;
        }
    }

    @Test
    void heapsortOrdenaIgualQueLaBibliotecaEstandar() {
        Random rng = new Random(3);
        List<Integer> referencia = new ArrayList<>();
        ListaEnlazada<Integer> datos = new ListaEnlazada<>();
        for (int i = 0; i < 2000; i++) {
            int v = rng.nextInt(500) - 250;
            referencia.add(v);
            datos.agregar(v);
        }
        Collections.sort(referencia);
        ListaEnlazada<Integer> ordenados = MonticuloMin.ordenar(datos, Integer::compare);
        int i = 0;
        for (int v : ordenados) assertEquals(referencia.get(i++), v);
    }

    @Test
    void vacioLanzaExcepcion() {
        MonticuloMin<String> m = new MonticuloMin<>(String::compareTo);
        assertThrows(NoSuchElementException.class, m::extraerMinimo);
        assertThrows(NoSuchElementException.class, m::verMinimo);
    }
}
