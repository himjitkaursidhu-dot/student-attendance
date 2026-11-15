package com.attendance.service;
import com.attendance.model.*;
import java.time.LocalDate;
import java.util.List;
public interface AttendanceService {
    int markAttendance(Attendance attendance);
    int markBatch(List<Attendance> attendance);
    void delete(int id);
    List<Attendance> getAttendanceByStudent(int studentId);
    double getAttendancePercentage(int studentId, int subjectId);
    List<AttendanceView> findRecords(Integer studentId,Integer subjectId,LocalDate from,LocalDate to);
    List<AttendanceReport> reports(Integer studentId,Integer subjectId,LocalDate from,LocalDate to,boolean below75);
}
