package sgmms.estructuras;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ListaEnlazadaTest {

    @Test
    void agregaAlFinalYAlInicioManteniendoElOrden() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(2);
        lista.agregar(3);
        lista.agregarAlInicio(1);
        assertEquals(3, lista.tamano());
        assertEquals("[1, 2, 3]", lista.toString());
        assertEquals(1, lista.primero());
        assertEquals(3, lista.ultimo());
        assertEquals(2, lista.obtener(1));
    }

    @Test
    void obtenerFueraDeRangoLanzaExcepcion() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("a");
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(1));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
    }

    @Test
    void eliminaCabezaColaYMedio() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        for (int i = 1; i <= 5; i++) lista.agregar(i);
        assertTrue(lista.eliminar(1));
        assertTrue(lista.eliminar(5));
        assertTrue(lista.eliminar(3));
        assertFalse(lista.eliminar(99));
        assertEquals("[2, 4]", lista.toString());
        assertEquals(2, lista.eliminarPrimero());
        assertEquals(4, lista.ultimo());
    }

    @Test
    void eliminarSiQuitaTodosLosQueCumplen() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        for (int i = 0; i < 10; i++) lista.agregar(i);
        assertEquals(5, lista.eliminarSi(n -> n % 2 == 0));
        assertEquals("[1, 3, 5, 7, 9]", lista.toString());
        assertEquals(2, lista.contar(n -> n > 5));
    }

    @Test
    void elIteradorPermiteEliminarDuranteElRecorrido() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("a");
        lista.agregar("b");
        lista.agregar("c");
        Iterator<String> it = lista.iterator();
        while (it.hasNext()) {
            if (it.next().equals("b")) it.remove();
        }
        assertEquals("[a, c]", lista.toString());
        assertThrows(NoSuchElementException.class, () -> {
            Iterator<String> fin = lista.iterator();
            fin.next();
            fin.next();
            fin.next();
        });
    }

    @Test
    void operacionesSobreListaVacia() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        assertTrue(lista.estaVacia());
        assertThrows(NoSuchElementException.class, lista::primero);
        assertThrows(NoSuchElementException.class, lista::eliminarPrimero);
        lista.agregar("x");
        lista.limpiar();
        assertTrue(lista.estaVacia());
        assertFalse(lista.iterator().hasNext());
    }
}
