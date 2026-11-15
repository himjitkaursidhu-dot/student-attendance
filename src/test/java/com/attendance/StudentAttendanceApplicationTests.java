package com.attendance;

import com.attendance.model.*;
import com.attendance.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:attendance_tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password="
})
@AutoConfigureMockMvc
class StudentAttendanceApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired StudentService students;
    @Autowired SubjectService subjects;
    @Autowired AttendanceService attendance;
    Student first;
    Subject oop;
    final LocalDate day=LocalDate.of(2024,1,1);

    @BeforeEach void setUp() {
        jdbc.update("DELETE FROM attendance"); jdbc.update("DELETE FROM students"); jdbc.update("DELETE FROM subjects");
        first=new Student(0,"Aman","aman@example.test","CSE",3); students.addStudent(first);
        oop=subjects.add(new Subject(0,"Object-Oriented Programming","OOP101"));
    }
    Attendance entry(int offset,String status) {
        return new Attendance(null,first.getStudentId(),oop.getSubjectId(),day.plusDays(offset),status);
    }
    @Test void homePageIsServed() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }
    @Test void studentCrudSearchAndNormalization() throws Exception {
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"  Simran  ","email":"SIMRAN@example.test","department":" IT ","semester":2}
            """)).andExpect(status().isCreated()).andExpect(jsonPath("$.studentId").isNumber())
            .andExpect(jsonPath("$.name").value("Simran")).andExpect(jsonPath("$.email").value("simran@example.test"));
        mvc.perform(get("/api/students/search").param("name","SIMRAN")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/students/search").param("name","aman@example.test")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/students/search").param("name",String.valueOf(first.getStudentId()))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/students/search").param("name","' OR 1=1 --")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/students/"+first.getStudentId()).contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Aman Updated","email":"aman@example.test","department":"CSE","semester":4}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.semester").value(4));
        mvc.perform(delete("/api/students/"+first.getStudentId())).andExpect(status().isOk());
        mvc.perform(get("/api/students/"+first.getStudentId())).andExpect(status().isNotFound());
    }
    @Test void invalidStudentAndDuplicateEmailAreRejected() throws Exception {
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":" ","email":"not-email","department":"","semester":0}
            """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Another Aman","email":"AMAN@example.test","department":"CSE","semester":3}
            """)).andExpect(status().isConflict());
        mvc.perform(put("/api/students/999999").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Missing","email":"missing@example.test","department":"CSE","semester":3}
            """)).andExpect(status().isNotFound());
    }
    @Test void subjectCrudAndDuplicateCode() throws Exception {
        mvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content("""
            {"subjectName":"Database Systems","subjectCode":" db101 "}
            """)).andExpect(status().isCreated()).andExpect(jsonPath("$.subjectCode").value("DB101"));
        mvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content("""
            {"subjectName":"Duplicate","subjectCode":"db101"}
            """)).andExpect(status().isConflict());
        mvc.perform(put("/api/subjects/"+oop.getSubjectId()).contentType(MediaType.APPLICATION_JSON).content("""
            {"subjectName":"Java OOP","subjectCode":"oop101"}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.subjectName").value("Java OOP"));
        mvc.perform(delete("/api/subjects/"+oop.getSubjectId())).andExpect(status().isOk());
        mvc.perform(get("/api/subjects/"+oop.getSubjectId())).andExpect(status().isNotFound());
    }
    @Test void remarkingSameDateCorrectsWithoutAddingAClass() {
        attendance.markAttendance(entry(0,"ABSENT")); attendance.markAttendance(entry(0,"PRESENT"));
        assertThat(attendance.getAttendanceByStudent(first.getStudentId())).hasSize(1);
        assertThat(attendance.getAttendancePercentage(first.getStudentId(),oop.getSubjectId())).isEqualTo(100.0);
    }
    @Test void percentageAndStrictBelow75Boundary() {
        attendance.markBatch(List.of(entry(0,"PRESENT"),entry(1,"PRESENT"),entry(2,"PRESENT"),entry(3,"ABSENT")));
        assertThat(attendance.getAttendancePercentage(first.getStudentId(),oop.getSubjectId())).isEqualTo(75.0);
        assertThat(attendance.reports(null,null,null,null,true)).isEmpty();
        attendance.markAttendance(entry(4,"ABSENT"));
        var report=attendance.reports(null,null,null,null,true);
        assertThat(report).hasSize(1); assertThat(report.get(0).percentage()).isEqualTo(60.0);
        assertThat(report.get(0).presentClasses()).isEqualTo(3); assertThat(report.get(0).absentClasses()).isEqualTo(2);
    }
    @Test void noRecordsAreNotDefaulters() {
        var report=attendance.reports(null,null,null,null,false);
        assertThat(report).hasSize(1); assertThat(report.get(0).totalClasses()).isZero();
        assertThat(attendance.getAttendancePercentage(first.getStudentId(),oop.getSubjectId())).isZero();
        assertThat(attendance.reports(null,null,null,null,true)).isEmpty();
    }
    @Test void reportsKeepSubjectsSeparateAndFilterDatesInclusively() throws Exception {
        Subject db=subjects.add(new Subject(0,"Database","DB101"));
        attendance.markBatch(List.of(entry(0,"PRESENT"),entry(1,"ABSENT"),entry(2,"PRESENT")));
        attendance.markAttendance(new Attendance(null,first.getStudentId(),db.getSubjectId(),day,"ABSENT"));
        mvc.perform(get("/api/attendance").param("subjectId",String.valueOf(oop.getSubjectId())).param("from","2024-01-02").param("to","2024-01-03"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].studentName").value("Aman"));
        mvc.perform(get("/api/attendance/reports").param("subjectId",String.valueOf(oop.getSubjectId())).param("from","2024-01-02").param("to","2024-01-03"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].totalClasses").value(2)).andExpect(jsonPath("$[0].percentage").value(50.0));
        mvc.perform(get("/api/attendance/defaulters").param("subjectId",String.valueOf(db.getSubjectId())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].percentage").value(0.0));
        mvc.perform(get("/api/attendance/reports").param("from","2025-01-01").param("to","2024-01-01")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/attendance").param("from","not-a-date")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/attendance").param("studentId","999999")).andExpect(status().isNotFound());
    }
    @Test void invalidBatchDoesNotPartiallySave() {
        Attendance missing=new Attendance(null,999999,oop.getSubjectId(),day,"PRESENT");
        assertThatThrownBy(() -> attendance.markBatch(List.of(entry(0,"PRESENT"),missing))).isInstanceOf(RuntimeException.class);
        assertThat(attendance.getAttendanceByStudent(first.getStudentId())).isEmpty();
        assertThatThrownBy(() -> attendance.markBatch(List.of(entry(0,"PRESENT"),entry(0,"ABSENT")))).isInstanceOf(IllegalArgumentException.class);
        assertThat(attendance.getAttendanceByStudent(first.getStudentId())).isEmpty();
    }
    @Test void batchEndpointAndAttendanceValidation() throws Exception {
        String json="[{\"studentId\":"+first.getStudentId()+",\"subjectId\":"+oop.getSubjectId()+",\"attendanceDate\":\"2024-01-01\",\"status\":\"PRESENT\"}]";
        mvc.perform(post("/api/attendance/batch").contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
        mvc.perform(post("/api/attendance").contentType(MediaType.APPLICATION_JSON).content(json.substring(1,json.length()-1).replace("PRESENT","LATE"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/attendance").contentType(MediaType.APPLICATION_JSON).content(json.substring(1,json.length()-1).replace("2024-01-01",LocalDate.now().plusDays(1).toString()))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/attendance/batch").contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/attendance/batch").contentType(MediaType.APPLICATION_JSON).content("[null]")).andExpect(status().isBadRequest());
    }
    @Test void removingStudentOrSubjectCascadesAndMissingDeletesReturn404() throws Exception {
        attendance.markAttendance(entry(0,"PRESENT")); subjects.delete(oop.getSubjectId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM attendance",Integer.class)).isZero();
        oop=subjects.add(new Subject(0,"OOP","OOP101")); attendance.markAttendance(entry(0,"PRESENT"));
        students.deleteStudent(first.getStudentId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM attendance",Integer.class)).isZero();
        mvc.perform(delete("/api/students/999999")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/attendance/999999")).andExpect(status().isNotFound());
    }
    @Test void deletingAnAttendanceEntryRecalculatesReport() throws Exception {
        attendance.markBatch(List.of(entry(0,"PRESENT"),entry(1,"ABSENT")));
        var rows=attendance.getAttendanceByStudent(first.getStudentId());
        int absentId=rows.stream().filter(a->a.getStatus().equals("ABSENT")).findFirst().orElseThrow().getAttendanceId();
        mvc.perform(delete("/api/attendance/"+absentId)).andExpect(status().isOk());
        assertThat(attendance.getAttendancePercentage(first.getStudentId(),oop.getSubjectId())).isEqualTo(100.0);
    }
}
