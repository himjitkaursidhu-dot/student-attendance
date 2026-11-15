package com.attendance.model;
/** Immutable report DTO. A zero total means no attendance has been recorded. */
public record AttendanceReport(int studentId, String studentName, int subjectId,
        String subjectName, String subjectCode, int totalClasses,
        int presentClasses, int absentClasses, double percentage) { }
