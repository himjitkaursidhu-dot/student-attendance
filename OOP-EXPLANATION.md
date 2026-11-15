# Explaining this project in your OOP viva

## Start with the flow

“When the user saves attendance, the browser sends JSON to `AttendanceController`. The controller calls `AttendanceService`. Spring provides `AttendanceServiceImpl`, which validates the entry and calls `AttendanceRepository`. The repository runs parameterized SQL using JdbcTemplate. The database stores the result and the response returns to the browser.”

Trace that flow in the source before your presentation. You should be able to point to each class, not just repeat the description.

## Encapsulation

`Student`, `Subject`, and `Attendance` keep their fields private. Their public getters and setters provide controlled access. For example, other classes read a student's name through `getName()` instead of accessing the field directly.

The request constraints, such as `@NotBlank` and `@Min(1)`, help reject invalid input. The service also normalizes email addresses and subject codes. Getters and setters alone do not enforce every business rule.

## Abstraction

`StudentService`, `SubjectService`, and `AttendanceService` are interfaces. A controller asks for a service operation without needing to know how SQL is written or which database is used.

For example, the attendance service exposes `markAttendance(...)`; the controller does not need the implementation details of checking references or correcting an existing record.

## Polymorphism

`AttendanceController` stores a dependency with the interface type `AttendanceService`. At runtime, Spring injects an `AttendanceServiceImpl` object. A call through the interface executes the implementation's overridden method.

The `@Override` methods in service implementation classes are concrete places to show this.

## Inheritance

`ResourceNotFoundException extends RuntimeException` is an example of class inheritance in this project. Service classes **implement interfaces**, which is not the same as extending another concrete class.

The entity classes do not form a student/subject inheritance hierarchy. Those are different concepts, so forcing inheritance between them would be misleading. Most collaboration here uses composition: a service has repositories, and a controller has a service.

## Constructors and dependency injection

The model classes retain default and parameterized constructors. Default constructors support JSON-to-object creation, while parameterized constructors are useful for row mapping and tests.

Controllers, services, and repositories receive dependencies through their constructors. Spring creates and connects these objects. `final` dependency fields help prevent accidentally replacing them later.

## Other Java concepts visible here

- **Collections:** `List<Student>` and `List<Attendance>` store multiple objects; a `Set` detects duplicate entries inside a batch.
- **Exception handling:** `ApiExceptionHandler` converts validation, missing-record, and database errors into HTTP responses.
- **Records:** `AttendanceReport` and `AttendanceView` are immutable output DTOs. They are report values, not database entities managed by an ORM.
- **Lambdas:** RowMapper lambdas turn rows in a JDBC ResultSet into Java objects.
- **Transactions:** `@Transactional` keeps a batch from being partly saved if a failure occurs.

## JDBC and JdbcTemplate

JDBC is Java's database access API. A driver connects it to a database such as MySQL or H2.

JdbcTemplate is Spring's helper around JDBC. It opens and closes JDBC resources, runs SQL, binds query parameters, and translates database exceptions. SQL is still written explicitly in the repository classes. This project does **not** use JPA or Hibernate.

Examples to show:

- `jdbc.update(...)` for INSERT, UPDATE, and DELETE.
- `jdbc.query(..., rowMapper, parameters...)` for several rows.
- `jdbc.queryForObject(...)` for a single calculated value.
- `GeneratedKeyHolder` for an auto-generated student or subject ID.

The `?` placeholders are bound parameters. User values are not concatenated into SQL. Dynamic filter fragments are fixed code; their values are still bound separately.

## Database relationships

`students` has primary key `student_id`. `subjects` has primary key `subject_id`. Each attendance row has foreign keys to one student and one subject.

A unique constraint on `(student_id, subject_id, attendance_date)` prevents duplicate class records. Foreign keys prevent attendance for nonexistent students or subjects. Cascading deletes remove related attendance when its student or subject is deleted.

## Calculations to practise

- 3 present and 1 absent: `3 / 4 × 100 = 75%`, not below the threshold.
- 2 present and 2 absent: `2 / 4 × 100 = 50%`, below the threshold.
- No recorded classes: show **No data**. Do not divide by zero or classify this as a defaulter.
- Correcting absent to present changes the numerator, not the total class count.

The SQL compares counts before rounding when selecting below-75% rows. Rounding is only for display.

## Questions your teacher may ask

**Why use layers?**
To keep HTTP input/output, business rules, and SQL separate, so each part is easier to understand, test, and change.

**Why keep both Java validation and database constraints?**
Java validation gives readable feedback. Database constraints protect consistency even when data is changed outside the browser.

**How is attendance updated?**
The repository looks for the student/subject/date combination with an UPDATE. If no row is updated, it inserts a new row. A database unique constraint is the final safeguard against duplicates.

**How do you search?**
A parameterized SELECT uses case-insensitive LIKE matching on name/email, or an exact match on the numeric student ID.

**What if the database is unavailable?**
At startup the datasource cannot initialize; during requests database failures are logged and the API returns a generic error rather than SQL details.

**What would you improve next?**
Add teacher login and roles, course enrollments, class-session IDs for multiple lectures on one day, an audit trail, pagination, and controlled retention of deleted records. The current version is intentionally a local coursework demonstration.
