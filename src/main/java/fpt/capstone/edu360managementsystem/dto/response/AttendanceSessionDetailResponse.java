package fpt.capstone.edu360managementsystem.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceSessionDetailResponse {
    private Long sessionId;
    private Long classId;
    private String className;
    private String subjectName;
    private String roomName;
    private String timeStart;
    private String timeEnd;

    private List<AttendanceStudentItem> students;
}
