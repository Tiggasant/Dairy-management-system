package com.ghunsai.dairy.exception;

import org.springframework.http.HttpStatus;

/** Business-level error that is safe to show to the API client. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static class NotFound extends ApiException {
        public NotFound(String message) {
            super(HttpStatus.NOT_FOUND, message);
        }
    }

    public static class BadRequest extends ApiException {
        public BadRequest(String message) {
            super(HttpStatus.BAD_REQUEST, message);
        }
    }

    public static class Conflict extends ApiException {
        public Conflict(String message) {
            super(HttpStatus.CONFLICT, message);
        }
    }

    public static class Unauthorized extends ApiException {
        public Unauthorized(String message) {
            super(HttpStatus.UNAUTHORIZED, message);
        }
    }

    public static class TooManyRequests extends ApiException {
        public TooManyRequests(String message) {
            super(HttpStatus.TOO_MANY_REQUESTS, message);
        }
    }
}
