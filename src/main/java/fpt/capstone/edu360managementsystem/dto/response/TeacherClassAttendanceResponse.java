package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassAttendanceResponse {
    private Long classId;
    private String className;
    private String subjectName;
    private String semesterName;
    private String classStatus;
    

    private Integer totalSlots;
    private Integer completedSlots;
    private Integer pendingSlots;
    private Double completionRate;
    

    private List<SessionAttendanceDetail> sessions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionAttendanceDetail {
        private Long sessionId;
        private LocalDate date;
        private Integer dayOfWeek;
        private String timeSlot;
        private String roomName;
        private String sessionStatus;
        private Boolean isAttendanceSubmitted;
        private Integer totalStudents;
        private Integer presentCount;
        private Integer absentCount;
        private Integer lateCount;
        private String lessonContent;
    }
}
