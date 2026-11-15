package com.attendance.model;

import jakarta.validation.constraints.*;

public class Subject {

    // Private variables - Encapsulation
    private int subjectId;
    @NotBlank @Size(max = 120)
    private String subjectName;
    @NotBlank @Size(max = 30)
    private String subjectCode;

    // Default constructor
    public Subject() {
    }

    // Parameterized constructor
    public Subject(int subjectId, String subjectName,
                   String subjectCode) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
    }

    // Getters and Setters
    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }
}
