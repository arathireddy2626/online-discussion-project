package com.onlinediscussion.model;

/**
 * A student enrolled in the course.
 *
 * <p>{@code studentID} uniquely identifies an object of this class, so it has no setter.
 */
public class Student extends User {

    private String studentID;

    public Student() {
        super();
    }

    public Student(String firstname, String lastname, String username, String studentID) {
        super(firstname, lastname, username);
        this.studentID = studentID;
    }

    public String getStudentID() {
        return studentID;
    }

    @Override
    public String toString() {
        return "Student[studentID=" + studentID + ", username=" + getUsername() + "]";
    }
}
