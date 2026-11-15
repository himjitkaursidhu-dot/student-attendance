package com.attendance.controller;

import com.attendance.exception.ResourceNotFoundException;
import com.attendance.model.Student;
import com.attendance.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Student add(@Valid @RequestBody Student student) { service.addStudent(student); return student; }
    @GetMapping public List<Student> all() { return service.findAll(); }
    @GetMapping("/{id}") public Student find(@PathVariable int id) {
        return service.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
    @GetMapping("/search") public List<Student> search(@RequestParam(defaultValue="") String name) {
        return service.searchByName(name);
    }
    @PutMapping("/{id}") public Student update(@PathVariable int id, @Valid @RequestBody Student student) {
        student.setStudentId(id); service.updateStudent(student); return student;
    }
    @DeleteMapping("/{id}") public Map<String, String> delete(@PathVariable int id) {
        service.deleteStudent(id); return Map.of("message", "Student and related attendance deleted.");
    }
}
