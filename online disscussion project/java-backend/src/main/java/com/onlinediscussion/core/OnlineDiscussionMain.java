package com.onlinediscussion.core;

import com.onlinediscussion.db.DBInterface;
import com.onlinediscussion.db.MySQLDBInterface;
import com.onlinediscussion.model.Course;
import com.onlinediscussion.model.Discussion;
import com.onlinediscussion.model.Message;
import com.onlinediscussion.model.Student;

import java.util.List;

/**
 * The public API of the Online Discussion Tool.
 *
 * <p>The class offers two groups of calls. The instructor's calls manage the course, its
 * roster and the discussions. The student's calls request enrolment and post messages. Both
 * groups live in this single facade, exactly as the class design states, and each call
 * checks that the signed in user is allowed to make it.
 *
 * <p>The instructor account is assumed to exist by the time of installation.
 */
public class OnlineDiscussionMain {

    private final DBInterface dbInterface;
    private Session session;
    private String currentCourseNumber = MySQLDBInterface.DEFAULT_COURSE_NUMBER;

    public OnlineDiscussionMain(DBInterface dbInterface) {
        this.dbInterface = dbInterface;
    }

    /** The database gateway that backs this facade. */
    public DBInterface getDbInterface() {
        return dbInterface;
    }

    /** The signed in user, or {@code null} when nobody is signed in. */
    public Session getSession() {
        return session;
    }

    /** The course that the instructor's calls act on. */
    public String getCurrentCourseNumber() {
        return currentCourseNumber;
    }

    public void setCurrentCourseNumber(String courseNumber) {
        if (courseNumber != null && !courseNumber.isBlank()) {
            this.currentCourseNumber = courseNumber.trim();
        }
    }

    // ==================================================================
    // For all users
    // ==================================================================

    /**
     * Signs a user in. Returns {@code true} when the user name and the password match an
     * existing account.
     */
    public boolean login(String username, String password) throws DiscussionException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        String role = dbInterface.verifyCredentials(username.trim(), password);
        if (role == null) {
            return false;
        }
        Student student = dbInterface.retrieveStudentByUsername(username.trim());
        String studentID = student == null ? null : student.getStudentID();
        String fullName = student == null ? username.trim() : student.getFullName();
        if (studentID == null && Role.fromDatabase(role) == Role.INSTRUCTOR) {
            fullName = username.trim();
        }
        session = new Session(username.trim(), Role.fromDatabase(role), studentID, fullName);
        return true;
    }

    /** Signs the current user out. */
    public void logout() {
        session = null;
    }

    // ==================================================================
    // Instructor's API calls
    // ==================================================================

    /** Creates a course and makes it the current course. */
    public void createCourse(Course c) throws DiscussionException {
        requireInstructor();
        if (c == null) {
            throw new DiscussionException("A course is required.");
        }
        dbInterface.storeCourse(c);
        currentCourseNumber = c.getCourseNumber();
    }

    /**
     * Returns the first student waiting to join the course, or {@code null} when the queue is
     * empty.
     */
    public Student retrieveFirstRequesttoAdd() throws DiscussionException {
        requireInstructor();
        List<Student> requests = dbInterface.retrieveEnrollmentRequests();
        return requests.isEmpty() ? null : requests.get(0);
    }

    /**
     * Returns the first student waiting to leave the course, or {@code null} when the queue is
     * empty.
     */
    public Student retrieveFirstRequestToRemove() throws DiscussionException {
        requireInstructor();
        List<Student> requests = dbInterface.retrieveRemovalRequests();
        return requests.isEmpty() ? null : requests.get(0);
    }

    /**
     * Approves an enrolment: the student joins the current course and the pending request is
     * cleared.
     */
    public void addStudent(Student s) throws DiscussionException {
        requireInstructor();
        if (s == null) {
            throw new DiscussionException("A student is required.");
        }
        dbInterface.storeStudent(s);
        dbInterface.enrollStudent(currentCourseNumber, s.getStudentID());
    }

    /** Removes a student from the current course and deletes the student record. */
    public void removeStudentbyID(String id) throws DiscussionException {
        requireInstructor();
        if (id == null || id.isBlank()) {
            throw new DiscussionException("A student identifier is required.");
        }
        dbInterface.unenrollStudent(currentCourseNumber, id);
        dbInterface.removeStudentByID(id);
    }

    /** Removes a student from the current course and deletes the student record. */
    public void removeStudentByUsername(String uname) throws DiscussionException {
        requireInstructor();
        if (uname == null || uname.isBlank()) {
            throw new DiscussionException("A user name is required.");
        }
        Student student = dbInterface.retrieveStudentByUsername(uname);
        if (student == null) {
            throw new DiscussionException("No student has the user name " + uname + ".");
        }
        dbInterface.unenrollStudent(currentCourseNumber, student.getStudentID());
        dbInterface.removeStudentByUsername(uname);
    }

    /** Finds a student by identifier. */
    public Student searchStudentByID(String id) throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveStudentByID(id);
    }

    /** Finds a student by user name. */
    public Student searchStudentByUsername(String uname) throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveStudentByUsername(uname);
    }

    /** Creates a discussion in the current course. */
    public void createDicussion(Discussion d) throws DiscussionException {
        requireInstructor();
        if (d == null) {
            throw new DiscussionException("A discussion is required.");
        }
        dbInterface.storeDiscussion(d);
    }

    /** Finds a discussion by identifier. */
    public Discussion searchDiscussionByID(String id) throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveDiscussionByID(id);
    }

    /** Finds every discussion with the given topic. */
    public List<Discussion> searchDiscussionByTopic(String topic) throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveDiscussionByTopic(topic);
    }

    /** Posts a message to a discussion. The author is always the signed in user. */
    public void postMessage(String discussionID, Message message) throws DiscussionException {
        requireSignedIn();
        if (message == null) {
            throw new DiscussionException("A message is required.");
        }
        message.setPostedBy(session.getUsername());
        message.setDiscussionID(discussionID);
        dbInterface.addMessageToDiscussion(discussionID, message);
    }

    /** Removes a message from a discussion. Only the instructor may do this. */
    public void removeMessage(String discussionID, Message message) throws DiscussionException {
        requireInstructor();
        dbInterface.removeMessageFromDiscussion(discussionID, message);
    }

    /** Finds one message of a discussion by its identifier. */
    public Message searchMessageByID(String discussionID, String messageID) throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveMessageByID(discussionID, messageID);
    }

    /** Finds every message of a discussion that was posted by the given user. */
    public List<Message> searchMessageByUsername(String discussionID, String uname) throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveMessagesByUsername(discussionID, uname);
    }

    // ==================================================================
    // Student's API calls
    // ==================================================================

    /**
     * Requests to join the course. A student who does not exist yet is created first so that
     * the instructor can act on the request.
     */
    public void requestToEnroll(Student s) throws DiscussionException {
        requireStudent();
        if (s == null) {
            throw new DiscussionException("A student is required.");
        }
        Student existing = dbInterface.retrieveStudentByID(s.getStudentID());
        if (existing == null) {
            dbInterface.storeStudent(s);
        }
        dbInterface.storeEnrollmentRequest(s.getStudentID());
    }

    /** Requests to leave the course. */
    public void requestToDrop(String studentID) throws DiscussionException {
        requireStudent();
        if (studentID == null || studentID.isBlank()) {
            throw new DiscussionException("A student identifier is required.");
        }
        dbInterface.storeRemovalRequest(studentID);
    }

    // ==================================================================
    // Additional calls used by the command line interface
    // ==================================================================

    /** Every student enrolled in the current course. */
    public List<Student> listEnrolledStudents() throws DiscussionException {
        requireInstructor();
        return dbInterface.retrieveEnrolledStudents(currentCourseNumber);
    }

    /** Every student waiting to join the course. */
    public List<Student> listEnrollmentRequests() throws DiscussionException {
        requireInstructor();
        return dbInterface.retrieveEnrollmentRequests();
    }

    /** Every student waiting to leave the course. */
    public List<Student> listRemovalRequests() throws DiscussionException {
        requireInstructor();
        return dbInterface.retrieveRemovalRequests();
    }

    /** The course that the instructor's calls act on. */
    public Course getCurrentCourse() throws DiscussionException {
        requireInstructor();
        return dbInterface.retrieveCourseByNumber(currentCourseNumber);
    }

    /** Every archived discussion. */
    public List<Discussion> listArchivedDiscussions() throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveArchivedDiscussions();
    }

    /** The archive as a whole. */
    public com.onlinediscussion.model.Archive getArchive() throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveArchive();
    }

    /** Adds a discussion to the archive. Only the instructor may do this. */
    public void archiveDiscussion(String id) throws DiscussionException {
        requireInstructor();
        dbInterface.archiveDiscussion(id);
    }

    /** Every discussion that is not archived. */
    public List<Discussion> listActiveDiscussions() throws DiscussionException {
        requireSignedIn();
        return dbInterface.retrieveActiveDiscussions();
    }

    /** The empty list, used when a search finds nothing. */
    static <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? java.util.Collections.emptyList() : values;
    }

    // ==================================================================
    // Guards
    // ==================================================================

    private void requireSignedIn() throws DiscussionException {
        if (session == null) {
            throw new DiscussionException("You must sign in first.");
        }
    }

    private void requireInstructor() throws DiscussionException {
        requireSignedIn();
        if (!session.isInstructor()) {
            throw new DiscussionException("Only the instructor may perform this action.");
        }
    }

    private void requireStudent() throws DiscussionException {
        requireSignedIn();
        if (!session.isStudent()) {
            throw new DiscussionException("Only a student may perform this action.");
        }
    }
}
