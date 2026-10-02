package co.edu.eci.blueprints.dto;

import co.edu.eci.blueprints.model.Blueprint;

import java.util.List;

public record AuthorBlueprints(String author, int totalPoints, List<Blueprint> blueprints) {

    public static AuthorBlueprints of(String author, List<Blueprint> blueprints) {
        int total = blueprints.stream().mapToInt(Blueprint::getPointsCount).sum();
        return new AuthorBlueprints(author, total, blueprints);
    }
}
