package com.onlinediscussion.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A course owned by an instructor. Holds the enrolled students and the discussions created
 * inside it.
 *
 * <p>{@code courseNumber} uniquely identifies an object of this class, so it has no setter.
 */
public class Course {

    private String courseName;
    private String courseNumber;
    private final List<Student> students = new ArrayList<>();
    private final List<Discussion> discussions = new ArrayList<>();

    public Course() {
    }

    public Course(String courseName, String courseNumber) {
        this.courseName = courseName;
        this.courseNumber = courseNumber;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseNumber() {
        return courseNumber;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(Collection<Student> newStudents) {
        students.clear();
        if (newStudents != null) {
            students.addAll(newStudents);
        }
    }

    public List<Discussion> getDiscussions() {
        return discussions;
    }

    public void setDiscussions(Collection<Discussion> newDiscussions) {
        discussions.clear();
        if (newDiscussions != null) {
            discussions.addAll(newDiscussions);
        }
    }

    /** Adds a student to this course. Duplicate students are ignored. */
    public boolean addStudent(Student s) {
        if (s == null || students.contains(s)) {
            return false;
        }
        return students.add(s);
    }

    /** Removes a student from this course. Returns true when a student was removed. */
    public boolean removeStudent(Student s) {
        return students.remove(s);
    }

    /** Finds a student by identifier. Returns {@code null} when there is no match. */
    public Student searchStudent(String id) {
        if (id == null) {
            return null;
        }
        for (Student student : students) {
            if (id.equals(student.getStudentID())) {
                return student;
            }
        }
        return null;
    }

    /** Adds a discussion to this course. Duplicate discussions are ignored. */
    public boolean addDiscussion(Discussion d) {
        if (d == null || discussions.contains(d)) {
            return false;
        }
        return discussions.add(d);
    }

    /** Removes a discussion from this course. Returns true when a discussion was removed. */
    public boolean removeDiscussion(Discussion d) {
        return discussions.remove(d);
    }

    /** Finds a discussion by identifier. Returns {@code null} when there is no match. */
    public Discussion searchDiscussionByID(String id) {
        if (id == null) {
            return null;
        }
        for (Discussion discussion : discussions) {
            if (id.equals(discussion.getDiscussionID())) {
                return discussion;
            }
        }
        return null;
    }

    /** Finds the first discussion with the given topic. Returns {@code null} when absent. */
    public Discussion searchDiscussionByTopic(String topic) {
        if (topic == null) {
            return null;
        }
        for (Discussion discussion : discussions) {
            if (topic.equalsIgnoreCase(discussion.getTopic())) {
                return discussion;
            }
        }
        return null;
    }

    /**
     * Moves a discussion of this course into the archive. Only the instructor may call this.
     *
     * <p>The archived discussion is taken out of the active list. Returns {@code true} when a
     * discussion was archived.
     */
    public boolean archiveDiscussion(String id) {
        Discussion discussion = searchDiscussionByID(id);
        if (discussion == null) {
            return false;
        }
        discussions.remove(discussion);
        return true;
    }

    @Override
    public String toString() {
        return "Course[courseNumber=" + courseNumber
                + ", courseName=" + courseName
                + ", students=" + students.size()
                + ", discussions=" + discussions.size()
                + "]";
    }
}
