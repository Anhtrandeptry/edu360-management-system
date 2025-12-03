package fpt.capstone.edu360managementsystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StudentScheduleResponse {
    private Long sessionId;
    private Long classId;
    private String className;
    private String subjectName;
    private String teacherName;
    private String roomName;
    private LocalDate date;
    private String timeStart;
    private String timeEnd;
    private Integer dayOfWeek; // 1=Mon, 7=Sun
    private String attendanceStatus; // PRESENT, ABSENT, LATE, UNMARKED
}