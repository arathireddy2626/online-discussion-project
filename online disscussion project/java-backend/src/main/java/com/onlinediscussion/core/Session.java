package com.onlinediscussion.core;

/**
 * The identity behind the currently signed in user.
 */
public class Session {

    private final String username;
    private final Role role;
    private final String studentID;
    private final String fullName;

    public Session(String username, Role role, String studentID, String fullName) {
        this.username = username;
        this.role = role;
        this.studentID = studentID;
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    /** The student identifier of the signed in student, or {@code null} for an instructor. */
    public String getStudentID() {
        return studentID;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isInstructor() {
        return role == Role.INSTRUCTOR;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    @Override
    public String toString() {
        return "Session[" + username + ", " + role + "]";
    }
}
