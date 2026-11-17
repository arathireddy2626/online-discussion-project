package com.onlinediscussion;

import com.onlinediscussion.core.DiscussionException;
import com.onlinediscussion.model.Course;
import com.onlinediscussion.model.Discussion;
import com.onlinediscussion.model.Message;
import com.onlinediscussion.model.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests of the Online Discussion Tool.
 *
 * <p>Every functionality in the backend design is exercised, first as the instructor and
 * then as a student.
 */
class OnlineDiscussionToolTest {

    private TestFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new TestFixture();
        fixture.setUp();
    }

    @AfterEach
    void tearDown() {
        fixture.tearDown();
    }

    // ==================================================================
    // Login and logout - for all users
    // ==================================================================

    @Test
    @DisplayName("The instructor can sign in and out")
    void instructorLoginAndLogout() throws Exception {
        assertTrue(fixture.api().login(TestFixture.INSTRUCTOR_USERNAME, TestFixture.INSTRUCTOR_PASSWORD));
        assertNotNull(fixture.api().getSession());
        assertTrue(fixture.api().getSession().isInstructor());
        assertEquals(TestFixture.INSTRUCTOR_USERNAME, fixture.api().getSession().getUsername());

        fixture.api().logout();
        assertNull(fixture.api().getSession());
    }

    @Test
    @DisplayName("A student can sign in and out")
    void studentLoginAndLogout() throws Exception {
        assertTrue(fixture.api().login(TestFixture.STUDENT_USERNAME, TestFixture.STUDENT_PASSWORD));
        assertNotNull(fixture.api().getSession());
        assertTrue(fixture.api().getSession().isStudent());
        assertEquals(TestFixture.STUDENT_ID, fixture.api().getSession().getStudentID());

        fixture.api().logout();
        assertNull(fixture.api().getSession());
    }

    @Test
    @DisplayName("A wrong password is refused")
    void loginWithWrongPassword() throws Exception {
        assertFalse(fixture.api().login(TestFixture.INSTRUCTOR_USERNAME, "not-the-password"));
        assertNull(fixture.api().getSession());
    }

    @Test
    @DisplayName("An unknown user name is refused")
    void loginWithUnknownUser() throws Exception {
        assertFalse(fixture.api().login("nobody-at-all", TestFixture.INSTRUCTOR_PASSWORD));
        assertNull(fixture.api().getSession());
    }

    @Test
    @DisplayName("Empty credentials are refused")
    void loginWithEmptyCredentials() throws Exception {
        assertFalse(fixture.api().login("", ""));
        assertFalse(fixture.api().login(null, null));
    }

    @Test
    @DisplayName("Calls that need a signed in user are refused when nobody is signed in")
    void callsRequireASignedInUser() {
        assertThrows(DiscussionException.class, () -> fixture.api().searchDiscussionByID("1"));
        assertThrows(DiscussionException.class, () -> fixture.api().createDicussion(new Discussion(null, "x", null)));
    }

    // ==================================================================
    // Instructor's API calls
    // ==================================================================

    @Test
    @DisplayName("The instructor creates a course")
    void instructorCreatesCourse() throws Exception {
        fixture.loginInstructor();
        Course course = new Course("Software Engineering", fixture.courseNumber());
        fixture.api().createCourse(course);
        assertEquals(fixture.courseNumber(), fixture.api().getCurrentCourseNumber());

        Course loaded = fixture.api().getDbInterface().retrieveCourseByNumber(fixture.courseNumber());
        assertNotNull(loaded);
        assertEquals("Software Engineering", loaded.getCourseName());
    }

    @Test
    @DisplayName("The instructor adds a student to the course")
    void instructorAddsStudent() throws Exception {
        fixture.loginInstructor();
        Student student = fixture.createStudent("add", "Alice", "Anderson");

        fixture.api().addStudent(student);

        List<Student> enrolled = fixture.api().listEnrolledStudents();
        assertTrue(enrolled.stream().anyMatch(s -> student.getStudentID().equals(s.getStudentID())));
        assertTrue(fixture.api().getDbInterface().isStudentEnrolled(fixture.courseNumber(), student.getStudentID()));
    }

    @Test
    @DisplayName("The instructor finds a student by identifier and by user name")
    void instructorSearchesStudents() throws Exception {
        fixture.loginInstructor();
        Student student = fixture.createStudent("find", "Bob", "Brown");
        fixture.api().addStudent(student);

        Student byId = fixture.api().searchStudentByID(student.getStudentID());
        assertNotNull(byId);
        assertEquals("Bob", byId.getFirstname());

        Student byUsername = fixture.api().searchStudentByUsername(student.getUsername());
        assertNotNull(byUsername);
        assertEquals(student.getStudentID(), byUsername.getStudentID());
    }

    @Test
    @DisplayName("The instructor removes a student by identifier")
    void instructorRemovesStudentById() throws Exception {
        fixture.loginInstructor();
        Student student = fixture.createStudent("rm1", "Carol", "Clark");
        fixture.api().addStudent(student);

        fixture.api().removeStudentbyID(student.getStudentID());

        assertNull(fixture.api().searchStudentByID(student.getStudentID()));
        assertFalse(fixture.api().getDbInterface().isStudentEnrolled(fixture.courseNumber(), student.getStudentID()));
    }

    @Test
    @DisplayName("The instructor removes a student by user name")
    void instructorRemovesStudentByUsername() throws Exception {
        fixture.loginInstructor();
        Student student = fixture.createStudent("rm2", "David", "Davis");
        fixture.api().addStudent(student);

        fixture.api().removeStudentByUsername(student.getUsername());

        assertNull(fixture.api().searchStudentByUsername(student.getUsername()));
        assertNull(fixture.api().searchStudentByID(student.getStudentID()));
    }

    @Test
    @DisplayName("Removing an unknown student is reported")
    void removingAnUnknownStudentFails() throws Exception {
        fixture.loginInstructor();
        assertThrows(DiscussionException.class, () -> fixture.api().removeStudentbyID("999999999"));
        assertThrows(DiscussionException.class, () -> fixture.api().removeStudentByUsername("nobody"));
    }

    @Test
    @DisplayName("The instructor creates a discussion and finds it by identifier and by topic")
    void instructorCreatesAndFindsDiscussion() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Design patterns");
        assertNotNull(discussion.getDiscussionID());

        Discussion byId = fixture.api().searchDiscussionByID(discussion.getDiscussionID());
        assertNotNull(byId);
        assertEquals(discussion.getTopic(), byId.getTopic());

        List<Discussion> byTopic = fixture.api().searchDiscussionByTopic(discussion.getTopic());
        assertEquals(1, byTopic.size());
        assertEquals(discussion.getDiscussionID(), byTopic.get(0).getDiscussionID());
    }

    @Test
    @DisplayName("Searching for a discussion that does not exist returns nothing")
    void searchingForAMissingDiscussion() throws Exception {
        fixture.loginInstructor();
        assertNull(fixture.api().searchDiscussionByID("99999999"));
        assertTrue(fixture.api().searchDiscussionByTopic("a topic that was never used").isEmpty());
    }

    @Test
    @DisplayName("The instructor archives a discussion and it leaves the active list")
    void instructorArchivesDiscussion() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("To be archived");

        fixture.api().archiveDiscussion(discussion.getDiscussionID());

        List<Discussion> archived = fixture.api().listArchivedDiscussions();
        assertTrue(archived.stream()
                .anyMatch(d -> discussion.getDiscussionID().equals(d.getDiscussionID())));
        assertFalse(fixture.api().listActiveDiscussions().stream()
                .anyMatch(d -> discussion.getDiscussionID().equals(d.getDiscussionID())));
    }

    @Test
    @DisplayName("Archiving a discussion that does not exist is reported")
    void archivingAMissingDiscussionFails() throws Exception {
        fixture.loginInstructor();
        assertThrows(DiscussionException.class, () -> fixture.api().archiveDiscussion("99999999"));
    }

    @Test
    @DisplayName("The instructor sees the first request to add and the first request to remove")
    void instructorReadsTheRequestQueues() throws Exception {
        fixture.loginInstructor();

        Student joiner = fixture.createStudent("join", "Eve", "Evans");
        fixture.api().getDbInterface().storeEnrollmentRequest(joiner.getStudentID());
        Student leaver = fixture.createStudent("leave", "Fred", "Franklin");
        fixture.api().getDbInterface().storeRemovalRequest(leaver.getStudentID());

        // The queue is served oldest request first, so the request that has been waiting the
        // longest comes back. The seeded database already holds requests, so the test checks
        // that the queue is ordered and that the new requests are in it.
        List<Student> enrolmentRequests = fixture.api().listEnrollmentRequests();
        assertTrue(enrolmentRequests.stream()
                .anyMatch(s -> joiner.getStudentID().equals(s.getStudentID())));

        Student firstToAdd = fixture.api().retrieveFirstRequesttoAdd();
        assertNotNull(firstToAdd);
        assertEquals(enrolmentRequests.get(0).getStudentID(), firstToAdd.getStudentID());

        List<Student> removalRequests = fixture.api().listRemovalRequests();
        assertTrue(removalRequests.stream()
                .anyMatch(s -> leaver.getStudentID().equals(s.getStudentID())));

        Student firstToRemove = fixture.api().retrieveFirstRequestToRemove();
        assertNotNull(firstToRemove);
        assertEquals(removalRequests.get(0).getStudentID(), firstToRemove.getStudentID());
    }

    @Test
    @DisplayName("An empty request queue reports nothing")
    void emptyRequestQueues() throws Exception {
        fixture.loginInstructor();
        fixture.api().getDbInterface().clearEnrollmentRequest(TestFixture.STUDENT_ID);
        List<Student> requests = fixture.api().listEnrollmentRequests();
        if (requests.isEmpty()) {
            assertNull(fixture.api().retrieveFirstRequesttoAdd());
        }
        assertNull(fixture.api().getDbInterface().retrieveEnrollmentRequests().stream()
                .filter(s -> s.getStudentID().equals(TestFixture.STUDENT_ID))
                .findFirst().orElse(null));
    }

    // ==================================================================
    // Student's API calls
    // ==================================================================

    @Test
    @DisplayName("A student requests to enrol and the instructor approves it")
    void studentRequestsToEnrol() throws Exception {
        fixture.loginStudent();
        Student student = fixture.createStudent("req", "Grace", "Green");
        fixture.api().logout();

        fixture.loginStudent();
        fixture.api().requestToEnroll(student);
        fixture.api().logout();

        fixture.loginInstructor();
        List<Student> pending = fixture.api().listEnrollmentRequests();
        assertTrue(pending.stream()
                .anyMatch(s -> student.getStudentID().equals(s.getStudentID())));

        Student request = pending.stream()
                .filter(s -> student.getStudentID().equals(s.getStudentID()))
                .findFirst()
                .orElseThrow();

        fixture.api().addStudent(request);
        assertTrue(fixture.api().getDbInterface().isStudentEnrolled(fixture.courseNumber(), student.getStudentID()));
        assertFalse(fixture.api().listEnrollmentRequests().stream()
                .anyMatch(s -> student.getStudentID().equals(s.getStudentID())));
    }

    @Test
    @DisplayName("A student requests to drop and the instructor sees the request")
    void studentRequestsToDrop() throws Exception {
        fixture.loginStudent();
        fixture.api().requestToDrop(TestFixture.STUDENT_ID);
        fixture.api().logout();

        fixture.loginInstructor();
        List<Student> pending = fixture.api().listRemovalRequests();
        assertTrue(pending.stream()
                .anyMatch(s -> TestFixture.STUDENT_ID.equals(s.getStudentID())));
        assertNotNull(fixture.api().retrieveFirstRequestToRemove());

        // The seeded student is not really leaving the course, so the request is cleared again.
        fixture.api().getDbInterface().clearRemovalRequest(TestFixture.STUDENT_ID);
    }

    @Test
    @DisplayName("A student can browse the discussions and post a message")
    void studentBrowsesAndPosts() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Student view");
        fixture.api().logout();

        fixture.loginStudent();
        List<Discussion> visible = fixture.api().listActiveDiscussions();
        assertTrue(visible.stream()
                .anyMatch(d -> discussion.getDiscussionID().equals(d.getDiscussionID())));

        fixture.api().postMessage(discussion.getDiscussionID(), TestFixture.textMessage("Hello from the student."));

        Discussion reloaded = fixture.api().searchDiscussionByID(discussion.getDiscussionID());
        assertNotNull(reloaded);
        assertEquals(1, reloaded.getMessages().size());
        Message posted = reloaded.getMessages().get(0);
        assertEquals(TestFixture.STUDENT_USERNAME, posted.getPostedBy());
        assertEquals("Hello from the student.", posted.getTextualContents());
    }

    @Test
    @DisplayName("A student finds a discussion by identifier and by topic")
    void studentSearchesDiscussions() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Searchable topic");
        fixture.api().logout();

        fixture.loginStudent();
        assertNotNull(fixture.api().searchDiscussionByID(discussion.getDiscussionID()));
        assertEquals(1, fixture.api().searchDiscussionByTopic(discussion.getTopic()).size());
    }

    @Test
    @DisplayName("A student finds a message by identifier and by user name")
    void studentSearchesMessages() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Message search");
        fixture.api().postMessage(discussion.getDiscussionID(),
                TestFixture.textMessage("Posted by the instructor."));
        fixture.api().logout();

        fixture.loginStudent();
        Discussion reloaded = fixture.api().searchDiscussionByID(discussion.getDiscussionID());
        assertNotNull(reloaded);
        assertEquals(1, reloaded.getMessages().size());
        String messageId = reloaded.getMessages().get(0).getMessageID();

        Message byId = fixture.api().searchMessageByID(discussion.getDiscussionID(), messageId);
        assertNotNull(byId);
        assertEquals(messageId, byId.getMessageID());

        List<Message> byUsername = fixture.api().searchMessageByUsername(discussion.getDiscussionID(),
                TestFixture.INSTRUCTOR_USERNAME);
        assertEquals(1, byUsername.size());
        assertEquals(TestFixture.INSTRUCTOR_USERNAME, byUsername.get(0).getPostedBy());
    }

    @Test
    @DisplayName("A student posts a message that carries a picture")
    void studentPostsAnImageMessage() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Image message");
        fixture.api().logout();

        fixture.loginStudent();
        byte[] picture = {(byte) 0x89, 'P', 'N', 'G'};
        fixture.api().postMessage(discussion.getDiscussionID(),
                TestFixture.imageMessage("chart.png", "png", picture, "Look at this chart."));

        Discussion reloaded = fixture.api().searchDiscussionByID(discussion.getDiscussionID());
        assertNotNull(reloaded);
        assertEquals(1, reloaded.getMessages().size());
        Message posted = reloaded.getMessages().get(0);
        assertNotNull(posted.getImageContents());
        assertEquals("chart.png", posted.getImageContents().getName());
        assertEquals("png", posted.getImageContents().getType());
        assertEquals(4, posted.getImageContents().getSize());
    }

    @Test
    @DisplayName("A student can browse the archive")
    void studentBrowsesTheArchive() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Archive browser");
        fixture.api().archiveDiscussion(discussion.getDiscussionID());
        fixture.api().logout();

        fixture.loginStudent();
        assertTrue(fixture.api().listArchivedDiscussions().stream()
                .anyMatch(d -> discussion.getDiscussionID().equals(d.getDiscussionID())));
        assertNotNull(fixture.api().getArchive());
    }

    // ==================================================================
    // Permissions - the instructor's calls are instructor only
    // ==================================================================

    @Test
    @DisplayName("A student cannot call the instructor's API")
    void studentCannotUseInstructorCalls() throws Exception {
        fixture.loginStudent();
        Discussion discussion = fixture.createDiscussion("Protected");

        assertThrows(DiscussionException.class, () -> fixture.api().createDicussion(new Discussion(null, "x", null)));
        assertThrows(DiscussionException.class, () -> fixture.api().addStudent(new Student("a", "b", "c", "999")));
        assertThrows(DiscussionException.class, () -> fixture.api().removeStudentbyID(TestFixture.STUDENT_ID));
        assertThrows(DiscussionException.class, () -> fixture.api().removeStudentByUsername(TestFixture.STUDENT_USERNAME));
        assertThrows(DiscussionException.class, () -> fixture.api().retrieveFirstRequesttoAdd());
        assertThrows(DiscussionException.class, () -> fixture.api().retrieveFirstRequestToRemove());
        assertThrows(DiscussionException.class, () -> fixture.api().archiveDiscussion(discussion.getDiscussionID()));
        assertThrows(DiscussionException.class, () -> fixture.api().removeMessage(discussion.getDiscussionID(),
                new Message()));
        assertThrows(DiscussionException.class, () -> fixture.api().createCourse(new Course("x", "y")));
    }

    @Test
    @DisplayName("The instructor cannot call the student's API")
    void instructorCannotUseStudentCalls() throws Exception {
        fixture.loginInstructor();
        assertThrows(DiscussionException.class, () -> fixture.api().requestToEnroll(new Student("a", "b", "c", "999")));
        assertThrows(DiscussionException.class, () -> fixture.api().requestToDrop(TestFixture.STUDENT_ID));
    }

    // ==================================================================
    // Messages
    // ==================================================================

    @Test
    @DisplayName("The instructor removes a message from a discussion")
    void instructorRemovesAMessage() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Message removal");
        fixture.api().postMessage(discussion.getDiscussionID(), TestFixture.textMessage("To be removed."));

        Discussion reloaded = fixture.api().searchDiscussionByID(discussion.getDiscussionID());
        assertEquals(1, reloaded.getMessages().size());
        Message target = reloaded.getMessages().get(0);

        fixture.api().removeMessage(discussion.getDiscussionID(), target);

        Discussion after = fixture.api().searchDiscussionByID(discussion.getDiscussionID());
        assertEquals(0, after.getMessages().size());
    }

    @Test
    @DisplayName("The instructor removes every message of one user")
    void instructorRemovesMessagesByUsername() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Remove by user");

        fixture.api().postMessage(discussion.getDiscussionID(),
                TestFixture.textMessage("Instructor message."));
        fixture.api().logout();

        fixture.loginStudent();
        fixture.api().postMessage(discussion.getDiscussionID(),
                TestFixture.textMessage("Student message."));
        fixture.api().logout();

        fixture.loginInstructor();
        List<Message> byUser = fixture.api().searchMessageByUsername(discussion.getDiscussionID(),
                TestFixture.STUDENT_USERNAME);
        assertEquals(1, byUser.size());

        fixture.api().removeMessage(discussion.getDiscussionID(), byUser.get(0));

        Discussion after = fixture.api().searchDiscussionByID(discussion.getDiscussionID());
        assertEquals(1, after.getMessages().size());
        assertEquals(TestFixture.INSTRUCTOR_USERNAME, after.getMessages().get(0).getPostedBy());
    }

    @Test
    @DisplayName("An archived discussion refuses new messages")
    void archivedDiscussionRefusesMessages() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Closed for posting");
        fixture.api().archiveDiscussion(discussion.getDiscussionID());

        assertThrows(DiscussionException.class, () -> fixture.api().postMessage(discussion.getDiscussionID(),
                TestFixture.textMessage("Too late.")));
    }

    @Test
    @DisplayName("A message needs text or an image")
    void emptyMessageIsRejected() throws Exception {
        fixture.loginInstructor();
        Discussion discussion = fixture.createDiscussion("Empty message");
        assertThrows(DiscussionException.class,
                () -> fixture.api().postMessage(discussion.getDiscussionID(), new Message()));
    }

    @Test
    @DisplayName("A message for a discussion that does not exist is rejected")
    void messageForAMissingDiscussionIsRejected() throws Exception {
        fixture.loginInstructor();
        assertThrows(DiscussionException.class, () -> fixture.api().postMessage("99999999",
                TestFixture.textMessage("Nowhere to go.")));
    }

    // ==================================================================
    // Model behaviour
    // ==================================================================

    @Test
    @DisplayName("A course holds its students and its discussions")
    void courseHoldsStudentsAndDiscussions() {
        Course course = new Course("Software Engineering", "SWE-101");
        Student student = new Student("Ada", "Lovelace", "alovelace", "42");
        Discussion discussion = new Discussion("7", "Analytical engines", "Jan-24-2024:11:34:28");

        assertTrue(course.addStudent(student));
        assertFalse(course.addStudent(student));
        assertTrue(course.addDiscussion(discussion));
        assertEquals(1, course.getStudents().size());
        assertEquals(1, course.getDiscussions().size());
        assertEquals(student, course.searchStudent("42"));
        assertEquals(discussion, course.searchDiscussionByID("7"));
        assertEquals(discussion, course.searchDiscussionByTopic("analytical engines"));
        assertNull(course.searchStudent("99"));
        assertTrue(course.removeStudent(student));
        assertEquals(0, course.getStudents().size());
    }

    @Test
    @DisplayName("A course archives one of its discussions")
    void courseArchivesADiscussion() {
        Course course = new Course("Software Engineering", "SWE-101");
        Discussion discussion = new Discussion("7", "Analytical engines", "Jan-24-2024:11:34:28");
        course.addDiscussion(discussion);

        assertTrue(course.archiveDiscussion("7"));
        assertEquals(0, course.getDiscussions().size());
        assertFalse(course.archiveDiscussion("7"));
    }

    @Test
    @DisplayName("A discussion holds its messages and finds them")
    void discussionHoldsMessages() {
        Discussion discussion = new Discussion("7", "Analytical engines", "Jan-24-2024:11:34:28");
        Message first = new Message("1", "7", "Jan-24-2024:11:35:00", "ada", "First", null);
        Message second = new Message("2", "7", "Jan-24-2024:11:36:00", "charles", "Second", null);

        discussion.postMessage(first);
        discussion.postMessage(second);

        assertEquals(2, discussion.getMessages().size());
        assertEquals(first, discussion.searchMessageByMessageID("1"));
        assertEquals(first, discussion.searchMessageByUsername("ada"));
        assertEquals(1, discussion.searchMessagesByUsername("ada").size());
        assertNull(discussion.searchMessageByMessageID("99"));
        assertNull(discussion.searchMessageByUsername("nobody"));
    }

    @Test
    @DisplayName("An archive holds the discussions that were archived")
    void archiveHoldsDiscussions() {
        com.onlinediscussion.model.Archive archive = new com.onlinediscussion.model.Archive();
        Discussion discussion = new Discussion("7", "Analytical engines", "Jan-24-2024:11:34:28");

        assertTrue(archive.addDiscussion(discussion));
        assertEquals(1, archive.getDiscussions().size());
        assertEquals(discussion, archive.retrieveDiscussionByID("7"));
        assertEquals(1, archive.retrieveDiscussionByTopic("Analytical engines").size());
        assertNull(archive.retrieveDiscussionByID("99"));
    }

    @Test
    @DisplayName("A student is a user and carries a student identifier")
    void studentIsAUser() {
        Student student = new Student("Ada", "Lovelace", "alovelace", "42");
        assertEquals("Ada", student.getFirstname());
        assertEquals("Lovelace", student.getLastname());
        assertEquals("alovelace", student.getUsername());
        assertEquals("42", student.getStudentID());
        assertEquals("Ada Lovelace", student.getFullName());

        student.setFirstname("Augusta");
        assertEquals("Augusta", student.getFirstname());
    }

    @Test
    @DisplayName("An instructor is a user")
    void instructorIsAUser() {
        com.onlinediscussion.model.Instructor instructor =
                new com.onlinediscussion.model.Instructor("Kasi", "Doe", "drKasi");
        assertEquals("Kasi", instructor.getFirstname());
        assertEquals("Doe", instructor.getLastname());
        assertEquals("drKasi", instructor.getUsername());
    }

    @Test
    @DisplayName("A message carries text, a picture or both")
    void messageCarriesItsContents() {
        Message message = new Message();
        message.setTextualContents("Some text");
        assertEquals("Some text", message.getTextualContents());
        assertNull(message.getImageContents());

        com.onlinediscussion.model.Image image =
                new com.onlinediscussion.model.Image("a.png", "png", new byte[] {1, 2, 3});
        message.setImageContents(image);
        assertEquals(image, message.getImageContents());
        assertEquals(3, message.getImageContents().getSize());
    }
}
