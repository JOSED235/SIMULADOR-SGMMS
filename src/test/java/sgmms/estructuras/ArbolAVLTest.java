package sgmms.estructuras;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class ArbolAVLTest {

    @Test
    void elRecorridoEnOrdenSaleOrdenado() {
        ArbolAVL<Integer, String> arbol = new ArbolAVL<>();
        for (int v : new int[]{50, 20, 70, 10, 30, 60, 80, 25}) arbol.insertar(v, "v" + v);
        assertEquals("[v10, v20, v25, v30, v50, v60, v70, v80]", arbol.enOrden().toString());
        assertEquals("v10", arbol.minimo());
        assertEquals(8, arbol.tamano());
    }

    /** Insertar en orden degeneraría un BST simple en una lista; el AVL debe quedar balanceado. */
    @Test
    void seMantieneBalanceadoConInsercionesOrdenadas() {
        ArbolAVL<Integer, Integer> arbol = new ArbolAVL<>();
        int n = 1023;
        for (int i = 0; i < n; i++) arbol.insertar(i, i);
        double limite = 1.45 * (Math.log(n + 2) / Math.log(2));
        assertTrue(arbol.altura() <= limite, "altura " + arbol.altura() + " supera " + limite);
    }

    @Test
    void eliminarHojasNodosConUnHijoYConDos() {
        ArbolAVL<Integer, Integer> arbol = new ArbolAVL<>();
        for (int v : new int[]{50, 20, 70, 10, 30, 60, 80}) arbol.insertar(v, v);
        assertTrue(arbol.eliminar(10));  // hoja
        assertTrue(arbol.eliminar(20));  // un hijo
        assertTrue(arbol.eliminar(50));  // dos hijos (raíz)
        assertFalse(arbol.eliminar(99));
        assertEquals("[30, 60, 70, 80]", arbol.enOrden().toString());
        assertEquals(30, arbol.minimo());
    }

    @Test
    void coincideConTreeMapEnOperacionesAleatoriasYSigueBalanceado() {
        ArbolAVL<Integer, Integer> arbol = new ArbolAVL<>();
        TreeMap<Integer, Integer> referencia = new TreeMap<>();
        Random rng = new Random(11);
        for (int i = 0; i < 4000; i++) {
            int clave = rng.nextInt(800);
            if (rng.nextInt(3) == 0) {
                assertEquals(referencia.remove(clave) != null, arbol.eliminar(clave));
            } else {
                referencia.put(clave, i);
                arbol.insertar(clave, i);
            }
        }
        assertEquals(referencia.size(), arbol.tamano());
        assertEquals(referencia.values().toString(), arbol.enOrden().toString());
        assertEquals(referencia.firstEntry().getValue(), arbol.minimo());
        assertTrue(arbol.altura() <= 1.45 * (Math.log(arbol.tamano() + 2) / Math.log(2)));
    }

    @Test
    void insertarClaveExistenteReemplazaElValor() {
        ArbolAVL<String, Integer> arbol = new ArbolAVL<>();
        arbol.insertar("a", 1);
        arbol.insertar("a", 2);
        assertEquals(1, arbol.tamano());
        assertEquals(2, arbol.obtener("a"));
    }
}
