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
    private Boolean hidden;

    private List<ScheduleItemView> schedule;
    private Integer sessionsGenerated;

    // Thêm trường phục vụ hiển thị học phí
    private Long pricePerSession;   // Giá mỗi buổi (VND)
    private Integer totalSessions;  // Tổng số buổi của lớp
    private Integer completedSessions; // Số buổi đã hoàn thành (status = DONE)

    // Tổng học phí = pricePerSession * totalSessions
    public Long getPrice() {
        if (pricePerSession == null || totalSessions == null) {
            return 0L;
        }
        return pricePerSession * totalSessions;
    }

    private Long courseId;
    private String courseTitle;

    private String subjectName;
    private String teacherFullName;
    private String teacherAvatarUrl;
    private Long teacherUserId;
    private String roomName;
    private Boolean online;
    private String meetingLink;
    private Integer currentStudents;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleItemView {

        private Integer dayOfWeek;
        private Long timeSlotId;
        private String startTime;
        private String endTime;
    }
}
