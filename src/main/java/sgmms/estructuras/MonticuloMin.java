package sgmms.estructuras;

import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Montículo binario de mínimos sobre un arreglo. Es la frontera de A* y Dijkstra
 * y también sirve para ordenar (heapsort) los tiempos de respuesta al calcular percentiles.
 */
public class MonticuloMin<T> {

    private Object[] datos = new Object[16];
    private int tamano;
    private final Comparator<? super T> comparador;

    public MonticuloMin(Comparator<? super T> comparador) {
        this.comparador = comparador;
    }

    public void insertar(T valor) {
        if (tamano == datos.length) {
            Object[] nuevo = new Object[datos.length * 2];
            System.arraycopy(datos, 0, nuevo, 0, tamano);
            datos = nuevo;
        }
        datos[tamano] = valor;
        subir(tamano++);
    }

    public T verMinimo() {
        if (tamano == 0) throw new NoSuchElementException("Montículo vacío");
        return elemento(0);
    }

    public T extraerMinimo() {
        if (tamano == 0) throw new NoSuchElementException("Montículo vacío");
        T minimo = elemento(0);
        datos[0] = datos[--tamano];
        datos[tamano] = null;
        if (tamano > 0) bajar(0);
        return minimo;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (comparador.compare(elemento(i), elemento(padre)) >= 0) break;
            intercambiar(i, padre);
            i = padre;
        }
    }

    private void bajar(int i) {
        while (true) {
            int izq = 2 * i + 1;
            int der = izq + 1;
            int menor = i;
            if (izq < tamano && comparador.compare(elemento(izq), elemento(menor)) < 0) menor = izq;
            if (der < tamano && comparador.compare(elemento(der), elemento(menor)) < 0) menor = der;
            if (menor == i) return;
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int a, int b) {
        Object tmp = datos[a];
        datos[a] = datos[b];
        datos[b] = tmp;
    }

    @SuppressWarnings("unchecked")
    private T elemento(int i) {
        return (T) datos[i];
    }

    /** Heapsort: devuelve una lista nueva con los elementos ordenados de menor a mayor. */
    public static <T> ListaEnlazada<T> ordenar(Iterable<T> elementos, Comparator<? super T> comparador) {
        MonticuloMin<T> monticulo = new MonticuloMin<>(comparador);
        for (T e : elementos) monticulo.insertar(e);
        ListaEnlazada<T> ordenada = new ListaEnlazada<>();
        while (!monticulo.estaVacio()) ordenada.agregar(monticulo.extraerMinimo());
        return ordenada;
    }
}
