package com.attendance.service;

import com.attendance.exception.ResourceNotFoundException;
import com.attendance.model.Subject;
import com.attendance.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository repository;
    public SubjectServiceImpl(SubjectRepository repository) { this.repository = repository; }
    private void normalize(Subject s) {
        s.setSubjectName(s.getSubjectName().trim()); s.setSubjectCode(s.getSubjectCode().trim().toUpperCase(Locale.ROOT));
    }
    @Override public List<Subject> findAll() { return repository.findAll(); }
    @Override public Subject findById(int id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Subject not found."));
    }
    @Override public Subject add(Subject s) { normalize(s); return repository.save(s); }
    @Override public Subject update(int id, Subject s) {
        normalize(s); s.setSubjectId(id);
        if (repository.update(s) == 0) throw new ResourceNotFoundException("Subject not found.");
        return s;
    }
    @Override public void delete(int id) {
        if (repository.delete(id) == 0) throw new ResourceNotFoundException("Subject not found.");
    }
}
