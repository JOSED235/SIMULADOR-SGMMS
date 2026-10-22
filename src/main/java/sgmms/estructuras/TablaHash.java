package sgmms.estructuras;

/**
 * Tabla hash con encadenamiento separado y redimensionamiento automático
 * cuando el factor de carga supera 0.75. Las búsquedas de rutas la usan para
 * guardar costos, predecesores y visitados en O(1) promedio.
 */
public class TablaHash<K, V> {

    private static final double FACTOR_CARGA_MAXIMO = 0.75;

    private static final class Entrada<K, V> {
        final K clave;
        V valor;
        Entrada<K, V> siguiente;

        Entrada(K clave, V valor, Entrada<K, V> siguiente) {
            this.clave = clave;
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }

    private Entrada<K, V>[] cubetas;
    private int tamano;

    public TablaHash() {
        this(16);
    }

    @SuppressWarnings("unchecked")
    public TablaHash(int capacidadInicial) {
        int capacidad = 1;
        while (capacidad < Math.max(4, capacidadInicial)) capacidad <<= 1;
        cubetas = (Entrada<K, V>[]) new Entrada[capacidad];
    }

    /** Inserta o reemplaza. Devuelve el valor anterior o null. */
    public V poner(K clave, V valor) {
        if (clave == null) throw new IllegalArgumentException("La clave no puede ser null");
        int i = indice(clave, cubetas.length);
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) {
                V anterior = e.valor;
                e.valor = valor;
                return anterior;
            }
        }
        cubetas[i] = new Entrada<>(clave, valor, cubetas[i]);
        tamano++;
        if (tamano > cubetas.length * FACTOR_CARGA_MAXIMO) redimensionar();
        return null;
    }

    public V obtener(K clave) {
        Entrada<K, V> e = buscar(clave);
        return e == null ? null : e.valor;
    }

    public V obtenerOPorDefecto(K clave, V porDefecto) {
        Entrada<K, V> e = buscar(clave);
        return e == null ? porDefecto : e.valor;
    }

    public boolean contieneClave(K clave) {
        return buscar(clave) != null;
    }

    public V eliminar(K clave) {
        int i = indice(clave, cubetas.length);
        Entrada<K, V> anterior = null;
        for (Entrada<K, V> e = cubetas[i]; e != null; anterior = e, e = e.siguiente) {
            if (e.clave.equals(clave)) {
                if (anterior == null) cubetas[i] = e.siguiente;
                else anterior.siguiente = e.siguiente;
                tamano--;
                return e.valor;
            }
        }
        return null;
    }

    public ListaEnlazada<K> claves() {
        ListaEnlazada<K> claves = new ListaEnlazada<>();
        for (Entrada<K, V> cubeta : cubetas) {
            for (Entrada<K, V> e = cubeta; e != null; e = e.siguiente) claves.agregar(e.clave);
        }
        return claves;
    }

    public ListaEnlazada<V> valores() {
        ListaEnlazada<V> valores = new ListaEnlazada<>();
        for (Entrada<K, V> cubeta : cubetas) {
            for (Entrada<K, V> e = cubeta; e != null; e = e.siguiente) valores.agregar(e.valor);
        }
        return valores;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    int capacidad() {
        return cubetas.length;
    }

    private Entrada<K, V> buscar(K clave) {
        if (clave == null) return null;
        for (Entrada<K, V> e = cubetas[indice(clave, cubetas.length)]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) return e;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void redimensionar() {
        Entrada<K, V>[] nuevas = (Entrada<K, V>[]) new Entrada[cubetas.length * 2];
        for (Entrada<K, V> cubeta : cubetas) {
            Entrada<K, V> e = cubeta;
            while (e != null) {
                Entrada<K, V> siguiente = e.siguiente;
                int i = indice(e.clave, nuevas.length);
                e.siguiente = nuevas[i];
                nuevas[i] = e;
                e = siguiente;
            }
        }
        cubetas = nuevas;
    }

    private static int indice(Object clave, int capacidad) {
        int h = clave.hashCode();
        h ^= (h >>> 16); // mezcla los bits altos, la capacidad es potencia de 2
        return h & (capacidad - 1);
    }
}
