package com.attendance.repository;

import com.attendance.model.Subject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class SubjectRepository {
    private final JdbcTemplate jdbc;
    public SubjectRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private final RowMapper<Subject> mapper = (rs, n) -> new Subject(rs.getInt("subject_id"), rs.getString("subject_name"), rs.getString("subject_code"));
    public List<Subject> findAll() { return jdbc.query("SELECT * FROM subjects ORDER BY subject_code", mapper); }
    public Optional<Subject> findById(int id) {
        return jdbc.query("SELECT * FROM subjects WHERE subject_id=?", mapper, id).stream().findFirst();
    }
    public Subject save(Subject s) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO subjects(subject_name,subject_code) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, s.getSubjectName()); ps.setString(2, s.getSubjectCode()); return ps;
        }, key);
        s.setSubjectId(java.util.Objects.requireNonNull(key.getKey()).intValue()); return s;
    }
    public int update(Subject s) {
        return jdbc.update("UPDATE subjects SET subject_name=?,subject_code=? WHERE subject_id=?", s.getSubjectName(),s.getSubjectCode(),s.getSubjectId());
    }
    public int delete(int id) { return jdbc.update("DELETE FROM subjects WHERE subject_id=?", id); }
}
