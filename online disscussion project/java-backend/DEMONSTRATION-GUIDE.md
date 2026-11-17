# Online Discussion Tool - Java Backend
## Demonstration Guide and Worked Examples

This document explains, in plain language, what the Online Discussion Tool does, how to run it,
and how to show each function to your professor. Every output below is **real output** copied
from an actual run of the program.

---

# Part 1 - What the program is

The Online Discussion Tool is a website where a class can hold online discussions. An
**instructor** creates discussions and manages the class list. A **student** joins the class,
reads discussions and writes replies.

This is the **backend**. It does the real work: it checks who is allowed to do what, it saves
everything in a MySQL database, and it gives the front end a clean set of functions to call.

The front end is **not** connected yet. That was asked for on purpose, so that the backend can
be checked on its own first.

## The three parts of this project

| Part | Where it is | What it does |
| --- | --- | --- |
| The database | MySQL, `discussion_tool` | Stores the users, the students, the discussions and the messages |
| The backend | `java-backend/` (this work) | The Java classes that do the work |
| The command line interface | `java-backend/cli.bat` | A way to try every function from a terminal |

## The classes, as designed

```
                    +--------------------+
                    |     DBInterface    |
                    +--------------------+
                             ^
                             | uses
                    +--------------------+
                    | OnlineDiscussionMain|
                    +--------------------+
                      |          |     ^
                      |          |     | inherits
            +---------+    +-----+-----+        +------------+
            |  Course  |    |   User   |        | Instructor |
            +---------+    +-----+-----+        +------------+
              |    ^             ^                   |
              |    |             | inherits         | manages
              |    |       +-----+-----+             |
              |    |       |  Student  |             |
              |    |       +-----------+             |
              |    |                                |
   +------------+  |          +------------+      |
   | Discussion |--+----------|  Archive   |<-----+
   +------------+             +------------+
         |     ^
         |     | contains
         |  +-----------+
         +--|  Message  |
            +-----------+
```

| Class | What it holds |
| --- | --- |
| `User` | first name, last name, user name |
| `Student` | a User, plus a student ID |
| `Instructor` | a User, nothing more |
| `Course` | course name, course number, the students, the discussions |
| `Discussion` | an ID, a topic, a date, the messages inside it |
| `Message` | an ID, the date, who posted it, the text, and an optional picture |
| `Archive` | the discussions that were archived |
| `DBInterface` | the list of things that can be saved and loaded |
| `OnlineDiscussionMain` | the public functions the rest of the program calls |

**One rule worth mentioning to your professor:** every attribute has a getter. But an attribute
that identifies an object (`studentID`, `username`, `courseNumber`, `discussionID`, `messageID`)
has **no setter**, because those values must never change once the object exists. The other
attributes do have setters.

---

# Part 2 - Setting it up (do this once, before the demonstration)

## 2.1 What you need

Nothing to install. The project carries its own Java (JDK 21) in `tools/jdk`, so a clean
Windows machine is enough.

The one thing that has to be running is **MySQL**, because that is where the data is kept.

## 2.2 Start MySQL

Run the helper script. It starts MySQL if it is stopped and then checks that the tool can
reach the database:

```bat
start-mysql.bat
```

You should see:

```
[1/4] MySQL tools ready: C:\Program Files\MySQL\MySQL Server 8.0\bin
[2/4] The MySQL80 service is already running.
[3/4] Port 3306 is open, MySQL is accepting connections.
[4/4] Testing the connection with the command line interface...

Signed in as drKasi (INSTRUCTOR).

==========================================================
 Everything is ready. You can start the demonstration:

   cli.bat --script demo-full.txt
```

### If MySQL is not running

The helper will tell you:

```
[2/4] The MySQL80 service is not running. Trying to start it...
      Could not start it from this window.

      This is normal if the terminal is not running as Administrator.
```

**This is not a fault in the program.** Starting a Windows service needs Administrator rights,
and a normal terminal does not have them. There are three ways round it:

| Method | How |
| --- | --- |
| The easy one | Open the Start menu, search for `services.msc`, find `MySQL80`, right click it, choose **Start** |
| PowerShell | Open PowerShell as Administrator, then run `Start-Service MySQL80` |
| From a normal terminal | Run `sc start MySQL80` from an Administrator command prompt |

MySQL is also set to **Automatic**, so it starts by itself when you turn the computer on. You
only need to do this after an unusual shutdown.

### If the `mysql` command is not found

The MySQL tools are already on your user path, but a terminal that was **already open** when
they were added will not see them. The helper script adds them for the current window, so the
`mysql` command works straight away.

If you want it in every new window, close the terminal and open a new one.

### A quick check by hand

```bat
mysql -u root -p -e "SELECT COUNT(*) FROM discussion_tool.Dicussion"
```

It asks for the password, and it prints the number of discussions. That proves the server is
up and the database is there.

## 2.3 Check the database settings

Open `java-backend/db.properties`:

```properties
db.host=127.0.0.1
db.port=3306
db.name=discussion_tool
db.user=root
db.password=your-password-here
```

Put your real MySQL password on the `db.password` line. If you would rather not keep the
password in a file, delete that line and set the environment variable `MYSQL_PASSWORD` instead.

## 2.4 Build the project

```bat
cd online disscussion project\java-backend
build.bat
```

You should see:

```
Compiling the main sources...
Compiling the test sources...
Build finished.
```

## 2.5 Run the automated tests

```bat
test.bat
```

You should see:

```
[         39 tests found           ]
[         39 tests successful      ]
[          0 tests failed          ]
```

**How to talk about this:** "I wrote 39 automated tests. Every test runs against the real MySQL
database, not a fake one. Each test creates its own data and deletes it again, so the tests can
be run over and over and never damage the real data."

---

# Part 3 - The accounts you can sign in with

The design says "the instructor account is assumed to exist by the time of installation". These
accounts are already in the database.

| Who | User name | Password | Notes |
| --- | --- | --- | --- |
| Instructor | `drKasi` | `Instructor@123` | Owns the course, can do everything |
| Student | `jdoe23` | `Student@123` | Student number 1, John Doe |
| Student | `arahman17` | `Student@123` | Aisha Rahman |
| Student | `dkim41` | `Student@123` | Daniel Kim |

---

# Part 4 - How to drive the program

There are three ways. Use whichever suits the moment.

## 4.1 Interactive mode - best for a live demonstration

```bat
cli.bat
```

You get a prompt. Type a command, press Enter, read the answer, type the next one.

```
Online Discussion Tool - command line interface
...
Type 'help' for the command list, 'exit' to quit.

> login drKasi Instructor@123
Signed in as drKasi (INSTRUCTOR).
> listCourses
SWE-101 - Software Engineering (16 students, 32 discussions)
> exit
Goodbye.
```

Type `help` at any time to see every command.

## 4.2 Script mode - best for a smooth, rehearsed demonstration

```bat
cli.bat --script demo-full.txt
```

This runs a prepared file of commands from top to bottom. Because the whole demonstration is in
one file, nothing can go wrong in front of your professor.

**How to talk about this:** "The command line interface is a test harness for the API. It lets
every function be called exactly the way the front end will call it later, so the front end only
has to send the same requests."

## 4.3 One command at a time

```bat
cli.bat login drKasi Instructor@123
```

Note: each run of this form starts a fresh session, so sign in again each time. Script mode and
interactive mode keep the session, so use one of those for a demonstration.

---

# Part 5 - The functions, one by one, with real output

Everything below is copied from an actual run.

---

## 5.1 Signing in and out (for everyone)

### Function 1 - `login`

Signs a user in and works out whether they are the instructor or a student.

**Show this:**

```
> login drKasi Instructor@123
Signed in as drKasi (INSTRUCTOR).
```

**And as a student:**

```
> login jdoe23 Student@123
Signed in as jdoe23 (STUDENT).
> whoami
Signed in as jdoe23 (STUDENT), student 1
```

**And a wrong password, to show it is checked:**

```
> login drKasi WrongPassword123
Login failed: the user name or the password is wrong.
```

**How to talk about this:** "The password is never stored as plain text. It is hashed with
bcrypt when it is saved and checked against the hash when you sign in. So even someone who can
read the database cannot see the passwords."

### Function 2 - `logout`

```
> logout
Signed out.
> whoami
Nobody is signed in.
```

---

## 5.2 The instructor's functions

### Function 3 - `createCourse`

Creates the course that holds the students and the discussions.

```
> createCourse Software Engineering SWE-101
Created the course Software Engineering (SWE-101).
```

**Note:** the course number is the last word, so a course name with spaces works.

### Function 4 - Looking at the course

```
> listCourses
SWE-101 - Software Engineering (16 students, 32 discussions)
```

```
> listStudents
Enrolled students: 16
  1 | John Doe | username: jdoe23
  5 | mona lis | username: mlis61
  9 | Victor Liu | username: vliu90
  24 | Victor Lirllu | username: vlirllu55
  31 | Amir Hassan | username: ahassan
  ...
```

### Function 5 - `createDicussion`

Creates a discussion thread.

```
> createDicussionCapture demo Discussion patterns in Java
Created the discussion "Discussion patterns in Java" with the identifier 186 (saved as %demo%).
```

**What is `%demo%`?** The script saves the new identifier under the name `demo`, so it can use
it later without anyone typing a number by hand. The same idea is used in the script for the
message and student identifiers.

### Function 6 - `searchDiscussionByID` and `searchDiscussionByTopic`

The same discussion, found two different ways.

```
> searchDiscussionByID 186
Discussion 186 | topic: Discussion patterns in Java | created: Oct-29-2026:17:12:55
  Messages: 0
```

```
> searchDiscussionByTopic Discussion patterns in Java
Discussions: 1
  186 | Discussion patterns in Java | Oct-29-2026:17:12:55 | 0 messages
```

### Function 7 - `postMessage`

The instructor starts the conversation.

```
> postMessage 186 Welcome to the discussion about design patterns.
Posted the message to the discussion 186.
```

### Function 8 - Browsing every discussion

```
> listDiscussions
Discussions: 33
  186 | Discussion patterns in Java | Oct-29-2026:17:12:55 | 1 messages
  44 | software engineer | Oct-15-2026:21:04:21 | 1 messages
  43 | software computer database | Oct-15-2026:21:00:46 | 1 messages
  41 | Discussion 1: Database Normalization | Oct-14-2026:18:37:18 | 3 messages
  40 | SmartShelf | Oct-08-2026:21:16:23 | 2 messages
  ...
```

### Function 9 - `retrieveFirstRequesttoAdd` and `retrieveFirstRequestToRemove`

These show the students who are waiting to join or to leave. The queue is served oldest request
first.

```
> listEnrollRequests
Enrolment requests: 3
  99 | kamil Laimaa | username: (none)
  154 | kamkiil Laiddmaa | username: (none)
  502 | New Student | username: newStudent
> firstRequesttoAdd
First enrolment request: 99 | kamil Laimaa | username: (none)
```

```
> listRemovalRequests
Removal requests: 0
> firstRequestToRemove
No removal request is waiting.
```

**How to talk about this:** "The instructor does not add or remove students directly. Students
ask to join or to leave, and the instructor approves the requests one at a time. That is the
`retrieveFirstRequesttoAdd` and `addStudent` pair working together."

### Function 10 - `searchStudentByID` and `searchStudentByUsername`

```
> searchStudentByID 1
1 | John Doe | username: jdoe23
> searchStudentByUsername jdoe23
1 | John Doe | username: jdoe23
```

### Function 11 - `addStudent`

Enrols a student. This is also how a pending request is approved.

```
> addStudent 502 New Student newStudent
Added the student 502 to SWE-101.
> searchStudentByID 502
502 | New Student | username: newStudent
```

**And the request disappears from the queue, which proves it was approved:**

```
> listEnrollRequests
Enrolment requests: 2
  99 | kamil Laimaa | username: (none)
  154 | kamkiil Laiddmaa | username: (none)
```

### Function 12 - `removeStudentbyID` and `removeStudentByUsername`

Removing by student number:

```
> addStudent 500 Demo Student demoUser
Added the student 500 to SWE-101.
> removeStudentbyID 500
Removed the student 500.
> searchStudentByID 500
No student has the identifier 500.
```

Removing by user name:

```
> addStudent 501 Demo Student demoUser2
Added the student 501 to SWE-101.
> searchStudentByUsername demoUser2
501 | Demo Student | username: demoUser2
> removeStudentByUsername demoUser2
Removed the student demoUser2.
> searchStudentByUsername demoUser2
No student has the user name demoUser2.
```

**How to talk about this:** "Removing a student also clears the queue rows and the enrolment row
that refer to them, so there are no leftovers in the database. The discussion messages are kept,
because a message belongs to the discussion rather than to the student."

---

## 5.3 The student's functions

Sign out and sign in as the student:

```
> logout
Signed out.
> login jdoe23 Student@123
Signed in as jdoe23 (STUDENT).
```

### Function 13 - `searchDiscussionByTopic` and `searchDiscussionByID`

The student can read the discussions.

```
> searchDiscussionByTopic Discussion patterns in Java
Discussions: 1
  186 | Discussion patterns in Java | Oct-29-2026:17:12:55 | 1 messages
```

### Function 14 - `postMessage`

```
> postMessageToTopic Discussion patterns in Java I agree, the factory method is the best choice here.
Posted the message to "Discussion patterns in Java" (186).
```

**How to talk about this:** "The student never writes their own name into the message. The
backend takes the name from the signed-in session, so a student cannot post as somebody else."

### Function 15 - `searchMessageByID` and `searchMessageByUsername`

```
> searchMessageByUsername 186 drKasi
  Messages: 1
    189 | drKasi | Oct-29-2026:17:12:55 | Welcome to the discussion about design patterns.
```

```
> searchMessageByUsername 186 jdoe23
  Messages: 1
    192 | jdoe23 | Oct-29-2026:17:12:57 | I agree, the factory method is the best choice here.
```

### Function 16 - `requestToEnroll`

A student asks to join the course.

```
> requestToEnroll 502 New Student newStudent
Requested to enrol the student 502.
```

### Function 17 - `requestToDrop`

A student asks to leave the course.

```
> requestToDrop 1
Requested to drop the student 1.
```

**How to talk about this:** "A student cannot remove themselves straight away. They ask, and
the instructor decides. That protects the class list from accidents."

---

## 5.4 The permissions - the part to show with care

This is the clearest proof that the design was implemented properly.

**A student tries to use an instructor's function:**

```
> listEnrollRequests
Error: Only the instructor may perform this action.
```

**How to talk about this:** "Every function checks who is signed in before it does anything. A
student cannot create a discussion, cannot add or remove students, cannot remove messages, and
cannot archive a discussion. The reverse is also true: the instructor cannot use the student's
join or leave functions. The automated tests check both directions."

---

## 5.5 Messages

### Function 18 - A message with a picture

`textualContents` and `imageContents` are both nullable, but a message must have one of them.

```
> postImageMessage 186 sample-picture.png Here is the UML diagram we drew.
Posted the image message to the discussion 186.
```

**How to talk about this:** "The picture is stored in the database as bytes, in the same table as
the text. The file name and the file type are stored next to it. The web server can send the
bytes back to the browser as an image."

### Function 19 - `removeMessage`

Only the instructor may do this.

```
> findMessage toRemove 190 jdoe23
Found the message 204 (saved as %toRemove%).
> removeMessage 190 %toRemove%
Removed the message 204.
> searchMessageByUsername 190 jdoe23
  Messages: 0
```

**Note for the demonstration:** the message numbers are given out by the database, so they are
not fixed. The `findMessage` command looks the number up for you and saves it under a name.

---

## 5.6 The archive

### Function 20 - `archiveDiscussion`

A discussion that is finished moves into the archive.

```
> archiveDiscussion 186
Archived the discussion 186.
```

**It leaves the normal list:**

```
> listDiscussions
Discussions: 32
  44 | software engineer | Oct-15-2026:21:04:21 | 1 messages
  43 | software computer database | Oct-15-2026:21:00:46 | 1 messages
  ...
```

**And it appears in the archive:**

```
> listArchived
Discussions: 2
  186 | Discussion patterns in Java | Oct-29-2026:17:12:55 | 3 messages
  4 | Previous semester project examples | Oct-02-2026:19:10:06 | 1 messages
```

**And the student can read the archive too:**

```
> login jdoe23 Student@123
Signed in as jdoe23 (STUDENT).
> listArchived
Discussions: 2
  186 | Discussion patterns in Java | Oct-29-2026:17:12:55 | 3 messages
  4 | Previous semester project examples | Oct-02-2026:19:10:06 | 1 messages
```

**One more good point to mention:** an archived discussion is closed. Posting a new message to it
is refused, and the tests check that too.

---

# Part 6 - The full list of functions, for your reference

## For everyone

| Function | What it does | Example |
| --- | --- | --- |
| `login` | Signs in and works out the role | `login drKasi Instructor@123` |
| `logout` | Signs out | `logout` |
| `whoami` | Shows who is signed in | `whoami` |

## The instructor's functions

| Function | What it does | Example |
| --- | --- | --- |
| `createCourse` | Creates the course | `createCourse Software Engineering SWE-101` |
| `retrieveFirstRequesttoAdd` | First student waiting to join | `firstRequesttoAdd` |
| `retrieveFirstRequestToRemove` | First student waiting to leave | `firstRequestToRemove` |
| `addStudent` | Enrols a student, approves a request | `addStudent 502 New Student newStudent` |
| `removeStudentbyID` | Removes a student by student number | `removeStudentbyID 500` |
| `removeStudentByUsername` | Removes a student by user name | `removeStudentByUsername demoUser2` |
| `searchStudentByID` | Finds a student by student number | `searchStudentByID 1` |
| `searchStudentByUsername` | Finds a student by user name | `searchStudentByUsername jdoe23` |
| `createDicussion` | Creates a discussion | `createDicussion Discussion patterns in Java` |
| `searchDiscussionByID` | Finds a discussion by identifier | `searchDiscussionByID 186` |
| `searchDiscussionByTopic` | Finds discussions by topic | `searchDiscussionByTopic Java` |
| `postMessage` | Posts a message | `postMessage 186 Welcome everyone.` |
| `removeMessage` | Removes a message | `removeMessage 186 %m%` |
| `searchMessageByID` | Finds a message by identifier | `searchMessageByID 186 189` |
| `searchMessageByUsername` | Finds messages by user name | `searchMessageByUsername 186 drKasi` |
| `archiveDiscussion` | Moves a discussion to the archive | `archiveDiscussion 186` |

## The student's functions

| Function | What it does | Example |
| --- | --- | --- |
| `requestToEnroll` | Asks to join the course | `requestToEnroll 502 New Student newStudent` |
| `requestToDrop` | Asks to leave the course | `requestToDrop 1` |
| `searchDiscussionByID` | Finds a discussion by identifier | `searchDiscussionByID 186` |
| `searchDiscussionByTopic` | Finds discussions by topic | `searchDiscussionByTopic Java` |
| `postMessage` | Posts a message | `postMessage 186 I agree.` |
| `searchMessageByID` | Finds a message by identifier | `searchMessageByID 186 189` |
| `searchMessageByUsername` | Finds messages by user name | `searchMessageByUsername 186 drKasi` |

## Extra commands that help at the terminal

| Command | What it does |
| --- | --- |
| `listCourses` | Every course |
| `listStudents` | The students in the current course |
| `listDiscussions` | Every discussion that is not archived |
| `listArchived` | Every archived discussion |
| `listEnrollRequests` | Every student waiting to join |
| `listRemovalRequests` | Every student waiting to leave |
| `setCourse <number>` | Chooses which course to work on |
| `findDiscussion <name> <topic>` | Finds a discussion and saves its identifier |
| `findMessage <name> <id> <username>` | Finds a message and saves its identifier |
| `createDicussionCapture <name> <topic>` | Creates a discussion and saves its identifier |
| `set <name> <value>` | Saves a value under a name |
| `echo <text>` | Prints a line |
| `help` | Shows every command |

---

# Part 7 - The command line commands, complete list

Run `cli.bat` and type `help` to see this list on the screen.

**For all users**

```
login <username> <password>          sign in
logout                              sign out
whoami                              show the signed in user
```

**Instructor only**

```
createCourse <name> <number>        create a course
setCourse <courseNumber>            choose the course to act on
firstRequesttoAdd                    first pending enrolment request
firstRequestToRemove                 first pending removal request
addStudent <id> <first> <last> [user] enrol a student
removeStudentbyID <id>               remove a student by identifier
removeStudentByUsername <username>   remove a student by user name
createDicussion <topic>              create a discussion
archiveDiscussion <id>               archive a discussion
listStudents                         the enrolled students
listEnrollRequests                   the pending enrolment requests
listRemovalRequests                  the pending removal requests
listCourses                          every course
```

**Student only**

```
requestToEnroll <id> <first> <last> [user]  request to join the course
requestToDrop <studentID>            request to leave the course
```

**Shared**

```
searchStudentByID <id>               find a student by identifier
searchStudentByUsername <username>   find a student by user name
searchDiscussionByID <id>            find a discussion by identifier
searchDiscussionByTopic <topic>      find discussions by topic
listDiscussions                      every discussion that is not archived
listArchived                         every archived discussion
postMessage <discussionID> <text>    post a message
postMessageToTopic <topic> <text>    post a message, naming the discussion by its topic
postImageMessage <id> <path> [text]  post a message with a picture
removeMessage <discussionID> <msgID>  remove a message
searchMessageByID <id> <msgID>       find a message by identifier
searchMessageByUsername <id> <user>  find messages by user name
```

**Only in a script**

```
set <name> <value>                 save a value under a name
findDiscussion <name> <topic>       save the identifier of a discussion
findMessage <name> <id> <username>  save the identifier of a message
createDicussionCapture <name> <topic>  create a discussion and save its identifier
echo <text>                         print a line
```

**A note on text with spaces.** You do not need quotes:

```
postMessage 186 Here is a longer reply that has spaces in it.
```

---

# Part 8 - The database

The project already had a MySQL database built for the professor's rubric. This backend uses it
as it is, so nothing had to be thrown away.

| Table | What it holds |
| --- | --- |
| `LoginCredential` | User name, password hash, role |
| `StudentRecord` | The student rows |
| `EnrollQueue` | Students waiting to join |
| `RemoveStudentQueue` | Students waiting to leave |
| `Dicussion` | The discussions |
| `Message` | The messages, with the optional picture |
| `DiscussionToMessage` | Which messages belong to which discussion |
| `ArchivedDicussions` | The archived discussions and their text files |
| `ReuqestToCreateDiscussion` | Students asking for a new discussion |
| `RequestToRetrieveDiscussion` | Students asking for an archived discussion |
| `course` | **Added** - the course |
| `course_student` | **Added** - the students in a course |

## Why two tables were added

The rubric database has no course table. A discussion just holds the course as free text, and
the class list lives in the request queues. But the class design asks for a `Course` class that
holds a list of students and a list of discussions. So
[010_course_bridge.sql](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/database/010_course_bridge.sql>)
adds `course`, `course_student` and a `CourseNumber` column on `Dicussion`.

Nothing was renamed and nothing was dropped, so the professor's ER diagram and the existing web
server both keep working. That was checked by running the old Node server against the new
database.

## Three problems the schema caused, and how they were solved

These are good points to mention, because they show the design was tested against a real
database and not only against a plan.

1. **Timestamps moved by six hours.** The Java runtime was in `America/Chicago` and the MySQL
   server was in UTC, so every time was written six hours out. Fixed by writing the timestamps
   as plain text, so the driver never shifts them.

2. **Two discussions created in the same second failed.** The `Dicussion` table has a rule that
   the creation time must be different every time. Fixed by moving the time forward a second
   until a free slot is found.

3. **A student could not be saved.** The student table points at the login table, so the login
   had to exist first. Fixed by creating the login automatically, with a generated password.

---

# Part 9 - The tests

```bat
test.bat
```

```
[         39 tests found           ]
[         39 tests successful      ]
[          0 tests failed          ]
```

## What is covered

| Area | Examples of what is checked |
| --- | --- |
| Signing in | Correct password, wrong password, unknown user, empty fields, for both roles |
| The instructor | Creates a course, adds a student, finds a student two ways, removes a student two ways |
| The request queues | The student asks to join and to leave, the instructor sees it and approves it |
| Discussions | Created, found by identifier and by topic, archived, leaves the active list |
| Messages | Posted as instructor and as student, with text, with a picture, with both, removed, found two ways |
| Refusals | An archived discussion takes no new messages, an empty message is refused |
| Permissions | A student can never reach an instructor's function, and the reverse |
| The classes | The collections, the searches and the getters on their own |

## Why the tests can be run again and again

Every test creates its own rows and deletes them again. Running the suite twice in a row leaves
the database in exactly the same state, which was checked by comparing the row counts before
and after.

**Good point to make:** the tests run against the **real MySQL database**, not a fake one. So
they prove the SQL, the foreign keys and the unique rules all work.

---

# Part 10 - A five minute demonstration plan

If you only have a short time, do this.

| Step | Command | What to point at |
| --- | --- | --- |
| 1 | `cli.bat --script demo-full.txt` | The whole thing runs by itself |
| 2 | Point at Part 1 of the output | The instructor signs in, a wrong password is refused |
| 3 | Point at Part 2 | The instructor creates a discussion and posts the first message |
| 4 | Point at Part 3 | The student signs in, reads it, and replies |
| 5 | Point at Part 3.7 | **The student is refused an instructor's function** |
| 6 | Point at Part 4 | The instructor approves the student's request to join |
| 7 | Point at Part 5.2 | A message with a picture |
| 8 | Point at Part 5.3 | The discussion is archived and moves to the archive list |
| 9 | Run `test.bat` | **39 tests, all passing** |

## If your professor asks a question

**"Why is there a command line interface and not a website?"**
The task said not to connect the front end yet. The command line interface is the test harness
for the API. When the front end is connected, it sends the same requests to the same functions.

**"How do you know who is allowed to do what?"**
The `OnlineDiscussionMain` class keeps the signed-in user. Every function checks the role before
it does anything. A student cannot create a discussion, remove a student, remove a message or
archive a discussion. The tests check both directions.

**"What happens if two people post at the same time?"**
Each message and each discussion is written in a database transaction. If a step fails, the
transaction is rolled back, so half-finished data is never saved.

**"Where are the passwords?"**
Hashed with bcrypt, never stored as plain text. That is why `LoginCredential.Password` holds a
long string like `$2b$12$...`.

**"Does this work with the database that was already there?"**
Yes. The existing rubric tables are used exactly as they were. Two tables were added to support
the `Course` class. The old Node server was run against the new database to check that it still
works.

**"How do you know the tests are testing the real thing?"**
They run against the real MySQL database. Each test creates its own data and removes it again,
so the suite can be run any number of times.

---

# Part 11 - Files to show your professor

| File | What it shows |
| --- | --- |
| [README.md](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/java-backend/README.md>) | The technical version of this document |
| [start-mysql.bat](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/java-backend/start-mysql.bat>) | Starts MySQL and checks the connection |
| [OnlineDiscussionMain.java](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/java-backend/src/main/java/com/onlinediscussion/core/OnlineDiscussionMain.java>) | The public API, the instructor's and the student's functions, and the permission checks |
| [MySQLDBInterface.java](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/java-backend/src/main/java/com/onlinediscussion/db/MySQLDBInterface.java>) | Every SQL statement |
| [OnlineDiscussionToolTest.java](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/java-backend/src/test/java/com/onlinediscussion/OnlineDiscussionToolTest.java>) | The 39 tests |
| [demo-full.txt](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/java-backend/demo-full.txt>) | The demonstration script |
| [010_course_bridge.sql](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/database/010_course_bridge.sql>) | The database change |
| [design.md](</C:/Users/secur/OneDrive/Desktop/ta/online disscussion project copyyyy/online disscussion project/database/design.md>) | The database design and the entity relationships |
