package com.onlinediscussion.db;

import com.onlinediscussion.core.DiscussionException;
import com.onlinediscussion.core.Role;
import com.onlinediscussion.model.Archive;
import com.onlinediscussion.model.Course;
import com.onlinediscussion.model.DateTimeFormat;
import com.onlinediscussion.model.Discussion;
import com.onlinediscussion.model.Image;
import com.onlinediscussion.model.Message;
import com.onlinediscussion.model.Student;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * JDBC implementation of {@link DBInterface} against the MySQL schema that the project
 * already uses.
 *
 * <p>The schema follows the professor's rubric (see {@code database/design.md}), so the table
 * and column names keep their original spelling, including {@code Dicussion} and
 * {@code ReuqestToCreateDiscussion}.
 */
public class MySQLDBInterface implements DBInterface, AutoCloseable {

    /** The course that the seeded discussions belong to. */
    public static final String DEFAULT_COURSE_NUMBER = "SWE-101";
    public static final String DEFAULT_COURSE_NAME = "Software Engineering";

    /**
     * The MySQL {@code DATETIME} text format. Timestamps are moved as plain text so that the
     * JDBC driver never shifts them into the time zone of the JVM.
     */
    private static final DateTimeFormatter SQL_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    public MySQLDBInterface(String host, int port, String database, String user, String password)
            throws DiscussionException {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.user = user;
        this.password = password;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new DiscussionException("The MySQL JDBC driver is not on the classpath.", e);
        }
    }

    /** Opens the connection. Safe to call more than once. */
    public void connect() throws DiscussionException {
        if (connection != null) {
            return;
        }
        try {
            Properties info = new Properties();
            info.setProperty("user", user);
            info.setProperty("password", password);
            info.setProperty("characterEncoding", "UTF-8");
            connection = DriverManager.getConnection(url, info);
        } catch (SQLException e) {
            throw new DiscussionException("Could not connect to the database: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Nothing useful can be done while shutting down.
            }
            connection = null;
        }
    }

    private Connection requireConnection() throws DiscussionException {
        if (connection == null) {
            connect();
        }
        return connection;
    }

    // ==================================================================
    // Students
    // ==================================================================

    @Override
    public void storeStudent(Student s) throws DiscussionException {
        if (s == null) {
            throw new DiscussionException("A student is required.");
        }
        if (isBlank(s.getStudentID())) {
            throw new DiscussionException("A student identifier is required.");
        }
        // The student table holds a foreign key on the user name, so the credential has to
        // exist before the student row can point at it.
        if (!isBlank(s.getUsername())) {
            ensureCredential(s.getUsername());
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "INSERT INTO StudentRecord (StudentID, Firstname, Lastname, EmailAddress, Username) "
                            + "VALUES (?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE Firstname = VALUES(Firstname), "
                            + "Lastname = VALUES(Lastname), EmailAddress = VALUES(EmailAddress), "
                            + "Username = VALUES(Username)");
            statement.setInt(1, Integer.parseInt(s.getStudentID().trim()));
            statement.setString(2, nullToEmpty(s.getFirstname()));
            statement.setString(3, nullToEmpty(s.getLastname()));
            statement.setString(4, studentEmail(s));
            if (isBlank(s.getUsername())) {
                statement.setNull(5, java.sql.Types.VARCHAR);
            } else {
                statement.setString(5, s.getUsername());
            }
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not store the student: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + s.getStudentID(), e);
        }
    }

    /**
     * The rubric schema requires an e-mail address on every student row. When the caller did not
     * supply one, a deterministic placeholder is derived from the student identifier.
     */
    private String studentEmail(Student s) {
        if (s.getUsername() != null && !s.getUsername().isBlank()) {
            return s.getUsername() + "@campus.edu";
        }
        return "student" + s.getStudentID() + "@campus.edu";
    }

    @Override
    public Student retrieveStudentByID(String id) throws DiscussionException {
        if (isBlank(id)) {
            return null;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT s.StudentID, s.Firstname, s.Lastname, s.Username FROM StudentRecord s "
                            + "WHERE s.StudentID = ?");
            statement.setInt(1, Integer.parseInt(id.trim()));
            ResultSet results = statement.executeQuery();
            Student student = null;
            if (results.next()) {
                student = mapStudent(results);
            }
            results.close();
            statement.close();
            return student;
        } catch (SQLException e) {
            throw new DiscussionException("Could not retrieve the student: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + id, e);
        }
    }

    @Override
    public Student retrieveStudentByUsername(String uname) throws DiscussionException {
        if (isBlank(uname)) {
            return null;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT s.StudentID, s.Firstname, s.Lastname, s.Username FROM StudentRecord s "
                            + "WHERE s.Username = ?");
            statement.setString(1, uname.trim());
            ResultSet results = statement.executeQuery();
            Student student = null;
            if (results.next()) {
                student = mapStudent(results);
            }
            results.close();
            statement.close();
            return student;
        } catch (SQLException e) {
            throw new DiscussionException("Could not retrieve the student: " + e.getMessage(), e);
        }
    }

    private Student mapStudent(ResultSet results) throws SQLException {
        int id = results.getInt("StudentID");
        return new Student(
                results.getString("Firstname"),
                results.getString("Lastname"),
                results.getString("Username"),
                String.valueOf(id));
    }

    @Override
    public void removeStudentByID(String id) throws DiscussionException {
        Student student = retrieveStudentByID(id);
        if (student == null) {
            throw new DiscussionException("No student has the identifier " + id + ".");
        }
        removeStudentInternal(student);
    }

    @Override
    public void removeStudentByUsername(String uname) throws DiscussionException {
        Student student = retrieveStudentByUsername(uname);
        if (student == null) {
            throw new DiscussionException("No student has the user name " + uname + ".");
        }
        removeStudentInternal(student);
    }

    /**
     * Deletes a student together with every dependent row. The discussion messages stay in
     * place because the rubric keeps them by user name, not by student identifier.
     */
    private void removeStudentInternal(Student student) throws DiscussionException {
        Connection conn = requireConnection();
        try {
            conn.setAutoCommit(false);
            deleteByStudentId(conn, "EnrollQueue", student.getStudentID());
            deleteByStudentId(conn, "RemoveStudentQueue", student.getStudentID());
            deleteByStudentId(conn, "ReuqestToCreateDiscussion", student.getStudentID());
            deleteByStudentId(conn, "RequestToRetrieveDiscussion", student.getStudentID());
            deleteByStudentId(conn, "course_student", student.getStudentID());
            try (PreparedStatement statement = conn.prepareStatement(
                    "DELETE FROM StudentRecord WHERE StudentID = ?")) {
                statement.setInt(1, Integer.parseInt(student.getStudentID()));
                statement.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DiscussionException("Could not remove the student: " + e.getMessage(), e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    /**
     * Deletes the rows of a queue table that belong to one student.
     *
     * <p>The queue tables use the rubric spelling {@code StudentID}. The enrolment table
     * {@code course_student} uses {@code student_id} instead, so it is handled separately.
     */
    private void deleteByStudentId(Connection conn, String table, String studentId) throws SQLException {
        if ("course_student".equals(table)) {
            try (PreparedStatement statement = conn.prepareStatement(
                    "DELETE FROM course_student WHERE student_id = ?")) {
                statement.setInt(1, Integer.parseInt(studentId));
                statement.executeUpdate();
            }
            return;
        }
        try (PreparedStatement statement = conn.prepareStatement("DELETE FROM " + table + " WHERE StudentID = ?")) {
            statement.setInt(1, Integer.parseInt(studentId));
            statement.executeUpdate();
        }
    }

    // ==================================================================
    // Discussions
    // ==================================================================

    @Override
    public String storeDiscussion(Discussion d) throws DiscussionException {
        if (d == null) {
            throw new DiscussionException("A discussion is required.");
        }
        if (isBlank(d.getTopic())) {
            throw new DiscussionException("A discussion needs a topic.");
        }
        Connection conn = requireConnection();
        try {
            conn.setAutoCommit(false);
            String courseNumber = DEFAULT_COURSE_NUMBER;
            try (PreparedStatement lookup = conn.prepareStatement(
                    "SELECT course_number FROM course WHERE course_name = ? LIMIT 1")) {
                lookup.setString(1, DEFAULT_COURSE_NAME);
                try (ResultSet results = lookup.executeQuery()) {
                    if (results.next()) {
                        courseNumber = results.getString(1);
                    }
                }
            }

            LocalDateTime created = DateTimeFormat.parse(d.getDateAndTime());
            if (created == null) {
                created = LocalDateTime.now(ZoneOffset.UTC);
            }

            // Dicussion.DateAndTimeOfCreation carries a unique index, so two discussions must
            // never share the same second. Move forward until a free slot is found.
            String id = null;
            for (int attempt = 0; attempt < 120 && id == null; attempt++) {
                try (PreparedStatement statement = conn.prepareStatement(
                        "INSERT INTO Dicussion (CourseName, CourseNumber, DateAndTimeOfCreation, Topic) "
                                + "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, DEFAULT_COURSE_NAME);
                    statement.setString(2, courseNumber);
                    statement.setString(3, SQL_DATE_TIME.format(created.plusSeconds(attempt)));
                    statement.setString(4, d.getTopic());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The database did not return a discussion identifier.");
                        }
                        id = String.valueOf(keys.getInt(1));
                    }
                } catch (SQLException e) {
                    if (isDuplicateKey(e)) {
                        continue;
                    }
                    throw e;
                }
            }
            if (id == null) {
                throw new SQLException("Could not find a free creation time for the discussion.");
            }
            conn.commit();
            return id;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DiscussionException("Could not store the discussion: " + e.getMessage(), e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    @Override
    public Discussion retrieveDiscussionByID(String id) throws DiscussionException {
        if (isBlank(id)) {
            return null;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT DiscussionID, Topic, DateAndTimeOfCreation FROM Dicussion WHERE DiscussionID = ?");
            statement.setInt(1, Integer.parseInt(id.trim()));
            ResultSet results = statement.executeQuery();
            Discussion discussion = null;
            if (results.next()) {
                discussion = mapDiscussion(results);
            }
            results.close();
            statement.close();
            if (discussion != null) {
                discussion.setMessages(retrieveMessagesByDiscussion(discussion.getDiscussionID()));
            }
            return discussion;
        } catch (SQLException e) {
            throw new DiscussionException("Could not retrieve the discussion: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The discussion identifier must be a number: " + id, e);
        }
    }

    private Discussion mapDiscussion(ResultSet results) throws SQLException {
        return new Discussion(
                String.valueOf(results.getInt("DiscussionID")),
                results.getString("Topic"),
                toDisplayTime(results.getString("DateAndTimeOfCreation")));
    }

    @Override
    public List<Discussion> retrieveDiscussionByTopic(String topic) throws DiscussionException {
        List<Discussion> matches = new ArrayList<>();
        if (isBlank(topic)) {
            return matches;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT DiscussionID, Topic, DateAndTimeOfCreation FROM Dicussion "
                            + "WHERE Topic = ? ORDER BY DateAndTimeOfCreation DESC");
            statement.setString(1, topic.trim());
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                Discussion discussion = mapDiscussion(results);
                discussion.setMessages(retrieveMessagesByDiscussion(discussion.getDiscussionID()));
                matches.add(discussion);
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not search the discussions: " + e.getMessage(), e);
        }
        return matches;
    }

    @Override
    public List<Discussion> retrieveActiveDiscussions() throws DiscussionException {
        List<Discussion> discussions = new ArrayList<>();
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT d.DiscussionID, d.Topic, d.DateAndTimeOfCreation FROM Dicussion d "
                            + "LEFT JOIN ArchivedDicussions a ON a.DiscussionID = d.DiscussionID "
                            + "WHERE a.DiscussionID IS NULL ORDER BY d.DateAndTimeOfCreation DESC");
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                Discussion discussion = mapDiscussion(results);
                discussion.setMessages(retrieveMessagesByDiscussion(discussion.getDiscussionID()));
                discussions.add(discussion);
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the discussions: " + e.getMessage(), e);
        }
        return discussions;
    }

    @Override
    public void archiveDiscussion(String id) throws DiscussionException {
        if (isBlank(id)) {
            throw new DiscussionException("A discussion identifier is required.");
        }
        Discussion discussion = retrieveDiscussionByID(id);
        if (discussion == null) {
            throw new DiscussionException("No discussion has the identifier " + id + ".");
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "INSERT INTO ArchivedDicussions (DiscussionID, FileName, FileType, FileData) "
                            + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE FileName = VALUES(FileName)");
            statement.setInt(1, Integer.parseInt(discussion.getDiscussionID()));
            statement.setString(2, "discussion-" + discussion.getDiscussionID() + ".txt");
            statement.setString(3, "txt");
            statement.setBytes(4, buildArchiveText(discussion).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not archive the discussion: " + e.getMessage(), e);
        }
    }

    /** Renders a discussion as plain text, the same way the HTTP server stores it. */
    private String buildArchiveText(Discussion discussion) {
        StringBuilder text = new StringBuilder();
        text.append("Course: ").append(DEFAULT_COURSE_NAME).append('\n');
        text.append("Topic: ").append(discussion.getTopic()).append('\n');
        text.append("Date and time of creation: ").append(discussion.getDateAndTime()).append('\n');
        text.append('\n');
        text.append("Entries\n");
        text.append("-------\n");
        for (Message message : discussion.getMessages()) {
            text.append(message.getPostedBy()).append(" | ").append(message.getDateAndTime()).append('\n');
            if (message.getTextualContents() != null) {
                text.append(message.getTextualContents()).append('\n');
            }
            if (message.getImageContents() != null) {
                text.append("[Picture: ").append(message.getImageContents().getName()).append("]\n");
            }
            text.append('\n');
        }
        return text.toString();
    }

    // ==================================================================
    // Messages
    // ==================================================================

    @Override
    public String addMessageToDiscussion(String discussionID, Message message) throws DiscussionException {
        if (isBlank(discussionID)) {
            throw new DiscussionException("A discussion identifier is required.");
        }
        if (message == null) {
            throw new DiscussionException("A message is required.");
        }
        if (isBlank(message.getTextualContents()) && message.getImageContents() == null) {
            throw new DiscussionException("A message needs text or an image.");
        }
        if (isBlank(message.getPostedBy())) {
            throw new DiscussionException("A message needs an author.");
        }
        Discussion discussion = retrieveDiscussionByID(discussionID);
        if (discussion == null) {
            throw new DiscussionException("No discussion has the identifier " + discussionID + ".");
        }
        if (isArchived(discussion.getDiscussionID())) {
            throw new DiscussionException("Archived discussions cannot receive new messages.");
        }

        Connection conn = requireConnection();
        try {
            conn.setAutoCommit(false);
            String messageId;
            try (PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO Message (DateAndTime, Username, Contents, ImageName, ImageType, ImageData) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime when = DateTimeFormat.parse(message.getDateAndTime());
                if (when == null) {
                    when = LocalDateTime.now(ZoneOffset.UTC);
                }
                statement.setString(1, SQL_DATE_TIME.format(when));
                statement.setString(2, message.getPostedBy());
                statement.setString(3, message.getTextualContents() == null ? "" : message.getTextualContents());
                Image image = message.getImageContents();
                if (image == null) {
                    statement.setNull(4, java.sql.Types.VARCHAR);
                    statement.setNull(5, java.sql.Types.VARCHAR);
                    statement.setNull(6, java.sql.Types.BLOB);
                } else {
                    statement.setString(4, image.getName());
                    statement.setString(5, image.getType());
                    statement.setBytes(6, image.getData());
                }
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("The database did not return a message identifier.");
                    }
                    messageId = String.valueOf(keys.getInt(1));
                }
            }

            try (PreparedStatement statement = conn.prepareStatement(
                    "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(EntryNo, '.', -1) AS UNSIGNED)), 0) + 1 AS nextNo "
                            + "FROM DiscussionToMessage WHERE DicussionID = ?")) {
                statement.setInt(1, Integer.parseInt(discussion.getDiscussionID()));
                try (ResultSet results = statement.executeQuery()) {
                    int next = results.next() ? results.getInt(1) : 1;
                    try (PreparedStatement link = conn.prepareStatement(
                            "INSERT INTO DiscussionToMessage (DicussionID, MessageID, EntryNo) VALUES (?, ?, ?)")) {
                        link.setInt(1, Integer.parseInt(discussion.getDiscussionID()));
                        link.setInt(2, Integer.parseInt(messageId));
                        link.setString(3, discussion.getDiscussionID() + "." + next);
                        link.executeUpdate();
                    }
                }
            }
            conn.commit();
            return messageId;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DiscussionException("Could not post the message: " + e.getMessage(), e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    @Override
    public void removeMessageFromDiscussion(String discussionID, Message message) throws DiscussionException {
        if (message == null || isBlank(message.getMessageID())) {
            throw new DiscussionException("A message with an identifier is required.");
        }
        Connection conn = requireConnection();
        try {
            conn.setAutoCommit(false);
            if (!isBlank(discussionID)) {
                try (PreparedStatement statement = conn.prepareStatement(
                        "DELETE FROM DiscussionToMessage WHERE DicussionID = ? AND MessageID = ?")) {
                    statement.setInt(1, Integer.parseInt(discussionID.trim()));
                    statement.setInt(2, Integer.parseInt(message.getMessageID()));
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = conn.prepareStatement(
                        "DELETE FROM DiscussionToMessage WHERE MessageID = ?")) {
                    statement.setInt(1, Integer.parseInt(message.getMessageID()));
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = conn.prepareStatement(
                    "DELETE FROM Message WHERE MessageID = ?")) {
                statement.setInt(1, Integer.parseInt(message.getMessageID()));
                statement.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DiscussionException("Could not remove the message: " + e.getMessage(), e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    @Override
    public List<Message> retrieveMessagesByDiscussion(String discussionID) throws DiscussionException {
        List<Message> messages = new ArrayList<>();
        if (isBlank(discussionID)) {
            return messages;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT m.MessageID, m.DateAndTime, m.Username, m.Contents, "
                            + "m.ImageName, m.ImageType, m.ImageData, dm.DicussionID "
                            + "FROM DiscussionToMessage dm JOIN Message m ON m.MessageID = dm.MessageID "
                            + "WHERE dm.DicussionID = ? ORDER BY dm.EntryNo");
            statement.setInt(1, Integer.parseInt(discussionID.trim()));
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                messages.add(mapMessage(results));
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the messages: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The discussion identifier must be a number: " + discussionID, e);
        }
        return messages;
    }

    @Override
    public Message retrieveMessageByID(String discussionID, String messageID) throws DiscussionException {
        if (isBlank(messageID)) {
            return null;
        }
        List<Message> messages = retrieveMessagesByDiscussion(discussionID);
        for (Message message : messages) {
            if (messageID.trim().equals(message.getMessageID())) {
                return message;
            }
        }
        return null;
    }

    @Override
    public List<Message> retrieveMessagesByUsername(String discussionID, String uname) throws DiscussionException {
        List<Message> matches = new ArrayList<>();
        if (isBlank(uname)) {
            return matches;
        }
        for (Message message : retrieveMessagesByDiscussion(discussionID)) {
            if (uname.trim().equals(message.getPostedBy())) {
                matches.add(message);
            }
        }
        return matches;
    }

    private Message mapMessage(ResultSet results) throws SQLException {
        Image image = null;
        String imageName = results.getString("ImageName");
        if (imageName != null) {
            image = new Image(imageName, results.getString("ImageType"), results.getBytes("ImageData"));
        }
        return new Message(
                String.valueOf(results.getInt("MessageID")),
                String.valueOf(results.getInt("DicussionID")),
                toDisplayTime(results.getString("DateAndTime")),
                results.getString("Username"),
                results.getString("Contents"),
                image);
    }

    /**
     * Turns a MySQL {@code DATETIME} text value into the shared display format.
     *
     * <p>The value is read as text so that the time zone of the JVM cannot shift it.
     */
    private static String toDisplayTime(String sqlDateTime) {
        if (sqlDateTime == null || sqlDateTime.isBlank()) {
            return null;
        }
        String text = sqlDateTime.trim();
        if (text.length() > 19) {
            text = text.substring(0, 19);
        }
        try {
            return DateTimeFormat.format(LocalDateTime.parse(text, SQL_DATE_TIME));
        } catch (RuntimeException e) {
            return text;
        }
    }

    /** Tells whether a SQLException was caused by a unique constraint violation. */
    private static boolean isDuplicateKey(SQLException e) {
        return e.getErrorCode() == 1062 || "23000".equals(e.getSQLState());
    }

    private boolean isArchived(String discussionID) throws DiscussionException {
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT DiscussionID FROM ArchivedDicussions WHERE DiscussionID = ?");
            statement.setInt(1, Integer.parseInt(discussionID));
            ResultSet results = statement.executeQuery();
            boolean archived = results.next();
            results.close();
            statement.close();
            return archived;
        } catch (SQLException e) {
            throw new DiscussionException("Could not check the archive: " + e.getMessage(), e);
        }
    }

    // ==================================================================
    // Courses
    // ==================================================================

    @Override
    public void storeCourse(Course c) throws DiscussionException {
        if (c == null) {
            throw new DiscussionException("A course is required.");
        }
        if (isBlank(c.getCourseNumber())) {
            throw new DiscussionException("A course number is required.");
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "INSERT INTO course (course_number, course_name, instructor_username) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), "
                            + "instructor_username = VALUES(instructor_username)");
            statement.setString(1, c.getCourseNumber().trim());
            statement.setString(2, c.getCourseName() == null ? c.getCourseNumber() : c.getCourseName());
            statement.setNull(3, java.sql.Types.VARCHAR);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not store the course: " + e.getMessage(), e);
        }
    }

    @Override
    public Course retrieveCourseByNumber(String courseNumber) throws DiscussionException {
        if (isBlank(courseNumber)) {
            return null;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT course_number, course_name FROM course WHERE course_number = ?");
            statement.setString(1, courseNumber.trim());
            ResultSet results = statement.executeQuery();
            Course course = null;
            if (results.next()) {
                course = new Course(results.getString("course_name"), results.getString("course_number"));
            }
            results.close();
            statement.close();
            if (course != null) {
                course.setStudents(retrieveEnrolledStudents(course.getCourseNumber()));
                course.setDiscussions(retrieveCourseDiscussions(course.getCourseNumber()));
            }
            return course;
        } catch (SQLException e) {
            throw new DiscussionException("Could not retrieve the course: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Course> retrieveAllCourses() throws DiscussionException {
        List<Course> courses = new ArrayList<>();
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT course_number, course_name FROM course ORDER BY course_number");
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                courses.add(new Course(results.getString("course_name"), results.getString("course_number")));
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the courses: " + e.getMessage(), e);
        }
        for (Course course : courses) {
            course.setStudents(retrieveEnrolledStudents(course.getCourseNumber()));
            course.setDiscussions(retrieveCourseDiscussions(course.getCourseNumber()));
        }
        return courses;
    }

    private List<Discussion> retrieveCourseDiscussions(String courseNumber) throws DiscussionException {
        List<Discussion> discussions = new ArrayList<>();
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT d.DiscussionID, d.Topic, d.DateAndTimeOfCreation FROM Dicussion d "
                            + "LEFT JOIN ArchivedDicussions a ON a.DiscussionID = d.DiscussionID "
                            + "WHERE d.CourseNumber = ? AND a.DiscussionID IS NULL "
                            + "ORDER BY d.DateAndTimeOfCreation DESC");
            statement.setString(1, courseNumber.trim());
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                Discussion discussion = mapDiscussion(results);
                discussion.setMessages(retrieveMessagesByDiscussion(discussion.getDiscussionID()));
                discussions.add(discussion);
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the course discussions: " + e.getMessage(), e);
        }
        return discussions;
    }

    /** Counts the discussions of a course that are not archived. */
    public int countActiveDiscussions(String courseNumber) throws DiscussionException {
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT COUNT(*) AS n FROM Dicussion d "
                            + "LEFT JOIN ArchivedDicussions a ON a.DiscussionID = d.DiscussionID "
                            + "WHERE d.CourseNumber = ? AND a.DiscussionID IS NULL");
            statement.setString(1, courseNumber.trim());
            ResultSet results = statement.executeQuery();
            int count = results.next() ? results.getInt(1) : 0;
            results.close();
            statement.close();
            return count;
        } catch (SQLException e) {
            throw new DiscussionException("Could not count the course discussions: " + e.getMessage(), e);
        }
    }

    @Override
    public void enrollStudent(String courseNumber, String studentID) throws DiscussionException {
        if (isBlank(courseNumber) || isBlank(studentID)) {
            throw new DiscussionException("A course number and a student identifier are required.");
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "INSERT INTO course_student (course_number, student_id) VALUES (?, ?) "
                            + "ON DUPLICATE KEY UPDATE enrolled_at = enrolled_at");
            statement.setString(1, courseNumber.trim());
            statement.setInt(2, Integer.parseInt(studentID.trim()));
            statement.executeUpdate();
            statement.close();
            clearRequest(studentID, "EnrollQueue");
        } catch (SQLException e) {
            throw new DiscussionException("Could not enrol the student: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + studentID, e);
        }
    }

    @Override
    public void unenrollStudent(String courseNumber, String studentID) throws DiscussionException {
        if (isBlank(courseNumber) || isBlank(studentID)) {
            throw new DiscussionException("A course number and a student identifier are required.");
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "DELETE FROM course_student WHERE course_number = ? AND student_id = ?");
            statement.setString(1, courseNumber.trim());
            statement.setInt(2, Integer.parseInt(studentID.trim()));
            statement.executeUpdate();
            statement.close();
            clearRequest(studentID, "RemoveStudentQueue");
        } catch (SQLException e) {
            throw new DiscussionException("Could not remove the student from the course: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + studentID, e);
        }
    }

    @Override
    public List<Student> retrieveEnrolledStudents(String courseNumber) throws DiscussionException {
        List<Student> students = new ArrayList<>();
        if (isBlank(courseNumber)) {
            return students;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT StudentID, Firstname, Lastname, Username FROM StudentRecord "
                            + "WHERE Username IS NOT NULL ORDER BY StudentID");
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                students.add(mapStudent(results));
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the enrolled students: " + e.getMessage(), e);
        }
        return students;
    }

    @Override
    public boolean isStudentEnrolled(String courseNumber, String studentID) throws DiscussionException {
        if (isBlank(courseNumber) || isBlank(studentID)) {
            return false;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT student_id FROM course_student WHERE course_number = ? AND student_id = ?");
            statement.setString(1, courseNumber.trim());
            statement.setInt(2, Integer.parseInt(studentID.trim()));
            ResultSet results = statement.executeQuery();
            boolean enrolled = results.next();
            results.close();
            statement.close();
            return enrolled;
        } catch (SQLException e) {
            throw new DiscussionException("Could not check the enrolment: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + studentID, e);
        }
    }

    // ==================================================================
    // Requests
    // ==================================================================

    @Override
    public void storeEnrollmentRequest(String studentID) throws DiscussionException {
        storeRequest(studentID, "EnrollQueue");
    }

    @Override
    public void storeRemovalRequest(String studentID) throws DiscussionException {
        storeRequest(studentID, "RemoveStudentQueue");
    }

    private void storeRequest(String studentID, String table) throws DiscussionException {
        if (isBlank(studentID)) {
            throw new DiscussionException("A student identifier is required.");
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "INSERT INTO " + table + " (StudentID) VALUES (?) ON DUPLICATE KEY UPDATE RequestedAt = RequestedAt");
            statement.setInt(1, Integer.parseInt(studentID.trim()));
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not store the request: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + studentID, e);
        }
    }

    @Override
    public List<Student> retrieveEnrollmentRequests() throws DiscussionException {
        return retrieveQueueStudents("EnrollQueue");
    }

    @Override
    public List<Student> retrieveRemovalRequests() throws DiscussionException {
        return retrieveQueueStudents("RemoveStudentQueue");
    }

    private List<Student> retrieveQueueStudents(String table) throws DiscussionException {
        List<Student> students = new ArrayList<>();
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT s.StudentID, s.Firstname, s.Lastname, s.Username FROM " + table + " q "
                            + "JOIN StudentRecord s ON s.StudentID = q.StudentID ORDER BY q.RequestedAt");
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                students.add(mapStudent(results));
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the requests: " + e.getMessage(), e);
        }
        return students;
    }

    @Override
    public void clearEnrollmentRequest(String studentID) throws DiscussionException {
        clearRequest(studentID, "EnrollQueue");
    }

    @Override
    public void clearRemovalRequest(String studentID) throws DiscussionException {
        clearRequest(studentID, "RemoveStudentQueue");
    }

    private void clearRequest(String studentID, String table) throws DiscussionException {
        if (isBlank(studentID)) {
            return;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "DELETE FROM " + table + " WHERE StudentID = ?");
            statement.setInt(1, Integer.parseInt(studentID.trim()));
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not clear the request: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + studentID, e);
        }
    }

    // ==================================================================
    // Archive
    // ==================================================================

    @Override
    public List<Discussion> retrieveArchivedDiscussions() throws DiscussionException {
        List<Discussion> discussions = new ArrayList<>();
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT d.DiscussionID, d.Topic, d.DateAndTimeOfCreation FROM Dicussion d "
                            + "JOIN ArchivedDicussions a ON a.DiscussionID = d.DiscussionID "
                            + "ORDER BY d.DateAndTimeOfCreation DESC");
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                Discussion discussion = mapDiscussion(results);
                discussion.setMessages(retrieveMessagesByDiscussion(discussion.getDiscussionID()));
                discussions.add(discussion);
            }
            results.close();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the archive: " + e.getMessage(), e);
        }
        return discussions;
    }

    @Override
    public Archive retrieveArchive() throws DiscussionException {
        return new Archive(retrieveArchivedDiscussions());
    }

    // ==================================================================
    // Credentials
    // ==================================================================

    @Override
    public String verifyCredentials(String username, String password) throws DiscussionException {
        if (isBlank(username) || password == null) {
            return null;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT Password FROM LoginCredential WHERE Username = ?");
            statement.setString(1, username.trim());
            ResultSet results = statement.executeQuery();
            String role = null;
            if (results.next()) {
                String hash = results.getString("Password");
                if (hash != null && BCrypt.verifyer().verify(password.toCharArray(), hash.toCharArray()).verified) {
                    role = "STUDENT";
                }
            }
            results.close();
            statement.close();
            if (role == null) {
                return null;
            }
            PreparedStatement roleStatement = requireConnection().prepareStatement(
                    "SELECT Role FROM LoginCredential WHERE Username = ?");
            roleStatement.setString(1, username.trim());
            ResultSet roleResults = roleStatement.executeQuery();
            String actualRole = roleResults.next() ? roleResults.getString("Role") : "STUDENT";
            roleResults.close();
            roleStatement.close();
            return actualRole == null ? "STUDENT" : actualRole;
        } catch (SQLException e) {
            throw new DiscussionException("Could not verify the credentials: " + e.getMessage(), e);
        }
    }

    @Override
    public void storeCredential(String username, String password, String role) throws DiscussionException {
        if (isBlank(username) || password == null) {
            throw new DiscussionException("A user name and a password are required.");
        }
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "INSERT INTO LoginCredential (Username, Password, Role) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE Password = VALUES(Password), Role = VALUES(Role)");
            statement.setString(1, username.trim());
            statement.setString(2, hash);
            statement.setString(3, Role.fromDatabase(role).name());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not store the credential: " + e.getMessage(), e);
        }
    }

    @Override
    public String ensureCredential(String username) throws DiscussionException {
        if (isBlank(username)) {
            throw new DiscussionException("A user name is required.");
        }
        String existing = retrieveRole(username.trim());
        if (existing != null) {
            return null;
        }
        String password = com.onlinediscussion.core.PasswordGenerator.generate();
        storeCredential(username.trim(), password, "STUDENT");
        return password;
    }

    @Override
    public void assignUsernameToStudent(String studentID, String username) throws DiscussionException {
        if (isBlank(studentID) || isBlank(username)) {
            throw new DiscussionException("A student identifier and a user name are required.");
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "UPDATE StudentRecord SET Username = ? WHERE StudentID = ?");
            statement.setString(1, username.trim());
            statement.setInt(2, Integer.parseInt(studentID.trim()));
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new DiscussionException("Could not assign the user name: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiscussionException("The student identifier must be a number: " + studentID, e);
        }
    }

    @Override
    public String retrieveRole(String username) throws DiscussionException {
        if (isBlank(username)) {
            return null;
        }
        try {
            PreparedStatement statement = requireConnection().prepareStatement(
                    "SELECT Role FROM LoginCredential WHERE Username = ?");
            statement.setString(1, username.trim());
            ResultSet results = statement.executeQuery();
            String role = results.next() ? results.getString("Role") : null;
            results.close();
            statement.close();
            return role;
        } catch (SQLException e) {
            throw new DiscussionException("Could not load the role: " + e.getMessage(), e);
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // The original failure is the one worth reporting.
        }
    }

    private static void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
            // The original failure is the one worth reporting.
        }
    }
}
