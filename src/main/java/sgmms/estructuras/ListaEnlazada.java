package sgmms.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Lista doblemente enlazada genérica. Es la colección base de todo el proyecto:
 * flota, tráfico, incidentes, rutas y bitácora se guardan aquí.
 */
public class ListaEnlazada<T> implements Iterable<T> {

    private static final class Nodo<T> {
        T valor;
        Nodo<T> anterior;
        Nodo<T> siguiente;

        Nodo(T valor) {
            this.valor = valor;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public void agregar(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);
        if (cola == null) {
            cabeza = cola = nuevo;
        } else {
            nuevo.anterior = cola;
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamano++;
    }

    public void agregarAlInicio(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);
        if (cabeza == null) {
            cabeza = cola = nuevo;
        } else {
            nuevo.siguiente = cabeza;
            cabeza.anterior = nuevo;
            cabeza = nuevo;
        }
        tamano++;
    }

    public void agregarTodos(ListaEnlazada<? extends T> otra) {
        for (T valor : otra) {
            agregar(valor);
        }
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de [0, " + tamano + ")");
        }
        Nodo<T> actual;
        if (indice < tamano / 2) {
            actual = cabeza;
            for (int i = 0; i < indice; i++) actual = actual.siguiente;
        } else {
            actual = cola;
            for (int i = tamano - 1; i > indice; i--) actual = actual.anterior;
        }
        return actual.valor;
    }

    public T primero() {
        if (cabeza == null) throw new NoSuchElementException("Lista vacía");
        return cabeza.valor;
    }

    public T ultimo() {
        if (cola == null) throw new NoSuchElementException("Lista vacía");
        return cola.valor;
    }

    public T eliminarPrimero() {
        if (cabeza == null) throw new NoSuchElementException("Lista vacía");
        T valor = cabeza.valor;
        desenlazar(cabeza);
        return valor;
    }

    /** Elimina la primera aparición del valor (por equals). */
    public boolean eliminar(T valor) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (iguales(n.valor, valor)) {
                desenlazar(n);
                return true;
            }
        }
        return false;
    }

    /** Elimina todos los elementos que cumplan la condición y devuelve cuántos quitó. */
    public int eliminarSi(Predicate<? super T> condicion) {
        int eliminados = 0;
        Nodo<T> n = cabeza;
        while (n != null) {
            Nodo<T> siguiente = n.siguiente;
            if (condicion.test(n.valor)) {
                desenlazar(n);
                eliminados++;
            }
            n = siguiente;
        }
        return eliminados;
    }

    public boolean contiene(T valor) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (iguales(n.valor, valor)) return true;
        }
        return false;
    }

    public int contar(Predicate<? super T> condicion) {
        int total = 0;
        for (T valor : this) {
            if (condicion.test(valor)) total++;
        }
        return total;
    }

    public void limpiar() {
        cabeza = cola = null;
        tamano = 0;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    private void desenlazar(Nodo<T> n) {
        if (n.anterior == null) cabeza = n.siguiente;
        else n.anterior.siguiente = n.siguiente;
        if (n.siguiente == null) cola = n.anterior;
        else n.siguiente.anterior = n.anterior;
        n.anterior = n.siguiente = null;
        tamano--;
    }

    private static boolean iguales(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> siguiente = cabeza;
            private Nodo<T> ultimoDevuelto;

            @Override
            public boolean hasNext() {
                return siguiente != null;
            }

            @Override
            public T next() {
                if (siguiente == null) throw new NoSuchElementException();
                ultimoDevuelto = siguiente;
                siguiente = siguiente.siguiente;
                return ultimoDevuelto.valor;
            }

            @Override
            public void remove() {
                if (ultimoDevuelto == null) throw new IllegalStateException();
                desenlazar(ultimoDevuelto);
                ultimoDevuelto = null;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            sb.append(n.valor);
            if (n.siguiente != null) sb.append(", ");
        }
        return sb.append(']').toString();
    }
}
