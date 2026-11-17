package com.onlinediscussion.core;

/**
 * Raised when an operation cannot be completed, for example because a record is missing, a
 * request is not allowed in the current state, or the database rejected a change.
 */
public class DiscussionException extends Exception {

    private static final long serialVersionUID = 1L;

    public DiscussionException(String message) {
        super(message);
    }

    public DiscussionException(String message, Throwable cause) {
        super(message, cause);
    }
}
