
package com.attendance.service;

import com.attendance.model.Student;
import java.util.List;
import java.util.Optional;

public interface StudentService {

    int addStudent(Student student);

    int updateStudent(Student student);

    int deleteStudent(int studentId);

    Optional<Student> findById(int studentId);

    List<Student> findAll();

    List<Student> searchByName(String name);
}
