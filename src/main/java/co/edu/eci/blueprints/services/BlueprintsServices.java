package co.edu.eci.blueprints.services;

import co.edu.eci.blueprints.filters.BlueprintsFilter;
import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistence;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class BlueprintsServices {

    private static final Comparator<Blueprint> ORDER =
            Comparator.comparing(Blueprint::getAuthor).thenComparing(Blueprint::getName);

    private final BlueprintPersistence persistence;
    private final BlueprintsFilter filter;

    public BlueprintsServices(BlueprintPersistence persistence, BlueprintsFilter filter) {
        this.persistence = persistence;
        this.filter = filter;
    }

    public void addNewBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        persistence.saveBlueprint(bp);
    }

    public List<Blueprint> getAllBlueprints() {
        return sortedAndFiltered(persistence.getAllBlueprints());
    }

    public List<Blueprint> getBlueprintsByAuthor(String author) {
        return sortedAndFiltered(persistence.getBlueprintsByAuthor(author));
    }

    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        return filter.apply(persistence.getBlueprint(author, name));
    }

    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        persistence.addPoint(author, name, x, y);
    }

    public Blueprint updateBlueprint(String author, String name, List<Point> points) throws BlueprintNotFoundException {
        persistence.updateBlueprint(author, name, points);
        return getBlueprint(author, name);
    }

    public void deleteBlueprint(String author, String name) throws BlueprintNotFoundException {
        persistence.deleteBlueprint(author, name);
    }

    private List<Blueprint> sortedAndFiltered(Collection<Blueprint> blueprints) {
        return blueprints.stream().sorted(ORDER).map(filter::apply).toList();
    }
}
