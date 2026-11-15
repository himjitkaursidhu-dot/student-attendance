package com.attendance.model;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class Attendance {

    // Private variables - Encapsulation
    private Integer attendanceId;
    @Positive
    private int studentId;
    @Positive
    private int subjectId;
    @NotNull @PastOrPresent
    private LocalDate attendanceDate;
    @NotNull @Pattern(regexp = "PRESENT|ABSENT", message = "must be PRESENT or ABSENT")
    private String status;

    // Default constructor
    public Attendance() {
    }

    // Parameterized constructor
    public Attendance(Integer attendanceId, int studentId,
                      int subjectId, LocalDate attendanceDate,
                      String status) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.attendanceDate = attendanceDate;
        this.status = status;
    }

    // Getters and Setters
    public Integer getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

