package com.example.civicsense.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when file upload to Cloudinary (or any storage backend) fails.
 * Maps to HTTP 500 Internal Server Error.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
