package fpt.capstone.edu360managementsystem.dto.response;

import java.time.LocalDate;
import java.util.List;

import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public class detail response for guest/unauthenticated users.
 * Contains class info + base course info (from Admin).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassPublicDetailResponse {

    // Class info
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxStudents;
    private Integer currentStudents;
    private ClassStatus status;
    private Boolean online;
    private String meetingLink;

    // Subject info
    private Long subjectId;
    private String subjectName;

    // Room info
    private Long roomId;
    private String roomName;

    // Semester info
    private Long semesterId;
    private String semesterName;

    // Teacher info
    private Long teacherId;
    private String teacherFullName;
    private String teacherAvatarUrl;
    private String teacherBio;
    private String teacherDepartment;

    // Base Course info (from Admin)
    private Long courseId;
    private String courseTitle;
    private String courseDescription;
    private String courseThumbnail;
    private List<CourseLessonView> courseLessons;

    // Schedule
    private List<ScheduleItemView> schedule;
    private Integer sessionsGenerated;

    // Price
    private Long pricePerSession;
    private Long totalPrice;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleItemView {
        private Integer dayOfWeek;
        private Long timeSlotId;
        private String startTime;
        private String endTime;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CourseLessonView {
        private Long id;
        private String title;
        private Integer orderIndex;
        private String description;
    }
}
