package fpt.capstone.edu360managementsystem.dto.response;

import java.time.LocalDate;
import java.util.List;

import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassResponse {

    private Long id;
    private String name;

    private Long semesterId;
    private Long subjectId;
    private Long teacherId;
    private Long roomId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxStudents;

    private String description;
    private ClassStatus status;

    private List<ScheduleItemView> schedule;
    private Integer sessionsGenerated;

    // Derived display fields for class list cards
    private String subjectName;
    private String teacherFullName;
    private String teacherAvatarUrl; // Avatar URL of the teacher
    private Long teacherUserId; // User ID of the teacher (for filtering in schedule)
    private String roomName;
    private Boolean online; // true nếu meetingLink != null
    private String meetingLink; // Link meeting cho lớp online
    private Integer currentStudents; // sẽ mở rộng sau (tạm null)

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleItemView {

        private Integer dayOfWeek;
        private Long timeSlotId;
        private String startTime;  // "HH:mm:ss"
        private String endTime;    // "HH:mm:ss"
    }
}
