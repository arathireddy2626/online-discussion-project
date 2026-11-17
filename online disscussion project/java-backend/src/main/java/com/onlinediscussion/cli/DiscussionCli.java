package com.onlinediscussion.cli;

import com.onlinediscussion.core.DiscussionException;
import com.onlinediscussion.core.OnlineDiscussionMain;
import com.onlinediscussion.core.Session;
import com.onlinediscussion.db.MySQLDBInterface;
import com.onlinediscussion.model.Course;
import com.onlinediscussion.model.Discussion;
import com.onlinediscussion.model.Image;
import com.onlinediscussion.model.Message;
import com.onlinediscussion.model.Student;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A command line front end for {@link OnlineDiscussionMain}.
 *
 * <p>It is deliberately not connected to the front end. It exists so that every API call can
 * be exercised from a terminal, either interactively or with a single command.
 *
 * <p>Usage:
 * <pre>
 *   java com.onlinediscussion.cli.DiscussionCli              # interactive mode
 *   java com.onlinediscussion.cli.DiscussionCli &lt;command&gt; [args...]   # one shot
 *   java com.onlinediscussion.cli.DiscussionCli --script file # run a file of commands
 * </pre>
 */
public final class DiscussionCli {

    private final OnlineDiscussionMain api;
    private final MySQLDBInterface database;

    public DiscussionCli(OnlineDiscussionMain api, MySQLDBInterface database) {
        this.api = api;
        this.database = database;
    }

    public static void main(String[] args) {
        Config config = Config.load();
        MySQLDBInterface database = null;
        try {
            database = new MySQLDBInterface(config.getHost(), config.getPort(), config.getDatabase(),
                    config.getUser(), config.getPassword());
            database.connect();
            OnlineDiscussionMain api = new OnlineDiscussionMain(database);
            DiscussionCli cli = new DiscussionCli(api, database);

            if (args.length == 0) {
                cli.runInteractive();
                return;
            }
            if ("--script".equals(args[0])) {
                if (args.length < 2) {
                    System.out.println("A script file is required.");
                    return;
                }
                cli.runScript(Path.of(args[1]));
                return;
            }
            if ("--help".equals(args[0]) || "-h".equals(args[0])) {
                cli.printHelp();
                return;
            }
            cli.execute(args);
        } catch (DiscussionException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    // ==================================================================
    // Interactive mode
    // ==================================================================

    private void runInteractive() throws DiscussionException {
        System.out.println("Online Discussion Tool - command line interface");
        System.out.println("Connected to " + database.getClass().getSimpleName() + ".");
        printHelp();
        System.out.println();
        System.out.println("Type 'help' for the command list, 'exit' to quit.");

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
            while (true) {
                System.out.print("\n> ");
                System.out.flush();
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                    System.out.println("Goodbye.");
                    break;
                }
                List<String> tokens = Config.tokenize(line);
                if (tokens.isEmpty()) {
                    continue;
                }
                try {
                    execute(tokens.toArray(new String[0]));
                } catch (DiscussionException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error: could not read the input: " + e.getMessage());
        }
    }

    private void runScript(Path script) throws DiscussionException {
        Map<String, String> variables = new HashMap<>();
        try {
            for (String line : Files.readAllLines(script)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                // The variables are filled in before the command is shown, so the output
                // shows the command that really ran.
                String expanded = substitute(trimmed, variables);
                System.out.println("> " + expanded);
                List<String> tokens = Config.tokenize(expanded);
                if (tokens.isEmpty()) {
                    continue;
                }
                try {
                    execute(tokens.toArray(new String[0]), variables);
                } catch (DiscussionException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error: could not read the script: " + e.getMessage());
        }
    }

    // ==================================================================
    // Script variables
    // ==================================================================

    /**
     * Replaces every {@code %name%} in the text with the value that the script stored under
     * that name. This lets a script capture an identifier and use it later without hard
     * coding it.
     */
    static String substitute(String text, Map<String, String> variables) {
        if (text == null || text.indexOf('%') < 0) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            int start = text.indexOf('%', index);
            if (start < 0) {
                result.append(text, index, text.length());
                break;
            }
            int end = text.indexOf('%', start + 1);
            if (end < 0) {
                result.append(text, index, text.length());
                break;
            }
            result.append(text, index, start);
            String name = text.substring(start + 1, end);
            String value = variables.get(name);
            result.append(value == null ? text.substring(start, end + 1) : value);
            index = end + 1;
        }
        return result.toString();
    }

    /** Pulls the first identifier out of the text that the command produced. */
    static String firstIdentifier(String text) {
        if (text == null) {
            return "";
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int pipe = trimmed.indexOf('|');
            if (pipe > 0) {
                return trimmed.substring(0, pipe).trim();
            }
        }
        return "";
    }

    // ==================================================================
    // Command dispatch
    // ==================================================================

    private void execute(String[] args) throws DiscussionException {
        execute(args, new HashMap<>());
    }

    private void execute(String[] args, Map<String, String> variables) throws DiscussionException {
        String command = args[0].toLowerCase();
        switch (command) {
            // -- for all users ------------------------------------------------
            case "login":
                requireArgs(args, 3, "login <username> <password>");
                if (api.login(args[1], args[2])) {
                    Session session = api.getSession();
                    System.out.println("Signed in as " + session.getUsername() + " (" + session.getRole() + ").");
                } else {
                    System.out.println("Login failed: the user name or the password is wrong.");
                }
                break;
            case "logout":
                api.logout();
                System.out.println("Signed out.");
                break;
            case "whoami":
                printSession();
                break;

            // -- instructor ----------------------------------------------------
            case "createcourse": {
                requireArgs(args, 3, "createCourse <courseName> <courseNumber>");
                // The course name can hold spaces, so the number is taken as the last word
                // and the name is everything in front of it.
                String courseNumber = args[args.length - 1];
                String courseName = joinFrom(args, 1, args.length - 1);
                api.createCourse(new Course(courseName, courseNumber));
                System.out.println("Created the course " + courseName + " (" + courseNumber + ").");
                break;
            }
            case "firstrequesttoadd": {
                Student student = api.retrieveFirstRequesttoAdd();
                System.out.println(student == null ? "No enrolment request is waiting."
                        : "First enrolment request: " + describe(student));
                break;
            }
            case "firstrequesttoremove": {
                Student student = api.retrieveFirstRequestToRemove();
                System.out.println(student == null ? "No removal request is waiting."
                        : "First removal request: " + describe(student));
                break;
            }
            case "addstudent":
                requireArgs(args, 5, "addStudent <studentID> <firstname> <lastname> [username]");
                api.addStudent(new Student(args[2], args[3],
                        args.length > 4 ? args[4] : defaultUsername(args[2], args[3]), args[1]));
                System.out.println("Added the student " + args[1] + " to " + api.getCurrentCourseNumber() + ".");
                break;
            case "removestudentbyid":
                requireArgs(args, 2, "removeStudentbyID <id>");
                api.removeStudentbyID(args[1]);
                System.out.println("Removed the student " + args[1] + ".");
                break;
            case "removestudentbyusername":
                requireArgs(args, 2, "removeStudentByUsername <username>");
                api.removeStudentByUsername(args[1]);
                System.out.println("Removed the student " + args[1] + ".");
                break;
            case "searchstudentbyid": {
                requireArgs(args, 2, "searchStudentByID <id>");
                Student student = api.searchStudentByID(args[1]);
                System.out.println(student == null ? "No student has the identifier " + args[1] + "."
                        : describe(student));
                break;
            }
            case "searchstudentbyusername": {
                requireArgs(args, 2, "searchStudentByUsername <username>");
                Student student = api.searchStudentByUsername(args[1]);
                System.out.println(student == null ? "No student has the user name " + args[1] + "."
                        : describe(student));
                break;
            }
            case "createdicussion":
                requireArgs(args, 2, "createDicussion <topic>");
                String topic = joinFrom(args, 1);
                api.createDicussion(new Discussion(null, topic, null));
                System.out.println("Created the discussion \"" + topic + "\".");
                break;
            case "createdicussioncapture": {
                requireArgs(args, 3, "createDicussionCapture <variableName> <topic>");
                String variableName = args[1];
                String newTopic = joinFrom(args, 2);
                api.createDicussion(new Discussion(null, newTopic, null));
                List<Discussion> created = api.searchDiscussionByTopic(newTopic);
                if (!created.isEmpty()) {
                    variables.put(variableName, created.get(0).getDiscussionID());
                    System.out.println("Created the discussion \"" + newTopic + "\" with the identifier "
                            + created.get(0).getDiscussionID() + " (saved as %" + variableName + "%).");
                } else {
                    System.out.println("Created the discussion \"" + newTopic + "\".");
                }
                break;
            }
            case "set": {
                requireArgs(args, 3, "set <variableName> <value>");
                variables.put(args[1], joinFrom(args, 2));
                System.out.println("Set %" + args[1] + "% to " + joinFrom(args, 2) + ".");
                break;
            }
            case "archivediscussion":
                requireArgs(args, 2, "archiveDiscussion <id>");
                api.archiveDiscussion(args[1]);
                System.out.println("Archived the discussion " + args[1] + ".");
                break;
            case "liststudents":
                printStudents("Enrolled students", api.listEnrolledStudents());
                break;
            case "listenrollrequests":
                printStudents("Enrolment requests", api.listEnrollmentRequests());
                break;
            case "listremovalrequests":
                printStudents("Removal requests", api.listRemovalRequests());
                break;
            case "listcourses": {
                for (Course course : api.getDbInterface().retrieveAllCourses()) {
                    int discussions = database.countActiveDiscussions(course.getCourseNumber());
                    System.out.println(course.getCourseNumber() + " - " + course.getCourseName()
                            + " (" + course.getStudents().size() + " students, "
                            + discussions + " discussions)");
                }
                break;
            }

            // -- shared reads ----------------------------------------------------
            case "searchdiscussionbyid": {
                requireArgs(args, 2, "searchDiscussionByID <id>");
                Discussion discussion = api.searchDiscussionByID(args[1]);
                if (discussion == null) {
                    System.out.println("No discussion has the identifier " + args[1] + ".");
                } else {
                    printDiscussion(discussion);
                }
                break;
            }
            case "searchdiscussionbytopic": {
                requireArgs(args, 2, "searchDiscussionByTopic <topic>");
                printDiscussions(api.searchDiscussionByTopic(joinFrom(args, 1)));
                break;
            }
            case "finddiscussion": {
                // Finds a discussion by topic and saves its identifier under a variable name,
                // so that a script can use the identifier without hard coding it.
                requireArgs(args, 3, "findDiscussion <variableName> <topic>");
                String variableName = args[1];
                String wantedTopic = joinFrom(args, 2);
                List<Discussion> found = api.searchDiscussionByTopic(wantedTopic);
                if (found.isEmpty()) {
                    System.out.println("No discussion has the topic \"" + wantedTopic + "\".");
                } else {
                    variables.put(variableName, found.get(0).getDiscussionID());
                    System.out.println("Found \"" + wantedTopic + "\" with the identifier "
                            + found.get(0).getDiscussionID() + " (saved as %" + variableName + "%).");
                }
                break;
            }
            case "listdiscussions":
                printDiscussions(api.listActiveDiscussions());
                break;
            case "listarchived":
                printDiscussions(api.listArchivedDiscussions());
                break;

            // -- messages ---------------------------------------------------------
            case "postmessage": {
                requireArgs(args, 3, "postMessage <discussionID> <text>");
                Message message = new Message();
                message.setTextualContents(joinFrom(args, 2));
                api.postMessage(args[1], message);
                System.out.println("Posted the message to the discussion " + args[1] + ".");
                break;
            }
            case "postmessagetotopic": {
                requireArgs(args, 3, "postMessageToTopic <topic> <text>");
                // Both the topic and the text can hold spaces, so the split point is found by
                // matching the longest topic that a discussion actually has.
                List<String> words = java.util.Arrays.asList(args).subList(1, args.length);
                List<Discussion> all = api.listActiveDiscussions();
                Discussion target = null;
                String targetTopic = null;
                String text = null;
                for (int wordsInTopic = words.size() - 1; wordsInTopic >= 1 && target == null; wordsInTopic--) {
                    String candidateTopic = String.join(" ", words.subList(0, wordsInTopic));
                    for (Discussion discussion : all) {
                        if (discussion.getTopic().equals(candidateTopic)) {
                            target = discussion;
                            targetTopic = candidateTopic;
                            text = String.join(" ", words.subList(wordsInTopic, words.size()));
                            break;
                        }
                    }
                }
                if (target == null) {
                    System.out.println("No discussion matches a topic in that command.");
                    break;
                }
                Message message = new Message();
                message.setTextualContents(text);
                api.postMessage(target.getDiscussionID(), message);
                System.out.println("Posted the message to \"" + targetTopic + "\" ("
                        + target.getDiscussionID() + ").");
                break;
            }
            case "postimagemessage": {
                requireArgs(args, 4, "postImageMessage <discussionID> <imagePath> <text>");
                Message message = new Message();
                message.setTextualContents(args.length > 3 ? args[3] : null);
                message.setImageContents(readImage(args[2]));
                api.postMessage(args[1], message);
                System.out.println("Posted the image message to the discussion " + args[1] + ".");
                break;
            }
            case "removemessage": {
                requireArgs(args, 3, "removeMessage <discussionID> <messageID>");
                Message message = api.searchMessageByID(args[1], args[2]);
                if (message == null) {
                    System.out.println("No message has the identifier " + args[2] + " in that discussion.");
                } else {
                    api.removeMessage(args[1], message);
                    System.out.println("Removed the message " + args[2] + ".");
                }
                break;
            }
            case "searchmessagebyid": {
                requireArgs(args, 3, "searchMessageByID <discussionID> <messageID>");
                Message message = api.searchMessageByID(args[1], args[2]);
                System.out.println(message == null ? "No message has the identifier " + args[2] + "."
                        : describe(message));
                break;
            }
            case "searchmessagebyusername": {
                requireArgs(args, 3, "searchMessageByUsername <discussionID> <username>");
                printMessages(api.searchMessageByUsername(args[1], args[2]));
                break;
            }
            case "findmessage": {
                // Finds the newest message of a discussion that was posted by a user and
                // saves its identifier, so that a script can remove it later.
                requireArgs(args, 4, "findMessage <variableName> <discussionID> <username>");
                String variableName = args[1];
                List<Message> found = api.searchMessageByUsername(args[2], args[3]);
                if (found.isEmpty()) {
                    System.out.println("No message in the discussion " + args[2] + " was posted by "
                            + args[3] + ".");
                } else {
                    Message newest = found.get(found.size() - 1);
                    variables.put(variableName, newest.getMessageID());
                    System.out.println("Found the message " + newest.getMessageID() + " (saved as %"
                            + variableName + "%).");
                }
                break;
            }

            // -- student ------------------------------------------------------------
            case "requesttoenroll": {
                requireArgs(args, 5, "requestToEnroll <studentID> <firstname> <lastname> [username]");
                api.requestToEnroll(new Student(args[2], args[3],
                        args.length > 4 ? args[4] : defaultUsername(args[2], args[3]), args[1]));
                System.out.println("Requested to enrol the student " + args[1] + ".");
                break;
            }
            case "requesttodrop":
                requireArgs(args, 2, "requestToDrop <studentID>");
                api.requestToDrop(args[1]);
                System.out.println("Requested to drop the student " + args[1] + ".");
                break;

            // -- helpers ---------------------------------------------------------------
            case "setcourse":
                requireArgs(args, 2, "setCourse <courseNumber>");
                api.setCurrentCourseNumber(args[1]);
                System.out.println("The current course is now " + api.getCurrentCourseNumber() + ".");
                break;
            case "echo":
                System.out.println(args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "");
                break;
            case "help":
                printHelp();
                break;
            default:
                System.out.println("Unknown command: " + args[0] + ". Type 'help' for the list.");
        }
    }

    // ==================================================================
    // Output helpers
    // ==================================================================

    private void printSession() {
        Session session = api.getSession();
        if (session == null) {
            System.out.println("Nobody is signed in.");
            return;
        }
        System.out.println("Signed in as " + session.getUsername()
                + " (" + session.getRole() + ")"
                + (session.getStudentID() == null ? "" : ", student " + session.getStudentID()));
    }

    private void printStudents(String heading, List<Student> students) {
        System.out.println(heading + ": " + students.size());
        for (Student student : students) {
            System.out.println("  " + describe(student));
        }
    }

    private void printDiscussions(List<Discussion> discussions) {
        System.out.println("Discussions: " + discussions.size());
        for (Discussion discussion : discussions) {
            System.out.println("  " + discussion.getDiscussionID() + " | " + discussion.getTopic()
                    + " | " + discussion.getDateAndTime() + " | " + discussion.getMessages().size() + " messages");
        }
    }

    private void printDiscussion(Discussion discussion) {
        System.out.println("Discussion " + discussion.getDiscussionID()
                + " | topic: " + discussion.getTopic()
                + " | created: " + discussion.getDateAndTime());
        printMessages(discussion.getMessages());
    }

    private void printMessages(List<Message> messages) {
        System.out.println("  Messages: " + messages.size());
        for (Message message : messages) {
            System.out.println("    " + message.getMessageID() + " | " + message.getPostedBy()
                    + " | " + message.getDateAndTime() + " | " + message.getTextualContents()
                    + (message.getImageContents() == null ? "" : " | image: " + message.getImageContents().getName()));
        }
    }

    private String describe(Student student) {
        return student.getStudentID() + " | " + student.getFullName()
                + " | username: " + (student.getUsername() == null ? "(none)" : student.getUsername());
    }

    private String describe(Message message) {
        return message.getMessageID() + " | " + message.getPostedBy()
                + " | " + message.getDateAndTime() + " | " + message.getTextualContents();
    }

    private Image readImage(String path) throws DiscussionException {
        try {
            byte[] data = Files.readAllBytes(Path.of(path));
            String name = Path.of(path).getFileName().toString();
            String type = "png";
            if (name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg")) {
                type = "jpg";
            } else if (name.toLowerCase().endsWith(".gif")) {
                type = "gif";
            }
            return new Image(name, type, data);
        } catch (IOException e) {
            throw new DiscussionException("Could not read the image " + path + ": " + e.getMessage(), e);
        }
    }

    private String defaultUsername(String firstname, String lastname) {
        String base = (firstname.charAt(0) + lastname).toLowerCase().replaceAll("[^a-z0-9]", "");
        return base;
    }

    private void requireArgs(String[] args, int count, String usage) throws DiscussionException {
        if (args.length < count) {
            throw new DiscussionException("Usage: " + usage);
        }
    }

    /** Joins the arguments from the given position on, so that a text can hold spaces. */
    private static String joinFrom(String[] args, int from) {
        return joinFrom(args, from, args.length);
    }

    /** Joins the arguments from {@code from} up to but not including {@code to}. */
    private static String joinFrom(String[] args, int from, int to) {
        StringBuilder text = new StringBuilder();
        for (int i = Math.max(from, 0); i < Math.min(to, args.length); i++) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(args[i]);
        }
        return text.toString();
    }

    private void printHelp() {
        System.out.println();
        System.out.println("Commands");
        System.out.println("  -- for all users --");
        System.out.println("    login <username> <password>          sign in");
        System.out.println("    logout                              sign out");
        System.out.println("    whoami                              show the signed in user");
        System.out.println("  -- instructor only --");
        System.out.println("    createCourse <name> <number>        create a course");
        System.out.println("    setCourse <courseNumber>            choose the course to act on");
        System.out.println("    firstRequesttoAdd                    first pending enrolment request");
        System.out.println("    firstRequestToRemove                 first pending removal request");
        System.out.println("    addStudent <id> <first> <last> [user] enrol a student");
        System.out.println("    removeStudentbyID <id>               remove a student by identifier");
        System.out.println("    removeStudentByUsername <username>   remove a student by user name");
        System.out.println("    createDicussion <topic>              create a discussion");
        System.out.println("    archiveDiscussion <id>               archive a discussion");
        System.out.println("    listStudents                         the enrolled students");
        System.out.println("    listEnrollRequests                   the pending enrolment requests");
        System.out.println("    listRemovalRequests                  the pending removal requests");
        System.out.println("    listCourses                          every course");
        System.out.println("  -- student only --");
        System.out.println("    requestToEnroll <id> <first> <last> [user]  request to join the course");
        System.out.println("    requestToDrop <studentID>            request to leave the course");
        System.out.println("  -- shared --");
        System.out.println("    searchStudentByID <id>               find a student by identifier");
        System.out.println("    searchStudentByUsername <username>   find a student by user name");
        System.out.println("    searchDiscussionByID <id>            find a discussion by identifier");
        System.out.println("    searchDiscussionByTopic <topic>      find discussions by topic");
        System.out.println("    listDiscussions                      every discussion that is not archived");
        System.out.println("    listArchived                         every archived discussion");
        System.out.println("    postMessage <discussionID> <text>    post a message");
        System.out.println("    postImageMessage <id> <path> [text]  post a message with a picture");
        System.out.println("    removeMessage <discussionID> <msgID>  remove a message");
        System.out.println("    searchMessageByID <id> <msgID>       find a message by identifier");
        System.out.println("    searchMessageByUsername <id> <user>  find messages by user name");
        System.out.println("  -- only in a script --");
        System.out.println("    set <name> <value>                 save a value under a name");
        System.out.println("    findDiscussion <name> <topic>       save the identifier of a discussion");
        System.out.println("    findMessage <name> <id> <username>  save the identifier of a message");
        System.out.println("    createDicussionCapture <name> <topic>  create a discussion and save its identifier");
        System.out.println("    echo <text>                         print a line");
        System.out.println();
        System.out.println("  In a script, %name% is replaced with the value that was saved under that name.");
        System.out.println();
    }

    /** Unused, kept so that the base64 encoder is available for future image downloads. */
    static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
