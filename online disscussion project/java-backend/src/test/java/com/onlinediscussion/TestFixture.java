package com.onlinediscussion;

import com.onlinediscussion.core.DiscussionException;
import com.onlinediscussion.core.OnlineDiscussionMain;
import com.onlinediscussion.core.Role;
import com.onlinediscussion.core.Session;
import com.onlinediscussion.db.MySQLDBInterface;
import com.onlinediscussion.model.Course;
import com.onlinediscussion.model.Discussion;
import com.onlinediscussion.model.Image;
import com.onlinediscussion.model.Message;
import com.onlinediscussion.model.Student;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Shared fixture for the test suite.
 *
 * <p>The tests run against the real MySQL database that the project already uses. Each test
 * works on its own rows, identified by a unique prefix, and removes them afterwards so that
 * repeated runs stay independent of each other and leave the seeded data untouched.
 */
public final class TestFixture {

    /** The instructor account that the assignment assumes exists at installation time. */
    public static final String INSTRUCTOR_USERNAME = "drKasi";
    public static final String INSTRUCTOR_PASSWORD = "Instructor@123";

    /** A student account that is guaranteed to exist in the seeded data. */
    public static final String STUDENT_USERNAME = "jdoe23";
    public static final String STUDENT_PASSWORD = "Student@123";
    public static final String STUDENT_ID = "1";

    private static final String PREFIX = "jtest";

    private MySQLDBInterface database;
    private OnlineDiscussionMain api;

    /**
     * Connects to the database and prepares a clean course that the tests can use.
     */
    public void setUp() throws Exception {
        database = new MySQLDBInterface(host(), port(), name(), user(), password());
        database.connect();
        api = new OnlineDiscussionMain(database);
        resetCourse();
    }

    /** Releases the database connection. */
    public void tearDown() {
        if (database != null) {
            try {
                cleanup();
            } catch (SQLException ignored) {
                // The rows are removed best effort.
            }
            database.close();
        }
    }

    public OnlineDiscussionMain api() {
        return api;
    }

    public MySQLDBInterface database() {
        return database;
    }

    /** The course number that the tests enrol students in. */
    public String courseNumber() {
        return "SWE-101";
    }

    /** Signs the instructor in and returns the session. */
    public Session loginInstructor() throws DiscussionException {
        if (!api.login(INSTRUCTOR_USERNAME, INSTRUCTOR_PASSWORD)) {
            throw new IllegalStateException("The instructor account could not be signed in.");
        }
        return api.getSession();
    }

    /** Signs the seeded student in and returns the session. */
    public Session loginStudent() throws DiscussionException {
        if (!api.login(STUDENT_USERNAME, STUDENT_PASSWORD)) {
            throw new IllegalStateException("The student account could not be signed in.");
        }
        return api.getSession();
    }

    /** Signs the instructor out. */
    public void logout() {
        api.logout();
    }

    /**
     * Builds a unique student identifier for a test.
     *
     * <p>The rubric schema stores the identifier as an {@code INT}, so the value is numeric.
     * The range above the seeded identifiers keeps the test rows apart from the demo data.
     */
    public static String uniqueId(String tag) {
        return String.valueOf(900000 + (System.nanoTime() % 90000));
    }

    /** Builds a unique user name for a test. */
    public static String uniqueUsername(String tag) {
        return PREFIX + tag + (System.nanoTime() % 100000);
    }

    /** Creates a student row and returns it. */
    public Student createStudent(String tag, String firstname, String lastname) throws DiscussionException {
        Student student = new Student(firstname, lastname, uniqueUsername(tag), uniqueId(tag));
        database.storeStudent(student);
        return student;
    }

    /** Creates a discussion with a unique topic and returns it with its assigned identifier. */
    public Discussion createDiscussion(String topic) throws DiscussionException {
        Discussion discussion = new Discussion(null, PREFIX + topic + "-" + uniqueId("t"), null);
        String id = database.storeDiscussion(discussion);
        return database.retrieveDiscussionByID(id);
    }

    /** Removes every row that the tests created. */
    private void cleanup() throws SQLException {
        try (Connection connection = rawConnection()) {
            // The test marker can sit at the start of the topic or inside it, so both ends
            // are matched. That keeps the cleanup working for every version of the topic.
            String topicPattern = "%" + PREFIX + "%";

            // The tests post messages as the seeded users, so the messages of a test
            // discussion cannot be found by user name. Their identifiers are collected
            // before the links are dropped, otherwise the rows would be left behind.
            List<Integer> messageIds = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT dm.MessageID FROM DiscussionToMessage dm "
                            + "JOIN Dicussion d ON d.DiscussionID = dm.DicussionID "
                            + "WHERE d.Topic LIKE ?")) {
                statement.setString(1, topicPattern);
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        messageIds.add(results.getInt(1));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT m.MessageID FROM Message m WHERE m.Username LIKE ?")) {
                statement.setString(1, PREFIX + "%");
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        messageIds.add(results.getInt(1));
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM DiscussionToMessage WHERE DicussionID IN "
                            + "(SELECT DiscussionID FROM Dicussion WHERE Topic LIKE ?)")) {
                statement.setString(1, topicPattern);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM DiscussionToMessage WHERE MessageID IN ("
                            + "SELECT m.MessageID FROM Message m WHERE m.Username LIKE ?)")) {
                statement.setString(1, PREFIX + "%");
                statement.executeUpdate();
            }
            for (Integer messageId : messageIds) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM Message WHERE MessageID = ?")) {
                    statement.setInt(1, messageId);
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM ArchivedDicussions WHERE DiscussionID IN "
                            + "(SELECT DiscussionID FROM Dicussion WHERE Topic LIKE ?)")) {
                statement.setString(1, topicPattern);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM Dicussion WHERE Topic LIKE ?")) {
                statement.setString(1, topicPattern);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM course_student WHERE student_id IN "
                            + "(SELECT StudentID FROM StudentRecord WHERE Username LIKE ?)")) {
                statement.setString(1, PREFIX + "%");
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM EnrollQueue WHERE StudentID IN "
                            + "(SELECT StudentID FROM StudentRecord WHERE Username LIKE ?)")) {
                statement.setString(1, PREFIX + "%");
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM RemoveStudentQueue WHERE StudentID IN "
                            + "(SELECT StudentID FROM StudentRecord WHERE Username LIKE ?)")) {
                statement.setString(1, PREFIX + "%");
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM StudentRecord WHERE Username LIKE ?")) {
                statement.setString(1, PREFIX + "%");
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM LoginCredential WHERE Username LIKE ?")) {
                statement.setString(1, PREFIX + "%");
                statement.executeUpdate();
            }
        }
    }

    /** Makes sure the test course exists and that no leftover test student is enrolled. */
    private void resetCourse() throws SQLException {
        try (Connection connection = rawConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO course (course_number, course_name, instructor_username) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE course_name = VALUES(course_name)")) {
                statement.setString(1, courseNumber());
                statement.setString(2, "Software Engineering");
                statement.setString(3, INSTRUCTOR_USERNAME);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM course_student WHERE course_number = ? AND student_id IN "
                            + "(SELECT StudentID FROM StudentRecord WHERE Username LIKE ?)")) {
                statement.setString(1, courseNumber());
                statement.setString(2, PREFIX + "%");
                statement.executeUpdate();
            }
        }
    }

    /** Opens a raw JDBC connection using the same settings as the production code. */
    private Connection rawConnection() throws SQLException {
        Properties info = new Properties();
        info.setProperty("user", user());
        info.setProperty("password", password());
        return DriverManager.getConnection("jdbc:mysql://" + host() + ":" + port() + "/" + name() + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true&serverTimezone=UTC", info);
    }

    /** Removes a discussion and everything that hangs off it. */
    public void dropDiscussion(String discussionID) {
        try (Connection connection = rawConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM DiscussionToMessage WHERE DicussionID = ?")) {
                statement.setInt(1, Integer.parseInt(discussionID));
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM ArchivedDicussions WHERE DiscussionID = ?")) {
                statement.setInt(1, Integer.parseInt(discussionID));
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM Dicussion WHERE DiscussionID = ?")) {
                statement.setInt(1, Integer.parseInt(discussionID));
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not drop the discussion " + discussionID, e);
        }
    }

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    static String host() {
        return pick("db.host", "MYSQL_HOST", "127.0.0.1");
    }

    static int port() {
        String value = pick("db.port", "MYSQL_PORT", "3306");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 3306;
        }
    }

    static String name() {
        return pick("db.name", "MYSQL_DATABASE", "discussion_tool");
    }

    static String user() {
        return pick("db.user", "MYSQL_USER", "root");
    }

    static String password() {
        return pick("db.password", "MYSQL_PASSWORD", "");
    }

    private static String pick(String propertyKey, String environmentKey, String fallback) {
        Path file = Path.of("db.properties");
        if (Files.exists(file)) {
            try {
                Properties properties = new Properties();
                try (var reader = Files.newBufferedReader(file)) {
                    properties.load(reader);
                }
                String value = properties.getProperty(propertyKey);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            } catch (Exception ignored) {
                // Fall through to the environment.
            }
        }
        String value = System.getenv(environmentKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback;
    }

    /** Builds a message with text only. */
    public static Message textMessage(String text) {
        Message message = new Message();
        message.setTextualContents(text);
        return message;
    }

    /** Builds a message that carries a picture. */
    public static Message imageMessage(String name, String type, byte[] data) {
        Message message = new Message();
        message.setImageContents(new Image(name, type, data));
        return message;
    }

    /** Builds a message that carries both text and a picture. */
    public static Message imageMessage(String name, String type, byte[] data, String text) {
        Message message = imageMessage(name, type, data);
        message.setTextualContents(text);
        return message;
    }

    /** Removes every row that starts with the test prefix. */
    public static void purgeTestRows() {
        try {
            TestFixture fixture = new TestFixture();
            fixture.cleanup();
        } catch (Exception ignored) {
            // Best effort.
        }
    }

    /** Runs a plain statement against the database. */
    public void execute(String sql) throws SQLException {
        try (Connection connection = rawConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
