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
    
    // Thống kê
    private Integer totalSlots;
    private Integer completedSlots;
    private Integer pendingSlots;
    private Double completionRate;
    
    // Danh sách sessions
    private List<SessionAttendanceDetail> sessions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionAttendanceDetail {
        private Long sessionId;
        private LocalDate date;
        private Integer dayOfWeek;
        private String timeSlot;          // "07:00 - 09:00"
        private String roomName;
        private String sessionStatus;     // PLANNED, COMPLETED, CANCELLED
        private Boolean isAttendanceSubmitted;  // Đã điểm danh chưa
        private Integer totalStudents;
        private Integer presentCount;
        private Integer absentCount;
        private Integer lateCount;
        private String lessonContent;
    }
}
