package fpt.capstone.edu360managementsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCertificateRequest {
    private Long id;  // null for create, non-null for update
    private String title;
    private String organization;
    private Integer year;
    private String description;
}
