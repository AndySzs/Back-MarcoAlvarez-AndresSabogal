package co.edu.eci.blueprints.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plano identificado por author + name. Thread-safe: varios clientes pueden
 * agregar puntos al mismo plano al mismo tiempo (REST y STOMP).
 */
public class Blueprint {

    /** Regex permitido para author y name (tambien evita '.' y '/' en el topico STOMP). */
    public static final String ID_REGEX = "[A-Za-z0-9_-]+";
    public static final int ID_MAX_LENGTH = 50;

    private final String author;
    private final String name;
    private final List<Point> points;

    public Blueprint(String author, String name, List<Point> points) {
        this.author = author;
        this.name = name;
        this.points = points == null ? new ArrayList<>() : new ArrayList<>(points);
    }

    public Blueprint(String author, String name) {
        this(author, name, List.of());
    }

    public String getAuthor() {
        return author;
    }

    public String getName() {
        return name;
    }

    public synchronized List<Point> getPoints() {
        return List.copyOf(points);
    }

    @JsonIgnore
    public synchronized int getPointsCount() {
        return points.size();
    }

    public synchronized void addPoint(Point p) {
        points.add(p);
    }

    public synchronized void replacePoints(List<Point> newPoints) {
        points.clear();
        if (newPoints != null) {
            points.addAll(newPoints);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Blueprint other)) return false;
        return Objects.equals(author, other.author) && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, name);
    }

    @Override
    public String toString() {
        return "Blueprint{author=" + author + ", name=" + name + ", points=" + getPointsCount() + "}";
    }
}
