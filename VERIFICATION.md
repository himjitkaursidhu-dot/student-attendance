# Verification

## Automated checks

The first full integration-test run completed on 14 November 2026 using the installed JDK 21 and Maven wrapper, compiling the application for Java 17.

Result: **12 tests, 0 failures, 0 errors, 0 skipped**.

The tests run a real Spring Boot context, JdbcTemplate repositories, SQL schema, and an isolated H2 database. MockMvc exercises HTTP controllers without writing to the saved local database.

Covered:

- Student create/read/update/delete, name/email/ID search, and email normalization.
- Invalid student input, duplicate email, and missing student responses.
- Subject create/update/delete, uppercase codes, and duplicate code rejection.
- Re-saving one student/subject/date corrects attendance without adding a class.
- Exactly 75% excluded from below-75% results; 60% included.
- Zero-record reports distinguished from recorded 0% attendance.
- Subject isolation, inclusive date filters, and invalid date filters.
- Invalid batches produce no partial saved records.
- Batch endpoint, invalid statuses, future dates, empty/null batch entries.
- Student/subject deletion cascades, missing delete responses, and recalculation after attendance deletion.

The simplified frontend passed JavaScript syntax and HTML/JavaScript ID-reference checks. Its form limits match backend limits. It has no external libraries, dashboard, animations, or CSV export.

## Database coverage

The automated tests use **H2 in MySQL compatibility mode**, not a running MySQL server. The optional MySQL driver, profile, and schema are supplied, but an end-to-end MySQL test has not been performed. MySQL compatibility mode is not a substitute for that test.

For MySQL, follow README.md with a fresh empty database. The schema does not migrate pre-existing tables.

## Limits

This remains a local, unauthenticated academic demonstration. The interface marks the complete stored student list. The calculation counts recorded PRESENT/ABSENT rows, not unmarked classes. There is at most one class per subject per calendar date.

## Browser smoke test

The simplified interface was served by the actual Spring Boot application on an isolated local test port using a temporary in-memory database. In the browser, a fictional student and subject were created, class attendance was saved, and the report showed 1 recorded class, 1 present, 0 absent, 100.00%, and Meets 75%.

The temporary browser-test tabs and server were closed after the check. Test data was not inserted into the default saved database or any existing MySQL database. A screenshot of the simple report screen was saved separately from the source bundle.

Seven additional lightweight frontend regression checks passed: Students as the default page, no-data display, server-provided low-attendance membership even when the displayed percentage rounds to 75, below-75 filtering, inverted-date rejection, empty query omission, and blank default attendance choices.

## Final release build

`mvnw.cmd -B verify` completed successfully on 14 November 2026 at 23:30 local time: **BUILD SUCCESS**, exit code 0, **12 tests, 0 failures, 0 errors, 0 skipped**. The executable archive is `target/student-attendance-1.0.0.jar` and includes its runtime dependencies.

An earlier restricted-sandbox rerun stalled during Java test-agent initialization. The final successful verification ran outside that restricted process. This was a tooling issue, not a failed attendance assertion. The MySQL limitation above still applies.

The packaged JAR also started successfully on its own with a temporary in-memory H2 database and an automatically assigned local port. Its test process was stopped after the startup check.
