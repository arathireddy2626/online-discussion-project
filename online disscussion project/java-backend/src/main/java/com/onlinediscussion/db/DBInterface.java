package com.onlinediscussion.db;

import com.onlinediscussion.core.DiscussionException;
import com.onlinediscussion.model.Archive;
import com.onlinediscussion.model.Course;
import com.onlinediscussion.model.Discussion;
import com.onlinediscussion.model.Message;
import com.onlinediscussion.model.Student;

import java.util.List;

/**
 * Persistence gateway for the Online Discussion Tool.
 *
 * <p>Every method mirrors one of the operations listed in the backend design. Methods that
 * create a record return the identifier that the database assigned so that callers can
 * reload the fully populated object; this keeps the "no setter on a unique identifier"
 * rule intact on the model classes.
 */
public interface DBInterface {

    // ------------------------------------------------------------------
    // Students
    // ------------------------------------------------------------------

    /** Inserts or replaces the record for the given student. */
    void storeStudent(Student s) throws DiscussionException;

    /** Loads a student by identifier. Returns {@code null} when there is no match. */
    Student retrieveStudentByID(String id) throws DiscussionException;

    /** Loads a student by user name. Returns {@code null} when there is no match. */
    Student retrieveStudentByUsername(String uname) throws DiscussionException;

    /** Deletes the student record and every row that depends on it. */
    void removeStudentByID(String id) throws DiscussionException;

    /** Deletes the student record and every row that depends on it. */
    void removeStudentByUsername(String uname) throws DiscussionException;

    // ------------------------------------------------------------------
    // Discussions
    // ------------------------------------------------------------------

    /** Inserts a discussion and returns the identifier the database assigned to it. */
    String storeDiscussion(Discussion d) throws DiscussionException;

    /** Loads a discussion and its messages. Returns {@code null} when there is no match. */
    Discussion retrieveDiscussionByID(String id) throws DiscussionException;

    /** Loads every discussion whose topic matches the given text. */
    List<Discussion> retrieveDiscussionByTopic(String topic) throws DiscussionException;

    /** Loads every discussion that is not archived, newest first. */
    List<Discussion> retrieveActiveDiscussions() throws DiscussionException;

    /** Moves a discussion into the archive. */
    void archiveDiscussion(String id) throws DiscussionException;

    // ------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------

    /** Attaches a message to a discussion and returns the new message identifier. */
    String addMessageToDiscussion(String discussionID, Message message) throws DiscussionException;

    /** Detaches a message from a discussion and deletes the message row. */
    void removeMessageFromDiscussion(String discussionID, Message message) throws DiscussionException;

    // ------------------------------------------------------------------
    // Courses (added during implementation)
    // ------------------------------------------------------------------

    /** Inserts or updates a course. */
    void storeCourse(Course c) throws DiscussionException;

    /** Loads a course by its number. Returns {@code null} when there is no match. */
    Course retrieveCourseByNumber(String courseNumber) throws DiscussionException;

    /** Loads every course, ordered by course number. */
    List<Course> retrieveAllCourses() throws DiscussionException;

    /** Enrols a student in a course. */
    void enrollStudent(String courseNumber, String studentID) throws DiscussionException;

    /** Removes a student from a course. */
    void unenrollStudent(String courseNumber, String studentID) throws DiscussionException;

    /** Loads the students enrolled in a course. */
    List<Student> retrieveEnrolledStudents(String courseNumber) throws DiscussionException;

    /** Tells whether a student is enrolled in a course. */
    boolean isStudentEnrolled(String courseNumber, String studentID) throws DiscussionException;

    // ------------------------------------------------------------------
    // Requests (added during implementation)
    // ------------------------------------------------------------------

    /** Records a request to join a course. */
    void storeEnrollmentRequest(String studentID) throws DiscussionException;

    /** Records a request to leave a course. */
    void storeRemovalRequest(String studentID) throws DiscussionException;

    /** Loads the students waiting to join a course, oldest request first. */
    List<Student> retrieveEnrollmentRequests() throws DiscussionException;

    /** Loads the students waiting to leave a course, oldest request first. */
    List<Student> retrieveRemovalRequests() throws DiscussionException;

    /** Clears a pending join request. */
    void clearEnrollmentRequest(String studentID) throws DiscussionException;

    /** Clears a pending leave request. */
    void clearRemovalRequest(String studentID) throws DiscussionException;

    // ------------------------------------------------------------------
    // Messages (read helpers, added during implementation)
    // ------------------------------------------------------------------

    /** Loads every message of a discussion, oldest first. */
    List<Message> retrieveMessagesByDiscussion(String discussionID) throws DiscussionException;

    /** Loads one message of a discussion by identifier. */
    Message retrieveMessageByID(String discussionID, String messageID) throws DiscussionException;

    /** Loads every message of a discussion posted by the given user name. */
    List<Message> retrieveMessagesByUsername(String discussionID, String uname) throws DiscussionException;

    // ------------------------------------------------------------------
    // Archive (added during implementation)
    // ------------------------------------------------------------------

    /** Loads every archived discussion. */
    List<Discussion> retrieveArchivedDiscussions() throws DiscussionException;

    /** Loads the archive as a whole. */
    Archive retrieveArchive() throws DiscussionException;

    // ------------------------------------------------------------------
    // Credentials (added during implementation)
    // ------------------------------------------------------------------

    /** Verifies a user name and password. Returns {@code null} when they do not match. */
    String verifyCredentials(String username, String password) throws DiscussionException;

    /** Creates a login credential. The password is hashed before it is stored. */
    void storeCredential(String username, String password, String role) throws DiscussionException;

    /** Assigns a login user name to an existing student record. */
    void assignUsernameToStudent(String studentID, String username) throws DiscussionException;

    /**
     * Makes sure a login credential exists for the given user name.
     *
     * <p>Returns the plain password when a new credential was created, or {@code null} when the
     * credential was already there. The student table holds a foreign key on the user name, so
     * a credential has to exist before a student row can name it.
     */
    String ensureCredential(String username) throws DiscussionException;

    /** Loads the role of a user, or {@code null} when the user is unknown. */
    String retrieveRole(String username) throws DiscussionException;
}
