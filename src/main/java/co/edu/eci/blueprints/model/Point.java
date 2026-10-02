package co.edu.eci.blueprints.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record Point(
        @Min(Point.MIN) @Max(Point.MAX) int x,
        @Min(Point.MIN) @Max(Point.MAX) int y) {

    public static final int MIN = -10000;
    public static final int MAX = 10000;

    public static boolean inRange(int value) {
        return value >= MIN && value <= MAX;
    }
}
