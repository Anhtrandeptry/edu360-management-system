package fpt.capstone.edu360managementsystem.dto.response;

import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceStudentItem {
    private Long studentId;
    private String studentName;
    private AttendanceStatus status;
}
