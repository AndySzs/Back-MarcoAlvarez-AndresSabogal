package co.edu.eci.blueprints.persistence;

import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBlueprintPersistenceTest {

    private InMemoryBlueprintPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryBlueprintPersistence();
    }

    @Test
    void seedDataIsLoaded() throws Exception {
        assertEquals(4, persistence.getBlueprint("juan", "plano-1").getPointsCount());
        assertEquals(3, persistence.getBlueprint("juan", "plano-2").getPointsCount());
        assertEquals(2, persistence.getBlueprintsByAuthor("juan").size());
    }

    @Test
    void saveAndGetBlueprint() throws Exception {
        persistence.saveBlueprint(new Blueprint("ana", "casa", List.of(new Point(1, 2))));

        Blueprint bp = persistence.getBlueprint("ana", "casa");
        assertEquals("ana", bp.getAuthor());
        assertEquals(List.of(new Point(1, 2)), bp.getPoints());
    }

    @Test
    void savingDuplicateThrows() throws Exception {
        persistence.saveBlueprint(new Blueprint("ana", "casa"));
        assertThrows(BlueprintPersistenceException.class,
                () -> persistence.saveBlueprint(new Blueprint("ana", "casa", List.of(new Point(9, 9)))));
    }

    @Test
    void updateReplacesAllPoints() throws Exception {
        persistence.updateBlueprint("juan", "plano-1", List.of(new Point(7, 7), new Point(8, 8)));
        assertEquals(List.of(new Point(7, 7), new Point(8, 8)),
                persistence.getBlueprint("juan", "plano-1").getPoints());
    }

    @Test
    void updateMissingThrows() {
        assertThrows(BlueprintNotFoundException.class,
                () -> persistence.updateBlueprint("nobody", "none", List.of()));
    }

    @Test
    void deleteRemovesBlueprint() throws Exception {
        persistence.deleteBlueprint("john", "house");
        assertThrows(BlueprintNotFoundException.class, () -> persistence.getBlueprint("john", "house"));
        assertThrows(BlueprintNotFoundException.class, () -> persistence.deleteBlueprint("john", "house"));
    }

    @Test
    void authorWithoutBlueprintsReturnsEmptySet() {
        assertTrue(persistence.getBlueprintsByAuthor("ghost").isEmpty());
    }

    @Test
    void concurrentAddPointKeepsEveryPoint() throws Exception {
        persistence.saveBlueprint(new Blueprint("race", "board"));
        int threads = 10;
        int pointsPerThread = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int id = t;
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < pointsPerThread; i++) {
                    persistence.addPoint("race", "board", id, i);
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(threads * pointsPerThread, persistence.getBlueprint("race", "board").getPointsCount());
    }
}
