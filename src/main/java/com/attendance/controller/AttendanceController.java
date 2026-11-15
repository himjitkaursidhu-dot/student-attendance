package com.attendance.controller;

import com.attendance.model.*;
import com.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService service;
    public AttendanceController(AttendanceService service) { this.service=service; }
    @PostMapping public Map<String,String> mark(@Valid @RequestBody Attendance a) {
        service.markAttendance(a); return Map.of("message","Attendance saved. Existing entries for the same student, subject and date are updated.");
    }
    @PostMapping("/batch") public Map<String,String> batch(@RequestBody List<Attendance> entries) {
        int count=service.markBatch(entries); return Map.of("message",count+" attendance entries saved.");
    }
    @DeleteMapping("/{id}") public Map<String,String> delete(@PathVariable int id) {
        service.delete(id); return Map.of("message","Attendance entry deleted.");
    }
    @GetMapping("/student/{studentId}") public List<Attendance> byStudent(@PathVariable int studentId) {
        return service.getAttendanceByStudent(studentId);
    }
    @GetMapping("/percentage/{studentId}/{subjectId}") public double percentage(@PathVariable int studentId,@PathVariable int subjectId) {
        return service.getAttendancePercentage(studentId,subjectId);
    }
    @GetMapping public List<AttendanceView> records(
            @RequestParam(required=false) Integer studentId,@RequestParam(required=false) Integer subjectId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.findRecords(studentId,subjectId,from,to);
    }
    @GetMapping("/reports") public List<AttendanceReport> reports(
            @RequestParam(required=false) Integer studentId,@RequestParam(required=false) Integer subjectId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.reports(studentId,subjectId,from,to,false);
    }
    @GetMapping("/defaulters") public List<AttendanceReport> below75(
            @RequestParam(required=false) Integer studentId,@RequestParam(required=false) Integer subjectId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.reports(studentId,subjectId,from,to,true);
    }
}
