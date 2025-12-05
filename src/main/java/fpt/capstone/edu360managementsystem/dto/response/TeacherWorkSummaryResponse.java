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
    
    // Thống kê tháng hiện tại
    private Integer totalAssignedClasses;      // Số lớp được phân công
    private Integer totalScheduledSlots;        // Tổng số slot theo lịch trong tháng
    private Integer totalCompletedSlots;        // Số slot đã điểm danh (completed)
    private Integer totalPendingSlots;          // Số slot chưa điểm danh
    private Double attendanceRate;              // Tỷ lệ hoàn thành (%)
    
    // Chi tiết theo lớp
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
        private String status; // ACTIVE, COMPLETED, etc.
    }
}
