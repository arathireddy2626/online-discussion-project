# Online Discussion Tool - Java Backend

This is the Java implementation of the Online Discussion Tool backend. It follows the backend
design that came with the assignment:

1. The design is implemented in Java.
2. It is integrated with the MySQL database the project already uses.
3. A command line interface exercises every API call. It is **not** connected to the front end.
4. Every functionality is covered by the test suite, both as the instructor and as a student.

The existing Node.js server in `../server` keeps working against the same database. Nothing in
this folder changes the behaviour of that server.

---

## 1. Quick start

```bat
cd java-backend

start-mysql.bat               REM starts MySQL and checks the connection
build.bat                     REM compiles the main and the test sources
test.bat                      REM runs the JUnit 5 suite
cli.bat                       REM starts the interactive command line interface
cli.bat --script demo-full.txt   REM runs the full demonstration
```

> **Looking for the demonstration guide?** See
> [DEMONSTRATION-GUIDE.md](DEMONSTRATION-GUIDE.md). It explains every function in plain English
> with real output, and it ends with a short plan for showing the work.

The JDK is bundled in `tools/jdk`, so no Java installation is needed on the machine.

The database settings live in `db.properties`. They match the settings in `../server/.env`:

```properties
db.host=127.0.0.1
db.port=3306
db.name=discussion_tool
db.user=root
db.password=...
```

Every setting can also come from the environment (`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`,
`MYSQL_USER`, `MYSQL_PASSWORD`). `db.properties` wins over the environment.

---

## 2. Layout

```
java-backend/
  build.bat                 compiles main and test sources
  test.bat                  runs the test suite
  cli.bat                   starts the command line interface
  cli.bat --script <file>   runs a file of commands
  DEMONSTRATION-GUIDE.md  the plain English guide with worked examples
  demo-full.txt             the full demonstration, every functionality
  demo-output.txt           the real output of the last run
  sample-picture.png        a small picture used by the demonstration
  db.properties             database settings
  lib/                      the dependency jars
  tools/jdk/                the bundled JDK 21
  src/main/java/com/onlinediscussion/
    model/                  User, Student, Instructor, Course, Discussion, Message, Image, Archive
    db/                     DBInterface and MySQLDBInterface
    core/                   OnlineDiscussionMain, Session, Role, DiscussionException
    cli/                    DiscussionCli and Config
  src/test/java/com/onlinediscussion/
    TestFixture.java        shared fixture, creates and cleans up its own rows
    OnlineDiscussionToolTest.java   the functional tests
```

The package split follows the class diagram: the model holds the data, `db` talks to MySQL,
`core` holds the public API, and `cli` is the command line front end.

---

## 3. The class design, as implemented

| Class | File | Notes |
| --- | --- | --- |
| `User` | `model/User.java` | `firstname`, `lastname`, `username`. `username` identifies the object, so it has no setter. |
| `Student` | `model/Student.java` | Extends `User`, adds `studentID`. No setter, it identifies the object. |
| `Instructor` | `model/Instructor.java` | Extends `User`, adds nothing. |
| `Course` | `model/Course.java` | `courseName`, `courseNumber`, a collection of `Student` and a collection of `Discussion`. |
| `Discussion` | `model/Discussion.java` | `discussionID`, `topic`, `dateAndTime`, a collection of `Message`. |
| `Message` | `model/Message.java` | `messageID`, `discussionID`, `dateAndTime`, `postedBy`, `textualContents` (nullable), `imageContents` (nullable). |
| `Image` | `model/Image.java` | The picture that a `Message` can carry. |
| `Archive` | `model/Archive.java` | A collection of `Discussion`. |
| `DBInterface` | `db/DBInterface.java` | The persistence contract. |
| `MySQLDBInterface` | `db/MySQLDBInterface.java` | The JDBC implementation. |
| `OnlineDiscussionMain` | `core/OnlineDiscussionMain.java` | The public API, with the instructor's and the student's calls. |

Every attribute has a getter. Attributes that identify an object (`studentID`, `username`,
`courseNumber`, `discussionID`, `messageID`) have no setter, as the design requires. The other
attributes do have one.

### The instructor's calls

| Call | Behaviour |
| --- | --- |
| `createCourse(c)` | Creates the course and makes it the current course. |
| `retrieveFirstRequesttoAdd()` | The oldest student waiting to join, or `null`. |
| `retrieveFirstRequestToRemove()` | The oldest student waiting to leave, or `null`. |
| `addStudent(s)` | Enrols the student and clears the pending request. |
| `removeStudentbyID(id)` | Removes the student from the course and deletes the record. |
| `removeStudentByUsername(uname)` | The same, looked up by user name. |
| `searchStudentByID(id)` | Finds a student. |
| `searchStudentByUsername(uname)` | Finds a student. |
| `createDicussion(d)` | Creates a discussion in the current course. |
| `searchDiscussionByID(id)` | Finds a discussion and its messages. |
| `searchDiscussionByTopic(topic)` | Finds every discussion with that topic. |
| `postMessage(discussionID, message)` | Posts a message. The author is always the signed in user. |
| `removeMessage(discussionID, message)` | Removes a message. |
| `searchMessageByID(discussionID, messageID)` | Finds one message. |
| `searchMessageByUsername(discussionID, uname)` | Finds every message of one user. |

### The student's calls

| Call | Behaviour |
| --- | --- |
| `requestToEnroll(s)` | Records a request to join the course. |
| `requestToDrop(studentID)` | Records a request to leave the course. |
| `searchDiscussionByID(id)` | Finds a discussion. |
| `searchDiscussionByTopic(topic)` | Finds discussions by topic. |
| `postMessage(discussionID, message)` | Posts a message. |
| `searchMessageByID(discussionID, messageID)` | Finds one message. |
| `searchMessageByUsername(discussionID, uname)` | Finds every message of one user. |

### For all users

`login(username, password)` returns `true` when the credentials match an account, and
`logout()` clears the session.

### How the permissions are enforced

`OnlineDiscussionMain` keeps the signed in `Session`. Every call checks it first:

- The instructor's calls need an instructor.
- The student's calls need a student.
- The shared calls need any signed in user.

A call that is not allowed raises a `DiscussionException` with a clear message. The tests
cover both directions, so a student can never reach an instructor's call and the instructor
can never reach a student's call.

---

## 4. The database

The project already had a MySQL schema built around the professor's rubric, and the Java
backend uses it as it is. The table and column names keep their original spelling, including
`Dicussion` and `ReuqestToCreateDiscussion`.

| Rubric table | Used for |
| --- | --- |
| `LoginCredential` | The user name, the password hash and the role. |
| `StudentRecord` | The student rows. |
| `EnrollQueue` | The students waiting to join. |
| `RemoveStudentQueue` | The students waiting to leave. |
| `Dicussion` | The discussions. |
| `Message` | The messages, with the optional picture. |
| `DiscussionToMessage` | The messages of a discussion. |
| `ArchivedDicussions` | The archived discussions and their text files. |
| `ReuqestToCreateDiscussion` | The students asking for a new discussion. |
| `RequestToRetrieveDiscussion` | The students asking for an archived discussion. |

### Migration 010 - the course bridge

The rubric schema has no course table: a discussion carries a free text `CourseName` and the
roster lives in the request queues. The class design needs a `Course` that owns a roster and
a set of discussions. `database/010_course_bridge.sql` adds the missing pieces without
renaming or dropping anything:

- `course` holds the course number, the course name and the owning instructor.
- `course_student` holds the enrolled students.
- `Dicussion.CourseNumber` points a discussion at its course.

The migration seeds the course `SWE-101` (Software Engineering), points the existing
discussions at it and enrols the students that already hold a login. It is safe to run more
than once.

Apply it with:

```bat
cd ..\server
node -e "const fs=require('fs'),m=require('mysql2/promise');(async()=>{const c=await m.createConnection({host:'127.0.0.1',port:3306,user:'root',password:'...',database:'discussion_tool',multipleStatements:true});await c.query(fs.readFileSync('../database/010_course_bridge.sql','utf8'));console.log('done');await c.end()})()"
```

### Notes on the schema

- `Dicussion.DateAndTimeOfCreation` carries a unique index. The Java layer therefore moves
  the creation time forward by a second until it finds a free slot.
- `StudentRecord.Username` is a foreign key on `LoginCredential.Username`. A credential is
  therefore created before a student row can name it.
- `Message.Contents` is `NOT NULL` in the rubric. A message that carries only a picture stores
  an empty string in that column.

---

## 5. The command line interface

`cli.bat` starts an interactive session. `cli.bat <command> [args...]` runs a single command.
`cli.bat --script <file>` runs a file of commands, which is the easiest way to drive a whole
session because the session is kept in memory.

| Group | Commands |
| --- | --- |
| For all users | `login <username> <password>`, `logout`, `whoami` |
| Instructor only | `createCourse <name> <number>`, `setCourse <number>`, `firstRequesttoAdd`, `firstRequestToRemove`, `addStudent <id> <first> <last> [username]`, `removeStudentbyID <id>`, `removeStudentByUsername <username>`, `createDicussion <topic>`, `archiveDiscussion <id>`, `listStudents`, `listEnrollRequests`, `listRemovalRequests`, `listCourses` |
| Student only | `requestToEnroll <id> <first> <last> [username]`, `requestToDrop <studentID>` |
| Shared | `searchStudentByID <id>`, `searchStudentByUsername <username>`, `searchDiscussionByID <id>`, `searchDiscussionByTopic <topic>`, `listDiscussions`, `listArchived`, `postMessage <discussionID> <text>`, `postMessageToTopic <topic> <text>`, `postImageMessage <discussionID> <path> [text]`, `removeMessage <discussionID> <messageID>`, `searchMessageByID <discussionID> <messageID>`, `searchMessageByUsername <discussionID> <username>` |

Text that holds spaces is written without quotes, for example:

```
postMessage 12 Here is a longer reply.
```

`postMessageToTopic` finds the discussion by matching the longest topic that a discussion
actually has, so both the topic and the text can hold spaces.

`demo-full.txt` walks through the whole tool once as the instructor and once as a student.

### Script variables

In a script, `%name%` is replaced with the value that was saved under that name. This keeps a
script from having to hard code an identifier that the database decides.

| Command | What it does |
| --- | --- |
| `set <name> <value>` | Saves a value under a name. |
| `findDiscussion <name> <topic>` | Finds a discussion and saves its identifier. |
| `findMessage <name> <id> <username>` | Finds a message and saves its identifier. |
| `createDicussionCapture <name> <topic>` | Creates a discussion and saves its identifier. |

```bat
createDicussionCapture d Demo check topic
postMessage %d% Hello everybody.
findMessage m %d% drKasi
removeMessage %d% %m%
```

---

## 6. The tests

`test.bat` runs the JUnit 5 suite against the real MySQL database.

```
[         39 tests found           ]
[         39 tests successful      ]
[          0 tests failed          ]
```

The tests cover:

- Signing in and out, for the instructor and for a student, with a wrong password, with an
  unknown user name and with empty credentials.
- The instructor creating a course, adding a student, finding a student by identifier and by
  user name, removing a student by identifier and by user name.
- The instructor reading the request queues, and the student asking to join and to leave, with
  the instructor seeing the requests.
- Creating a discussion, finding it by identifier and by topic, archiving it, and the
  archived discussion leaving the active list.
- Posting a message as the instructor and as a student, with text, with a picture and with
  both, removing a message, and finding a message by identifier and by user name.
- An archived discussion refusing new messages, and a message without contents being refused.
- The permissions: a student can never reach an instructor's call and the instructor can
  never reach a student's call.
- The model classes on their own: the collections, the searches and the getters.

Every test creates its own rows and removes them again, so the suite can be run repeatedly and
leaves the seeded data alone. Running it twice in a row leaves the database in exactly the same
state.

---

## 7. Dependencies

| Jar | Why |
| --- | --- |
| `mysql-connector-j-8.4.0.jar` | The MySQL JDBC driver. |
| `bcrypt-0.10.2.jar` | Hashes and checks the passwords. |
| `bytes-1.5.0.jar` | A dependency of the bcrypt library. |
| `junit-platform-console-standalone-1.10.2.jar` | The JUnit 5 runner. |

All of them are in `lib/`.
