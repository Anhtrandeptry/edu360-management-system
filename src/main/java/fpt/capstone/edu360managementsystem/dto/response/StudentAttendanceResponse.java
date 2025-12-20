package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentAttendanceResponse {

    private Long classId;
    private String className;
    private String subjectName;
    private String teacherName;

    private int totalSessions;
    private int attendedSessions;
    private int absentSessions;
    private int lateSessions;
    private int unmarkedSessions;
    private double attendanceRate;

    private List<SessionAttendanceItem> sessions;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionAttendanceItem {

        private Long sessionId;
        private LocalDate date;
        private String timeStart;
        private String timeEnd;
        private String roomName;
        private String status; // PRESENT, ABSENT, LATE, UNMARKED
        private String note;
    }
}
