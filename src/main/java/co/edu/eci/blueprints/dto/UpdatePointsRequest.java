package co.edu.eci.blueprints.dto;

import co.edu.eci.blueprints.model.Point;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdatePointsRequest(@NotNull @Valid List<@NotNull @Valid Point> points) {
}
