package co.edu.eci.blueprints.rt;

import co.edu.eci.blueprints.model.Point;

import java.util.List;

/**
 * Mensaje que el servidor publica en /topic/blueprints.{author}.{name}.
 * type: "points" (agregar puntos), "replace" (reemplazar todos) o "deleted".
 */
public record BlueprintUpdate(String type, String author, String name, List<Point> points, String senderId, Long ts) {

    public static final String POINTS = "points";
    public static final String REPLACE = "replace";
    public static final String DELETED = "deleted";
}
