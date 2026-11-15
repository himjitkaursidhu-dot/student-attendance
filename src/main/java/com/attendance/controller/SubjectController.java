package com.attendance.controller;
import com.attendance.model.Subject;
import com.attendance.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectService service;
    public SubjectController(SubjectService service) { this.service = service; }
    @GetMapping public List<Subject> all() { return service.findAll(); }
    @GetMapping("/{id}") public Subject find(@PathVariable int id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Subject add(@Valid @RequestBody Subject s) { return service.add(s); }
    @PutMapping("/{id}") public Subject update(@PathVariable int id, @Valid @RequestBody Subject s) { return service.update(id, s); }
    @DeleteMapping("/{id}") public Map<String,String> delete(@PathVariable int id) {
        service.delete(id); return Map.of("message", "Subject and related attendance deleted.");
    }
}
