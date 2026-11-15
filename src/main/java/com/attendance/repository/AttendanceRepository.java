package com.attendance.repository;

import com.attendance.model.Attendance;
import com.attendance.model.AttendanceReport;
import com.attendance.model.AttendanceView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AttendanceRepository {
    private final JdbcTemplate jdbc;
    public AttendanceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private final RowMapper<Attendance> mapper = (rs, n) -> new Attendance(rs.getInt("attendance_id"),
        rs.getInt("student_id"), rs.getInt("subject_id"), rs.getDate("attendance_date").toLocalDate(), rs.getString("status"));
    // Update an existing date instead of inflating the denominator with a duplicate class.
    public int markAttendance(Attendance a) {
        int updated = jdbc.update("UPDATE attendance SET status=? WHERE student_id=? AND subject_id=? AND attendance_date=?",
            a.getStatus(), a.getStudentId(), a.getSubjectId(), Date.valueOf(a.getAttendanceDate()));
        if (updated > 0) return updated;
        return jdbc.update("INSERT INTO attendance(student_id,subject_id,attendance_date,status) VALUES (?,?,?,?)",
            a.getStudentId(), a.getSubjectId(), Date.valueOf(a.getAttendanceDate()), a.getStatus());
    }
    public int delete(int id) { return jdbc.update("DELETE FROM attendance WHERE attendance_id=?", id); }
    public List<Attendance> getAttendanceByStudent(int id) {
        return jdbc.query("SELECT * FROM attendance WHERE student_id=? ORDER BY attendance_date DESC,subject_id", mapper, id);
    }
    public double getAttendancePercentage(int studentId, int subjectId) {
        Double percentage = jdbc.queryForObject("SELECT COALESCE(100.0*SUM(CASE WHEN status='PRESENT' THEN 1 ELSE 0 END)/NULLIF(COUNT(*),0),0) FROM attendance WHERE student_id=? AND subject_id=?",
            Double.class, studentId, subjectId);
        return Math.round((percentage == null ? 0 : percentage) * 100.0) / 100.0;
    }
    public List<AttendanceView> findRecords(Integer studentId, Integer subjectId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT a.*,s.name AS student_name,sub.subject_name,sub.subject_code FROM attendance a JOIN students s ON s.student_id=a.student_id JOIN subjects sub ON sub.subject_id=a.subject_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (studentId != null) { sql.append(" AND a.student_id=?"); args.add(studentId); }
        if (subjectId != null) { sql.append(" AND a.subject_id=?"); args.add(subjectId); }
        if (from != null) { sql.append(" AND a.attendance_date>=?"); args.add(Date.valueOf(from)); }
        if (to != null) { sql.append(" AND a.attendance_date<=?"); args.add(Date.valueOf(to)); }
        sql.append(" ORDER BY a.attendance_date DESC,s.student_id,sub.subject_code");
        return jdbc.query(sql.toString(), (rs,n) -> new AttendanceView(rs.getInt("attendance_id"),rs.getInt("student_id"),
            rs.getString("student_name"),rs.getInt("subject_id"),rs.getString("subject_name"),rs.getString("subject_code"),
            rs.getDate("attendance_date").toLocalDate(),rs.getString("status")),args.toArray());
    }
    public List<AttendanceReport> reports(Integer studentId, Integer subjectId, LocalDate from, LocalDate to, boolean below75) {
        // Date predicates belong in the LEFT JOIN, so pairs without records still appear.
        StringBuilder sql = new StringBuilder("""
            SELECT s.student_id,s.name AS student_name,sub.subject_id,sub.subject_name,sub.subject_code,
              COUNT(a.attendance_id) AS total_classes,
              COALESCE(SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END),0) AS present_classes,
              COALESCE(100.0*SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END)/NULLIF(COUNT(a.attendance_id),0),0) AS percentage
            FROM students s CROSS JOIN subjects sub
            LEFT JOIN attendance a ON a.student_id=s.student_id AND a.subject_id=sub.subject_id
            """);
        List<Object> args = new ArrayList<>();
        if (from != null) { sql.append(" AND a.attendance_date>=?"); args.add(Date.valueOf(from)); }
        if (to != null) { sql.append(" AND a.attendance_date<=?"); args.add(Date.valueOf(to)); }
        sql.append(" WHERE 1=1");
        if (studentId != null) { sql.append(" AND s.student_id=?"); args.add(studentId); }
        if (subjectId != null) { sql.append(" AND sub.subject_id=?"); args.add(subjectId); }
        sql.append(" GROUP BY s.student_id,s.name,sub.subject_id,sub.subject_name,sub.subject_code");
        // Compare exact counts BEFORE rounding: exactly 75% must not be included.
        if (below75) sql.append(" HAVING COUNT(a.attendance_id)>0 AND 100*SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END)<75*COUNT(a.attendance_id)");
        sql.append(" ORDER BY s.student_id,sub.subject_code");
        return jdbc.query(sql.toString(), (rs,n) -> {
            int total=rs.getInt("total_classes"), present=rs.getInt("present_classes");
            return new AttendanceReport(rs.getInt("student_id"),rs.getString("student_name"),rs.getInt("subject_id"),
                rs.getString("subject_name"),rs.getString("subject_code"),total,present,total-present,
                Math.round(rs.getDouble("percentage")*100.0)/100.0);
        }, args.toArray());
    }
}
