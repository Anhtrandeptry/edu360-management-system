package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentScheduleItemResponse {

    private Long sessionId;
    private Long classId;
    private String className;
    private String subjectName;
    private String teacherName;
    private String roomName;

    private LocalDate date;
    private String timeStart;   // "HH:mm:ss"
    private String timeEnd;     // "HH:mm:ss"
}
