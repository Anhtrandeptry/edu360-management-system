package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherWorkSummaryResponse {
    private Long teacherId;
    private String teacherName;
    private String email;
    private String phone;
    private List<String> subjectNames;
    

    private Integer totalAssignedClasses;
    private Integer totalScheduledSlots;
    private Integer totalCompletedSlots;
    private Integer totalPendingSlots;
    private Double attendanceRate;
    

    private List<ClassWorkDetail> classDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassWorkDetail {
        private Long classId;
        private String className;
        private String subjectName;
        private String semesterName;
        private Integer totalSlots;
        private Integer completedSlots;
        private Integer pendingSlots;
        private String status;
    }
}
