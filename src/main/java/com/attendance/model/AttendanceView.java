package com.attendance.model;
import java.time.LocalDate;
/** Read-only joined view for displaying attendance with names, not just IDs. */
public record AttendanceView(int attendanceId, int studentId, String studentName,
        int subjectId, String subjectName, String subjectCode,
        LocalDate attendanceDate, String status) { }
