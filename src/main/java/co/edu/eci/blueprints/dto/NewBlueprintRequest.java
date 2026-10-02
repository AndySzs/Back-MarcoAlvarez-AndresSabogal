package co.edu.eci.blueprints.dto;

import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewBlueprintRequest(
        @NotBlank @Size(max = Blueprint.ID_MAX_LENGTH) @Pattern(regexp = Blueprint.ID_REGEX) String author,
        @NotBlank @Size(max = Blueprint.ID_MAX_LENGTH) @Pattern(regexp = Blueprint.ID_REGEX) String name,
        @Valid List<@Valid Point> points) {

    public Blueprint toBlueprint() {
        return new Blueprint(author, name, points == null ? List.of() : points);
    }
}
