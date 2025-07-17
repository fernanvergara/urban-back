package com.transporte.urbanback.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.persistence.EntityNotFoundException; // Importar EntityNotFoundException
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la API REST.
 * Utiliza @ControllerAdvice para interceptar excepciones en toda la aplicación.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Maneja excepciones de tipo ResourceNotFoundException, mapeándolas a 404 Not Found.
     * @param ex La excepción ResourceNotFoundException.
     * @param request La solicitud web actual.
     * @return ResponseEntity con el objeto ErrorResponse y estado 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja excepciones de tipo EntityNotFoundException (de JPA), mapeándolas a 404 Not Found.
     * @param ex La excepción EntityNotFoundException.
     * @param request La solicitud web actual.
     * @return ResponseEntity con el objeto ErrorResponse y estado 404.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja excepciones de tipo IllegalArgumentException, mapeándolas a 400 Bad Request.
     * @param ex La excepción IllegalArgumentException.
     * @param request La solicitud web actual.
     * @return ResponseEntity con el objeto ErrorResponse y estado 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja excepciones de tipo IllegalStateException, mapeándolas a 409 Conflict.
     * @param ex La excepción IllegalStateException.
     * @param request La solicitud web actual.
     * @return ResponseEntity con el objeto ErrorResponse y estado 409.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de tipo SecurityException, mapeándolas a 403 Forbidden.
     * @param ex La excepción SecurityException.
     * @param request La solicitud web actual.
     * @return ResponseEntity con el objeto ErrorResponse y estado 403.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Maneja excepciones genéricas (Exception), mapeándolas a 500 Internal Server Error.
     * Esta es la última línea de defensa para cualquier excepción no manejada específicamente.
     * @param ex La excepción genérica.
     * @param request La solicitud web actual.
     * @return ResponseEntity con el objeto ErrorResponse y estado 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrió un error inesperado en el servidor.", // Mensaje genérico para seguridad
                request.getDescription(false).replace("uri=", "")
        );
        // Opcional: Loggear la excepción completa para depuración
        // logger.error("Error interno del servidor: ", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Personaliza el manejo de excepciones de validación de argumentos de método (@Valid).
     * @param ex La excepción MethodArgumentNotValidException.
     * @param headers Los encabezados HTTP.
     * @param status El estado HTTP.
     * @param request La solicitud web actual.
     * @return ResponseEntity con un mapa de errores de validación y estado 400.
     */
    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Uno o más campos tienen errores de validación.",
                request.getDescription(false).replace("uri=", "")
        );
        // Añadir los detalles de los errores de campo al mensaje o a un campo adicional si se desea
        errorResponse.setMessage(errorResponse.getMessage() + " Errores: " + errors.toString());

        return new ResponseEntity<>(errorResponse, headers, status);
    }

     
}
