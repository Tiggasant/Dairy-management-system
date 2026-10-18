package com.ghunsai.dairy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every exception into the same {@link ApiError} JSON body.
 * Framework exceptions (bad JSON, wrong HTTP method, 404 ...) are handled by the parent class
 * and re-shaped in {@link #handleExceptionInternal}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        HttpStatus status = ex.getStatus();
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", "You do not have permission to perform this action"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Bad Request", "Invalid value for parameter '" + ex.getName() + "'"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", "Request conflicts with existing data (possible duplicate)"));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleLock(PessimisticLockingFailureException ex) {
        log.warn("Lock failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", "The record is busy, please retry"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal Server Error", "Something went wrong. Please try again later"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> fieldErrors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        ApiError body = ApiError.of(400, "Bad Request", "Validation failed", fieldErrors);
        return ResponseEntity.badRequest().headers(headers).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String reason = status != null ? status.getReasonPhrase() : "Error";
        String message = status != null && status.is4xxClientError()
                ? clientMessage(status)
                : "Something went wrong. Please try again later";
        if (statusCode.is5xxServerError()) {
            log.error("Framework exception", ex);
        }
        return ResponseEntity.status(statusCode).headers(headers)
                .body(ApiError.of(statusCode.value(), reason, message));
    }

    private static String clientMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Resource not found";
            case METHOD_NOT_ALLOWED -> "HTTP method not supported for this endpoint";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported content type, use application/json";
            case NOT_ACCEPTABLE -> "Requested response format is not supported";
            default -> "Malformed or invalid request";
        };
    }
}
