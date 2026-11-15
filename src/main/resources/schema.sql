-- Compatible with MySQL 8+ and H2 in MySQL mode. Never drops existing tables.
CREATE TABLE IF NOT EXISTS students (
  student_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  email VARCHAR(254) NOT NULL,
  department VARCHAR(100) NOT NULL,
  semester INT NOT NULL,
  CONSTRAINT uq_student_email UNIQUE (email),
  CONSTRAINT ck_student_semester CHECK (semester BETWEEN 1 AND 12)
);
CREATE TABLE IF NOT EXISTS subjects (
  subject_id INT AUTO_INCREMENT PRIMARY KEY,
  subject_name VARCHAR(120) NOT NULL,
  subject_code VARCHAR(30) NOT NULL,
  CONSTRAINT uq_subject_code UNIQUE (subject_code)
);
CREATE TABLE IF NOT EXISTS attendance (
  attendance_id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  subject_id INT NOT NULL,
  attendance_date DATE NOT NULL,
  status VARCHAR(7) NOT NULL,
  CONSTRAINT uq_attendance UNIQUE (student_id, subject_id, attendance_date),
  CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT', 'ABSENT')),
  CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
  CONSTRAINT fk_attendance_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE
);
