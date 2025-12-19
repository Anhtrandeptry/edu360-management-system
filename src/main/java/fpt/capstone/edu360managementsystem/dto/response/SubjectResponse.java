package fpt.capstone.edu360managementsystem.dto.response;

import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SubjectResponse {

    private Long id;
    private String name;
    private SubjectStatus status;
    private long classCount;
    private long courseCount;  // Số lượng khóa học của môn học
}
