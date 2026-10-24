package com.example.civicsense.exception;

import com.example.civicsense.enums.ComplaintStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an invalid complaint status transition is attempted.
 * Maps to HTTP 422 Unprocessable Entity.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(ComplaintStatus from, ComplaintStatus to) {
        super(String.format(
            "Invalid status transition: [%s] → [%s] is not allowed by the state machine", from, to));
    }

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
