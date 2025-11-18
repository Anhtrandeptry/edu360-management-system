package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrolledStudentResponse {
    private Long studentId;
    private String fullName;
    private String email;
    private String phoneNumber;
}
