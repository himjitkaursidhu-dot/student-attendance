package com.attendance.service;

import com.attendance.exception.ResourceNotFoundException;
import com.attendance.model.*;
import com.attendance.repository.*;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceRepository repository;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final Validator validator;
    public AttendanceServiceImpl(AttendanceRepository repository, StudentRepository students, SubjectRepository subjects, Validator validator) {
        this.repository=repository; this.students=students; this.subjects=subjects; this.validator=validator;
    }
    private void checkReferences(int studentId, int subjectId) {
        students.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        subjects.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject not found."));
    }
    private void validate(Attendance a) {
        if(a == null) throw new IllegalArgumentException("Attendance entry must not be null.");
        var errors=validator.validate(a);
        if(!errors.isEmpty()) throw new IllegalArgumentException(errors.stream()
            .map(e -> e.getPropertyPath()+": "+e.getMessage()).sorted().reduce((a1,a2)->a1+"; "+a2).orElse("Invalid attendance."));
        checkReferences(a.getStudentId(),a.getSubjectId());
    }
    @Override @Transactional public int markAttendance(Attendance a) { validate(a); return repository.markAttendance(a); }
    @Override @Transactional public int markBatch(List<Attendance> entries) {
        if(entries == null || entries.isEmpty()) throw new IllegalArgumentException("Choose at least one student.");
        if(entries.size()>1000) throw new IllegalArgumentException("Save no more than 1000 entries at once.");
        Set<String> keys=new HashSet<>();
        for(Attendance a:entries) {
            validate(a);
            if(!keys.add(a.getStudentId()+":"+a.getSubjectId()+":"+a.getAttendanceDate()))
                throw new IllegalArgumentException("The batch contains the same student/subject/date twice.");
        }
        for(Attendance a:entries) repository.markAttendance(a);
        return entries.size();
    }
    @Override public void delete(int id) {
        if(repository.delete(id)==0) throw new ResourceNotFoundException("Attendance record not found.");
    }
    @Override public List<Attendance> getAttendanceByStudent(int studentId) {
        students.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        return repository.getAttendanceByStudent(studentId);
    }
    @Override public double getAttendancePercentage(int studentId,int subjectId) {
        checkReferences(studentId,subjectId); return repository.getAttendancePercentage(studentId,subjectId);
    }
    private void validateFilters(Integer studentId,Integer subjectId,LocalDate from,LocalDate to) {
        if(from!=null && to!=null && from.isAfter(to)) throw new IllegalArgumentException("Start date must be on or before end date.");
        if(studentId!=null) students.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        if(subjectId!=null) subjects.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject not found."));
    }
    @Override public List<AttendanceView> findRecords(Integer studentId,Integer subjectId,LocalDate from,LocalDate to) {
        validateFilters(studentId,subjectId,from,to); return repository.findRecords(studentId,subjectId,from,to);
    }
    @Override public List<AttendanceReport> reports(Integer studentId,Integer subjectId,LocalDate from,LocalDate to,boolean below75) {
        validateFilters(studentId,subjectId,from,to); return repository.reports(studentId,subjectId,from,to,below75);
    }
}
