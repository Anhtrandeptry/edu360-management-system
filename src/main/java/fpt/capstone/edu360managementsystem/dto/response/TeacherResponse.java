package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Teacher entity. Returns teacher information with user
 * details for frontend.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeacherResponse {

    private Long id;              // teacher.id
    private Long userId;          // user.id (for compatibility with existing FE code)
    private String username;      // user.username
    private String fullName;      // user.fullName
    private String email;         // user.email
    private String phoneNumber;   // user.phoneNumber
    private Long subjectId;       // subject.id
    private String subjectName;   // subject.name
    private String specialization; // teacher.specialization
    private String degree;        // teacher.degree
    private Boolean active;       // user.active
    private long classCount;      // số lớp chưa COMPLETE mà giáo viên đang dạy

}
