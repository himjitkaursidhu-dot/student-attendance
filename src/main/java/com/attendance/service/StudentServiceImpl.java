package com.attendance.service;

import com.attendance.exception.ResourceNotFoundException;
import com.attendance.model.Student;
import com.attendance.repository.StudentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {
    private final StudentRepository repository;
    public StudentServiceImpl(StudentRepository repository) { this.repository = repository; }
    private void normalize(Student s) {
        s.setName(s.getName().trim()); s.setEmail(s.getEmail().trim().toLowerCase(Locale.ROOT));
        s.setDepartment(s.getDepartment().trim());
    }
    @Override public int addStudent(Student s) { normalize(s); return repository.addStudent(s); }
    @Override public int updateStudent(Student s) {
        normalize(s);
        if (repository.updateStudent(s) == 0) throw new ResourceNotFoundException("Student not found.");
        return 1;
    }
    @Override public int deleteStudent(int id) {
        if (repository.deleteStudent(id) == 0) throw new ResourceNotFoundException("Student not found.");
        return 1;
    }
    @Override public Optional<Student> findById(int id) { return repository.findById(id); }
    @Override public List<Student> findAll() { return repository.findAll(); }
    @Override public List<Student> searchByName(String term) { return repository.searchByName(term.trim()); }
}
