package co.edu.eci.blueprints.persistence;

import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryBlueprintPersistence implements BlueprintPersistence {

    private final Map<String, Blueprint> blueprints = new ConcurrentHashMap<>();

    public InMemoryBlueprintPersistence() {
        // Datos semilla, coordenadas dentro de un canvas de 600x400
        seed(new Blueprint("juan", "plano-1",
                List.of(new Point(50, 50), new Point(300, 50), new Point(300, 250), new Point(50, 250))));
        seed(new Blueprint("juan", "plano-2",
                List.of(new Point(100, 300), new Point(250, 100), new Point(400, 300))));
        seed(new Blueprint("john", "house",
                List.of(new Point(150, 350), new Point(150, 200), new Point(300, 80),
                        new Point(450, 200), new Point(450, 350), new Point(150, 350))));
        seed(new Blueprint("jane", "garden",
                List.of(new Point(80, 320), new Point(200, 180), new Point(320, 320),
                        new Point(440, 180), new Point(560, 320))));
    }

    private void seed(Blueprint bp) {
        blueprints.put(key(bp.getAuthor(), bp.getName()), bp);
    }

    private static String key(String author, String name) {
        return author + ":" + name;
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        Blueprint previous = blueprints.putIfAbsent(key(bp.getAuthor(), bp.getName()), bp);
        if (previous != null) {
            throw new BlueprintPersistenceException(
                    "Blueprint already exists: " + bp.getAuthor() + "/" + bp.getName());
        }
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        Blueprint bp = blueprints.get(key(author, name));
        if (bp == null) {
            throw new BlueprintNotFoundException("Blueprint not found: " + author + "/" + name);
        }
        return bp;
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) {
        return blueprints.values().stream()
                .filter(bp -> bp.getAuthor().equals(author))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        return Set.copyOf(blueprints.values());
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        getBlueprint(author, name).addPoint(new Point(x, y));
    }

    @Override
    public void updateBlueprint(String author, String name, List<Point> points) throws BlueprintNotFoundException {
        getBlueprint(author, name).replacePoints(points);
    }

    @Override
    public void deleteBlueprint(String author, String name) throws BlueprintNotFoundException {
        if (blueprints.remove(key(author, name)) == null) {
            throw new BlueprintNotFoundException("Blueprint not found: " + author + "/" + name);
        }
    }
}
