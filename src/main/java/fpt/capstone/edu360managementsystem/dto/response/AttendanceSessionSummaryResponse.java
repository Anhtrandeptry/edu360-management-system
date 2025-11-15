package fpt.capstone.edu360managementsystem.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceSessionSummaryResponse {
    private Long sessionId;
    private Long classId;
    private String className;
    private String subjectName;
    private String roomName;
    private String timeStart;     // "HH:mm:ss"
    private String timeEnd;       // "HH:mm:ss"
    private boolean marked;       // đã có bản ghi != UNMARKED cho ít nhất 1 HS
}
