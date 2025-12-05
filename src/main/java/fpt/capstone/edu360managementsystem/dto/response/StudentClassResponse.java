package fpt.capstone.edu360managementsystem.dto.response;

import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentClassResponse {
    private Long classId;
    private String className;
    private String subjectName;
    private String teacherName;
    private String teacherAvatarUrl;
    private String roomName;
    private String semesterName;

    private LocalDate startDate;
    private LocalDate endDate;
    private ClassStatus status;

    // Course của lớp (mỗi lớp có 1 course riêng)
    private Long courseId;
    private String courseTitle;
}
