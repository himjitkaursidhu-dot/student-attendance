# Student Attendance Management System

A Java coursework project built with Spring Boot and JdbcTemplate. It keeps the original controller → service → repository structure and adds the missing database setup, validation, subject management, reports, and browser workflows.

## Run on Windows

1. Install a **JDK 17 or newer** if one is not already installed. Java 21 works too.
2. Open this folder, the one containing `pom.xml`.
3. Double-click `start.bat`. Leave its terminal open.
4. Once the console says the application has started, open **http://localhost:8080**.
5. Stop the application with **Ctrl+C** in its terminal.

The completed ZIP includes a built JAR, and `start.bat` uses it without downloading Maven dependencies. If that JAR is absent, the launcher builds/runs from source; that first source run needs internet access to download Maven and the Java dependencies. The Maven wrapper is included, so a separate Maven installation is not required. If Windows blocks the launcher, run the commands below in a terminal instead. If `start.bat` cannot locate your JDK, set `JAVA_HOME` to the JDK installation folder, not its `bin` folder.

PowerShell alternative:

```powershell
# Only needed when JAVA_HOME has not already been configured:
$env:JAVA_HOME = 'C:\path\to\your\jdk'
.\mvnw.cmd spring-boot:run
```

macOS/Linux, with a JDK installed:

```sh
sh mvnw spring-boot:run
```

### Default database: H2 through JDBC

The default configuration uses a real, local, file-based H2 database. No separate database server is needed. Data persists in `data/attendance.mv.db` between restarts. Schema creation runs automatically from `src/main/resources/schema.sql`; no sample student records are inserted automatically.

H2 still uses JDBC and JdbcTemplate. It is not a JavaScript mock database or a JPA implementation. The H2 web console is disabled. Tests use an isolated, in-memory H2 database and do not change your saved demo data.

## Use MySQL instead

Use MySQL 8+ if your teacher expects MySQL specifically. Create a **new, empty database** first. Do not point this version at an old database without reviewing its schema and backing it up. `CREATE TABLE IF NOT EXISTS` creates missing tables; it does not migrate tables or repair old duplicate records.

```sql
CREATE DATABASE student_attendance_db;
```

Set the connection values in PowerShell, using your actual MySQL account:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/student_attendance_db'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'your-local-mysql-password'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=mysql'
```

If that database name is already in use, create a different empty database and change `DB_URL`. The MySQL profile creates the required tables using the same schema. Credentials belong in local environment variables, not in source code or your submission. H2 and MySQL hold separate data; changing profiles does not transfer records.

## What each requirement maps to

| Teacher's requirement | Implementation |
|---|---|
| Add, update, delete, search students | Students screen, `StudentController`, `StudentService`, `StudentRepository` |
| Subject-wise attendance | Subjects screen, subject selector, `subject_id` foreign key |
| Date-wise attendance | Attendance screen and date-range record/report filters |
| Automatic percentages | SQL aggregation in `AttendanceRepository` |
| Retrieve students below 75% | Reports screen and `/api/attendance/defaulters` |
| JDBC connectivity | H2/MySQL JDBC drivers and Spring datasource configuration |
| Spring Boot and JdbcTemplate | Boot application, constructor-injected JdbcTemplate repositories |

## Attendance rules you should explain

- One subject has at most **one class per calendar date** in this version.
- Each student can have one record per subject/date: `PRESENT` or `ABSENT`.
- Saving the same student/subject/date again corrects the status; it does not add another class.
- Percentage = **present recorded classes ÷ total recorded classes × 100**. For example, 3 present out of 4 recorded classes is 75%.
- `ABSENT` counts in the denominator. An **unmarked** class is not silently treated as absent. Mark the complete class list for reliable reports.
- A student-subject pair with no records shows **No data**, not a defaulter. The API represents its percentage as 0 with `totalClasses = 0`; the total distinguishes it from 0% after recorded absences.
- “Below 75%” means **strictly less than 75**, calculated before rounding. Exactly 75% is not included.
- Reports are subject-wise. A student can meet the threshold in one subject and fall below it in another. A student may therefore have multiple report rows.
- From/to filters include both dates and recalculate the percentage for the selected period.
- Future attendance dates, invalid statuses, missing students/subjects, and repeated entries within a batch are rejected.
- Batch attendance is transactional: all entries save together or none do.
- Deleting a student or subject also removes its attendance, enforced by foreign keys. The browser asks for confirmation.

## Quick classroom demonstration

1. Add two fictional students in Students.
2. Add a subject called Object-Oriented Programming with code `OOP101`.
3. Choose that subject and a past date in Attendance. Explicitly mark each student's status and save.
4. Repeat for four different past dates: give the first student 3 present and 1 absent, and the second 2 present and 2 absent.
5. Open Reports. Their percentages should be **75%** and **50%**. Only the second should appear in Below 75%.
6. Reopen one date and correct the second student's absence to present. The result becomes 75%, without increasing the number of recorded classes.
7. Try searching and editing a student, and filtering records or reports by date.

Use fictional data for the presentation. The default app starts empty so it does not mix invented records with your own records.

## API reference

All requests use the same local server. POST/PUT bodies are JSON. Error responses use `{ "message": "..." }` and an appropriate HTTP status.

| Method | URL | Purpose |
|---|---|---|
| GET | `/api/students` | List students |
| GET | `/api/students/search?name=aman` | Search name/email or exact numeric ID |
| GET | `/api/students/{id}` | Find one student |
| POST | `/api/students` | Create student; returns saved student with generated ID |
| PUT | `/api/students/{id}` | Update student |
| DELETE | `/api/students/{id}` | Delete student and attendance |
| GET/POST | `/api/subjects` | List/create subjects |
| GET/PUT/DELETE | `/api/subjects/{id}` | Find/update/delete subject |
| POST | `/api/attendance` | Save or correct a single attendance entry |
| POST | `/api/attendance/batch` | Save an array of entries atomically, maximum 1000 |
| GET | `/api/attendance` | Joined records with names |
| DELETE | `/api/attendance/{id}` | Remove an incorrectly recorded entry |
| GET | `/api/attendance/student/{studentId}` | Raw attendance for one student |
| GET | `/api/attendance/percentage/{studentId}/{subjectId}` | Percentage as a number |
| GET | `/api/attendance/reports` | All student-subject report rows, including no-data pairs |
| GET | `/api/attendance/defaulters` | Only recorded pairs strictly below 75% |

`GET /api/attendance`, `/reports`, and `/defaulters` accept optional `studentId`, `subjectId`, `from`, and `to` query parameters. Dates use `YYYY-MM-DD`.

Student body:

```json
{"name":"Aman","email":"aman@example.test","department":"CSE","semester":3}
```

Subject body:

```json
{"subjectName":"Object-Oriented Programming","subjectCode":"OOP101"}
```

Attendance body, using IDs returned when you create the student and subject:

```json
{"studentId":1,"subjectId":1,"attendanceDate":"2024-01-01","status":"PRESENT"}
```

## Build and tests

```powershell
.\mvnw.cmd clean verify
java -jar target\student-attendance-1.0.0.jar
```

After a successful build, the executable JAR includes both the Java backend and the browser interface. For MySQL, add `--spring.profiles.active=mysql` to the JAR command after setting the database environment variables.

The tests cover student CRUD/search, subject operations, duplicate constraints, attendance corrections, strict threshold calculation, no-data reports, subject/date filtering, input validation, batch rejection without partial writes, and cascading deletions. See `VERIFICATION.md` for the checks actually run on the delivered copy.

## Project layout

```text
src/main/java/com/attendance/
  StudentAttendanceApplication.java
  controller/    HTTP endpoints
  service/       Interfaces and business rules
  repository/    Parameterized SQL through JdbcTemplate
  model/         Encapsulated entities and immutable report DTOs
  exception/     Consistent error responses
src/main/resources/
  application.properties          H2 demo configuration
  application-mysql.properties    Optional MySQL configuration
  schema.sql                      Tables and integrity constraints
  static/                         Browser interface
src/test/java/com/attendance/      Automated integration tests
```

## Scope and limitations

This is a **local academic project**, not a production university system. The server binds to localhost. It has no login, roles, audit history, timetable, course enrollment, or multiple class periods per subject per date. The marking screen uses the entire stored student list; use one cohort for the demonstration rather than mixing unrelated course groups. Reports list student-subject combinations, so some can show no data when a subject does not apply to a student. Use a fresh database and fictional records for the demonstration.

A real deployment would need those features, access controls, backups, and a security review. Do not expose this unauthenticated demo to the internet.

For your viva, read `OOP-EXPLANATION.md` and walk through the code yourself. Be clear about which parts you started and which changes you received help with.
