package sgmms.modelo;

/** Texto flotante sobre el mapa (puntos ganados o perdidos). */
public class Efecto {

    private static final double DURACION = 1.6;

    private final double x;
    private final double y;
    private final String texto;
    private final boolean positivo;
    private double vida = DURACION;

    public Efecto(double x, double y, String texto, boolean positivo) {
        this.x = x;
        this.y = y;
        this.texto = texto;
        this.positivo = positivo;
    }

    public void avanzar(double dt) {
        vida -= dt;
    }

    public boolean terminado() {
        return vida <= 0;
    }

    /** Va de 1 (recién creado) a 0 (por desaparecer). */
    public double opacidad() {
        return Math.max(0, Math.min(1, vida / DURACION));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getTexto() {
        return texto;
    }

    public boolean esPositivo() {
        return positivo;
    }
}
