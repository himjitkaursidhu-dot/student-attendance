package com.attendance.repository;

import com.attendance.model.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {
    private final JdbcTemplate jdbc;
    public StudentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private final RowMapper<Student> mapper = (rs, n) -> new Student(rs.getInt("student_id"),
        rs.getString("name"), rs.getString("email"), rs.getString("department"), rs.getInt("semester"));
    public int addStudent(Student s) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO students(name,email,department,semester) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, s.getName()); ps.setString(2, s.getEmail());
            ps.setString(3, s.getDepartment()); ps.setInt(4, s.getSemester()); return ps;
        }, key);
        s.setStudentId(java.util.Objects.requireNonNull(key.getKey()).intValue());
        return 1;
    }
    public int updateStudent(Student s) {
        return jdbc.update("UPDATE students SET name=?, email=?, department=?, semester=? WHERE student_id=?",
            s.getName(), s.getEmail(), s.getDepartment(), s.getSemester(), s.getStudentId());
    }
    public int deleteStudent(int id) { return jdbc.update("DELETE FROM students WHERE student_id=?", id); }
    public Optional<Student> findById(int id) {
        return jdbc.query("SELECT * FROM students WHERE student_id=?", mapper, id).stream().findFirst();
    }
    public List<Student> findAll() { return jdbc.query("SELECT * FROM students ORDER BY student_id", mapper); }
    public List<Student> searchByName(String term) {
        String pattern = "%" + term.toLowerCase(java.util.Locale.ROOT) + "%";
        int id = -1;
        try { id = Integer.parseInt(term); } catch (NumberFormatException ignored) { /* Not an ID search. */ }
        return jdbc.query("SELECT * FROM students WHERE LOWER(name) LIKE ? OR LOWER(email) LIKE ? OR student_id=? ORDER BY student_id",
            mapper, pattern, pattern, id);
    }
}
