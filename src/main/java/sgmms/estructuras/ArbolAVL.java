package sgmms.estructuras;

import java.util.NoSuchElementException;

/**
 * Árbol AVL (BST autobalanceado). La cola de triage guarda los incidentes
 * pendientes ordenados por su plazo límite: el mínimo es el más urgente y el
 * recorrido en orden es la lista que ve el operador.
 */
public class ArbolAVL<K extends Comparable<K>, V> {

    private static final class Nodo<K, V> {
        final K clave;
        V valor;
        Nodo<K, V> izquierdo;
        Nodo<K, V> derecho;
        int altura = 1;

        Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private Nodo<K, V> raiz;
    private int tamano;

    public void insertar(K clave, V valor) {
        raiz = insertar(raiz, clave, valor);
    }

    public boolean eliminar(K clave) {
        int antes = tamano;
        raiz = eliminar(raiz, clave);
        return tamano < antes;
    }

    public V obtener(K clave) {
        Nodo<K, V> n = raiz;
        while (n != null) {
            int c = clave.compareTo(n.clave);
            if (c == 0) return n.valor;
            n = c < 0 ? n.izquierdo : n.derecho;
        }
        return null;
    }

    public boolean contiene(K clave) {
        return obtener(clave) != null;
    }

    public V minimo() {
        if (raiz == null) throw new NoSuchElementException("Árbol vacío");
        Nodo<K, V> n = raiz;
        while (n.izquierdo != null) n = n.izquierdo;
        return n.valor;
    }

    public ListaEnlazada<V> enOrden() {
        ListaEnlazada<V> resultado = new ListaEnlazada<>();
        enOrden(raiz, resultado);
        return resultado;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return tamano == 0;
    }

    public int altura() {
        return altura(raiz);
    }

    public void limpiar() {
        raiz = null;
        tamano = 0;
    }

    private Nodo<K, V> insertar(Nodo<K, V> n, K clave, V valor) {
        if (n == null) {
            tamano++;
            return new Nodo<>(clave, valor);
        }
        int c = clave.compareTo(n.clave);
        if (c < 0) n.izquierdo = insertar(n.izquierdo, clave, valor);
        else if (c > 0) n.derecho = insertar(n.derecho, clave, valor);
        else {
            n.valor = valor;
            return n;
        }
        return balancear(n);
    }

    private Nodo<K, V> eliminar(Nodo<K, V> n, K clave) {
        if (n == null) return null;
        int c = clave.compareTo(n.clave);
        if (c < 0) n.izquierdo = eliminar(n.izquierdo, clave);
        else if (c > 0) n.derecho = eliminar(n.derecho, clave);
        else {
            if (n.izquierdo == null || n.derecho == null) {
                tamano--;
                return n.izquierdo != null ? n.izquierdo : n.derecho;
            }
            // Dos hijos: se reemplaza por el sucesor en orden
            Nodo<K, V> sucesor = n.derecho;
            while (sucesor.izquierdo != null) sucesor = sucesor.izquierdo;
            Nodo<K, V> reemplazo = new Nodo<>(sucesor.clave, sucesor.valor);
            reemplazo.izquierdo = n.izquierdo;
            reemplazo.derecho = eliminar(n.derecho, sucesor.clave);
            n = reemplazo;
        }
        return balancear(n);
    }

    private Nodo<K, V> balancear(Nodo<K, V> n) {
        actualizarAltura(n);
        int balance = balance(n);
        if (balance > 1) {
            if (balance(n.izquierdo) < 0) n.izquierdo = rotarIzquierda(n.izquierdo);
            return rotarDerecha(n);
        }
        if (balance < -1) {
            if (balance(n.derecho) > 0) n.derecho = rotarDerecha(n.derecho);
            return rotarIzquierda(n);
        }
        return n;
    }

    private Nodo<K, V> rotarDerecha(Nodo<K, V> y) {
        Nodo<K, V> x = y.izquierdo;
        y.izquierdo = x.derecho;
        x.derecho = y;
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    private Nodo<K, V> rotarIzquierda(Nodo<K, V> x) {
        Nodo<K, V> y = x.derecho;
        x.derecho = y.izquierdo;
        y.izquierdo = x;
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }

    private void actualizarAltura(Nodo<K, V> n) {
        n.altura = 1 + Math.max(altura(n.izquierdo), altura(n.derecho));
    }

    private int altura(Nodo<K, V> n) {
        return n == null ? 0 : n.altura;
    }

    private int balance(Nodo<K, V> n) {
        return altura(n.izquierdo) - altura(n.derecho);
    }

    private void enOrden(Nodo<K, V> n, ListaEnlazada<V> resultado) {
        if (n == null) return;
        enOrden(n.izquierdo, resultado);
        resultado.agregar(n.valor);
        enOrden(n.derecho, resultado);
    }
}
