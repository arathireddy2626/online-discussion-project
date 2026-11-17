package com.onlinediscussion.core;

/**
 * The two kinds of actor that can sign in to the tool.
 */
public enum Role {

    INSTRUCTOR,
    STUDENT;

    /**
     * Parses the role stored in the database. Accepts the {@code INSTRUCTOR} / {@code STUDENT}
     * spelling used by the {@code LoginCredential.Role} column as well as the mixed case
     * variants used by the HTTP layer.
     */
    public static Role fromDatabase(String value) {
        if (value == null) {
            return STUDENT;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.equals("INSTRUCTOR") || normalized.equals("TEACHER")) {
            return INSTRUCTOR;
        }
        return STUDENT;
    }
}
