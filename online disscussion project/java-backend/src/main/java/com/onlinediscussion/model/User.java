package com.onlinediscussion.model;

/**
 * Base class for every human actor in the system.
 *
 * <p>{@code username} uniquely identifies an object of this class, so it deliberately has no
 * setter. {@code firstname} and {@code lastname} are mutable and therefore have setters.
 */
public class User {

    private String firstname;
    private String lastname;
    private String username;

    public User() {
    }

    public User(String firstname, String lastname, String username) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getUsername() {
        return username;
    }

    /** Convenience accessor used by the command line interface and by log output. */
    public String getFullName() {
        if (firstname == null || firstname.isBlank()) {
            return lastname == null ? "" : lastname;
        }
        if (lastname == null || lastname.isBlank()) {
            return firstname;
        }
        return firstname + " " + lastname;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[username=" + username + "]";
    }
}
