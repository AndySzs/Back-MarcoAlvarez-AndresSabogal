package co.edu.eci.blueprints.persistence;

/** Se lanza cuando se intenta crear un plano que ya existe. */
public class BlueprintPersistenceException extends Exception {
    public BlueprintPersistenceException(String message) {
        super(message);
    }
}
