package sgmms.grafo;

import sgmms.estructuras.ListaEnlazada;
import sgmms.estructuras.TablaHash;

/**
 * Grafo dirigido y ponderado de la red vial. Cada calle se guarda como dos vías
 * (una por sentido) enlazadas entre sí, con lista de adyacencia en una tabla hash.
 */
public class Grafo {

    private final TablaHash<String, Nodo> nodos = new TablaHash<>();
    private final TablaHash<String, ListaEnlazada<Via>> salientes = new TablaHash<>();
    private final ListaEnlazada<Nodo> listaNodos = new ListaEnlazada<>();
    private final ListaEnlazada<Via> vias = new ListaEnlazada<>();
    private int versionCierres;

    public Nodo agregarNodo(String id, double x, double y, boolean salida) {
        if (nodos.contieneClave(id)) throw new IllegalArgumentException("Nodo repetido: " + id);
        Nodo nodo = new Nodo(id, x, y, salida);
        nodos.poner(id, nodo);
        salientes.poner(id, new ListaEnlazada<>());
        listaNodos.agregar(nodo);
        return nodo;
    }

    /** Agrega una calle de doble sentido y devuelve el carril a→b. */
    public Via agregarCalle(String a, String b) {
        Nodo na = exigir(a);
        Nodo nb = exigir(b);
        if (via(a, b) != null) throw new IllegalArgumentException("Calle repetida: " + a + "-" + b);
        Via ida = new Via(na, nb);
        Via vuelta = new Via(nb, na);
        ida.setOpuesta(vuelta);
        vuelta.setOpuesta(ida);
        salientes.obtener(a).agregar(ida);
        salientes.obtener(b).agregar(vuelta);
        vias.agregar(ida);
        vias.agregar(vuelta);
        return ida;
    }

    public Nodo nodo(String id) {
        return nodos.obtener(id);
    }

    public ListaEnlazada<Via> salientes(Nodo nodo) {
        return salientes.obtener(nodo.getId());
    }

    public Via via(String a, String b) {
        ListaEnlazada<Via> lista = salientes.obtener(a);
        if (lista == null) return null;
        for (Via v : lista) {
            if (v.getDestino().getId().equals(b)) return v;
        }
        return null;
    }

    public ListaEnlazada<Nodo> nodos() {
        return listaNodos;
    }

    public ListaEnlazada<Via> vias() {
        return vias;
    }

    /** Cierra la calle completa (ambos sentidos). Los cierres se acumulan. */
    public void cerrarCalle(Via via) {
        via.cerrar();
        via.getOpuesta().cerrar();
        versionCierres++;
    }

    public void abrirCalle(Via via) {
        via.abrir();
        via.getOpuesta().abrir();
        versionCierres++;
    }

    /** Cambia cada vez que se abre o cierra una calle: las unidades la usan para saber cuándo replanificar. */
    public int getVersionCierres() {
        return versionCierres;
    }

    /** Ocupación relativa promedio de todas las vías, útil para métricas. */
    public double densidadPromedio() {
        double suma = 0;
        for (Via v : vias) suma += v.densidad();
        return vias.estaVacia() ? 0 : suma / vias.tamano();
    }

    private Nodo exigir(String id) {
        Nodo n = nodos.obtener(id);
        if (n == null) throw new IllegalArgumentException("No existe el nodo " + id);
        return n;
    }
}
