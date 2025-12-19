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
public class TeacherListForAttendanceResponse {

    private Long teacherId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private List<String> subjectNames;
    private String degree;
    private String specialization;

    // Quick stats
    private Integer assignedClasses;
    private Integer completedSlotsThisMonth;
    private Integer totalSlotsThisMonth;
    private Double attendanceRateThisMonth;
}
