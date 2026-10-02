package co.edu.eci.blueprints.dto;

/** Envoltorio uniforme de todas las respuestas REST. */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> of(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
