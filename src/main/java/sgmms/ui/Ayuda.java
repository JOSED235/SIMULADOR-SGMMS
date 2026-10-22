package sgmms.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Superposición con las reglas y los controles. */
final class Ayuda {

    private Ayuda() {
    }

    static Node crear(Runnable alCerrar) {
        VBox contenido = new VBox(14);
        contenido.getStyleClass().add("tarjeta");
        contenido.setMaxWidth(720);

        Label titulo = new Label("Cómo se juega");
        titulo.getStyleClass().add("titulo-medio");
        Button cerrar = new Button("Cerrar (Esc)");
        cerrar.getStyleClass().addAll("boton", "boton-primario");
        cerrar.setOnAction(e -> alCerrar.run());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(titulo, espacio, cerrar);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        contenido.getChildren().addAll(cabecera,
                bloque("Tu papel",
                        "Eres el operador del centro de despacho. Llegan incidentes a distintos puntos de la ciudad y "
                                + "cada uno tiene un plazo: debes lograr que todas las unidades que necesita lleguen "
                                + "antes de que el anillo de colores se vacíe. Si se vence, cuenta como fallo."),
                bloque("Incidentes",
                        "Robo → patrulla.  Emergencia médica → ambulancia.  Incendio → bomberos (gravedad 3: dos camiones).  "
                                + "Accidente de tránsito → patrulla y ambulancia, y además cierra la calle en ambos sentidos.\n"
                                + "Un incendio que pasa la mitad de su plazo sin bomberos en escena se propaga y sube de gravedad."),
                bloque("Despachar",
                        "• Clic en un incidente para verlo en el panel: aparecen las unidades recomendadas con su ETA calculado por A* "
                                + "sobre el tráfico actual. Enter despacha las recomendadas.\n"
                                + "• O selecciona una unidad (en el mapa o en la flota) y haz clic en el incidente.\n"
                                + "• Una unidad que va en camino se puede reasignar a otro incidente más urgente."),
                bloque("Tráfico y cierres",
                        "Las calles cambian de color según la congestión (capa T). Las unidades recalculan su ruta cada pocos "
                                + "segundos y apenas se cierra o se abre una vía. Los autos civiles también se desvían."),
                bloque("Tomar el volante",
                        "Selecciona una unidad y presiona C para conducirla con WASD o las flechas. La línea cian es el GPS. "
                                + "Manejando no te frena la congestión, pero chocar con un auto sí. C o Esc devuelven la unidad al piloto automático."),
                bloque("Atajos",
                        "Espacio: pausa · 1 / 2 / 3: velocidad x1, x2, x4 · Tab: siguiente incidente urgente · T: capa de tráfico · "
                                + "F12: guardar captura · Clic derecho: limpiar selección"),
                bloque("Puntaje",
                        "Cada incidente resuelto da 100 puntos por nivel de gravedad, más un bono de hasta 50 % si las unidades "
                                + "llegaron con plazo de sobra. Cada incidente vencido resta 75."));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setMaxWidth(740);
        scroll.setMaxHeight(620);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        StackPane velo = new StackPane(scroll);
        velo.getStyleClass().add("velo");
        velo.setPadding(new Insets(24));
        velo.setOnMouseClicked(e -> {
            if (e.getTarget() == velo) alCerrar.run();
        });
        return velo;
    }

    private static Node bloque(String titulo, String texto) {
        Label t = new Label(titulo.toUpperCase());
        t.getStyleClass().add("titulo-seccion");
        Label cuerpo = new Label(texto);
        cuerpo.setWrapText(true);
        cuerpo.setStyle("-fx-line-spacing: 3;");
        return new VBox(4, t, cuerpo);
    }
}
