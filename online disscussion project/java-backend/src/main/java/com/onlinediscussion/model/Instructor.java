package com.onlinediscussion.model;

/**
 * The instructor who owns the course. Adds no attributes of its own.
 */
public class Instructor extends User {

    public Instructor() {
        super();
    }

    public Instructor(String firstname, String lastname, String username) {
        super(firstname, lastname, username);
    }

    @Override
    public String toString() {
        return "Instructor[username=" + getUsername() + "]";
    }
}
