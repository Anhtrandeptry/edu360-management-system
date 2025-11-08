package fpt.capstone.edu360managementsystem.dto.response;

import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ClassResponse {
    private Long id;
    private String name;
    private String code;

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

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ScheduleItemView {
        private Integer dayOfWeek;
        private Long timeSlotId;
        private String startTime;  // "HH:mm:ss"
        private String endTime;    // "HH:mm:ss"
    }
}
