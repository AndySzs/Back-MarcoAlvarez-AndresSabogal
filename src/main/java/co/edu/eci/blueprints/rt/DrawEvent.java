package co.edu.eci.blueprints.rt;

import co.edu.eci.blueprints.model.Point;

/**
 * Mensaje que el cliente envia a /app/draw.
 * senderId y ts son opcionales; el front los usa para medir latencia y reconocer su eco.
 */
public record DrawEvent(String author, String name, Point point, String senderId, Long ts) {
}
