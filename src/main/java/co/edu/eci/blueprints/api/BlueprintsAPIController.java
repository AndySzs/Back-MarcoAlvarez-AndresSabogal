package co.edu.eci.blueprints.api;

import co.edu.eci.blueprints.dto.ApiResponse;
import co.edu.eci.blueprints.dto.AuthorBlueprints;
import co.edu.eci.blueprints.dto.NewBlueprintRequest;
import co.edu.eci.blueprints.dto.UpdatePointsRequest;
import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import co.edu.eci.blueprints.rt.BlueprintEventsPublisher;
import co.edu.eci.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/blueprints", "/api/blueprints"})
@Tag(name = "Blueprints", description = "CRUD de planos; los cambios se difunden por STOMP")
public class BlueprintsAPIController {

    private final BlueprintsServices services;
    private final BlueprintEventsPublisher events;

    public BlueprintsAPIController(BlueprintsServices services, BlueprintEventsPublisher events) {
        this.services = services;
        this.events = events;
    }

    @Operation(summary = "Lista todos los planos, o los de un autor si se envia ?author=")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(@RequestParam(required = false) String author) {
        if (author != null && !author.isBlank()) {
            return ok(byAuthor(author));
        }
        return ok(services.getAllBlueprints());
    }

    @Operation(summary = "Planos de un autor con el total de puntos")
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<?>> getByAuthor(@PathVariable String author) {
        return ok(byAuthor(author));
    }

    @Operation(summary = "Obtiene un plano")
    @GetMapping("/{author}/{name}")
    public ResponseEntity<ApiResponse<?>> getOne(@PathVariable String author, @PathVariable String name)
            throws BlueprintNotFoundException {
        return ok(services.getBlueprint(author, name));
    }

    @Operation(summary = "Crea un plano (409 si ya existe)")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody NewBlueprintRequest req)
            throws BlueprintPersistenceException {
        Blueprint bp = req.toBlueprint();
        services.addNewBlueprint(bp);
        return respond(HttpStatus.CREATED, "Blueprint created", bp);
    }

    @Operation(summary = "Reemplaza todos los puntos del plano y publica 'replace'")
    @PutMapping("/{author}/{name}")
    public ResponseEntity<ApiResponse<?>> replacePoints(@PathVariable String author, @PathVariable String name,
                                                        @Valid @RequestBody UpdatePointsRequest req)
            throws BlueprintNotFoundException {
        Blueprint bp = services.updateBlueprint(author, name, req.points());
        events.publishReplace(author, name, bp.getPoints());
        return ok(bp);
    }

    @Operation(summary = "Agrega un punto al plano y publica 'points'")
    @PutMapping("/{author}/{name}/points")
    public ResponseEntity<ApiResponse<?>> addPoint(@PathVariable String author, @PathVariable String name,
                                                   @Valid @RequestBody Point point)
            throws BlueprintNotFoundException {
        services.addPoint(author, name, point.x(), point.y());
        events.publishPoints(author, name, List.of(point), null, System.currentTimeMillis());
        return respond(HttpStatus.ACCEPTED, "Point added", services.getBlueprint(author, name));
    }

    @Operation(summary = "Elimina un plano y publica 'deleted'")
    @DeleteMapping("/{author}/{name}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String author, @PathVariable String name)
            throws BlueprintNotFoundException {
        services.deleteBlueprint(author, name);
        events.publishDeleted(author, name);
        return respond(HttpStatus.OK, "Blueprint deleted", null);
    }

    private AuthorBlueprints byAuthor(String author) {
        return AuthorBlueprints.of(author, services.getBlueprintsByAuthor(author));
    }

    private static ResponseEntity<ApiResponse<?>> ok(Object data) {
        return respond(HttpStatus.OK, "OK", data);
    }

    private static ResponseEntity<ApiResponse<?>> respond(HttpStatus status, String message, Object data) {
        return ResponseEntity.status(status).body(ApiResponse.of(status.value(), message, data));
    }
}
