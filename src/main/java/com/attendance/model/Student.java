package com.attendance.model;

import jakarta.validation.constraints.*;

public class Student {

    // Private variables - Encapsulation
    private int studentId;
    @NotBlank @Size(max = 150)
    private String name;
    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Size(max = 100)
    private String department;
    @Min(1) @Max(12)
    private int semester;

    // Default constructor
    public Student() {
    }

    // Parameterized constructor
    public Student(int studentId, String name, String email,
                   String department, int semester) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.semester = semester;
    }

    // Getters and Setters
    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }
}
