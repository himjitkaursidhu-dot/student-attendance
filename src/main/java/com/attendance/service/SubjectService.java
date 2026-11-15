package com.attendance.service;
import com.attendance.model.Subject;
import java.util.List;
public interface SubjectService {
    List<Subject> findAll();
    Subject findById(int id);
    Subject add(Subject subject);
    Subject update(int id, Subject subject);
    void delete(int id);
}
