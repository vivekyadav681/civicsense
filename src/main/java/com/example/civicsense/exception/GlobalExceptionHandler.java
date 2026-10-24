package com.example.civicsense.exception;

import com.example.civicsense.dto.response.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;

/**
 * Centralised exception handler that converts exceptions into standardised
 * {@link ApiError} JSON responses. Following the "never leak stack traces" principle.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ApiError> handleBadTransition(IllegalStateTransitionException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid State Transition", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateEmailException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this action");
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiError> handleStorage(StorageException ex) {
        log.error("Storage failure", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Storage Error", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large",
                     "Uploaded file exceeds the maximum allowed size (10 MB)");
    }

    /** Handles @Valid annotation failures for both @RequestBody and @ModelAttribute. */
    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.validation.BindException.class})
    public ResponseEntity<ApiError> handleValidation(Exception ex) {
        org.springframework.validation.BindingResult bindingResult = ex instanceof MethodArgumentNotValidException manve
                ? manve.getBindingResult()
                : ((org.springframework.validation.BindException) ex).getBindingResult();

        List<String> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation errors")
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /** Catch-all for unexpected errors — avoids leaking implementation details. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                     "An unexpected error occurred. Please try again later.");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message) {
        ApiError body = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
