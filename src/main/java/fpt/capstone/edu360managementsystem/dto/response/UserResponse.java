package fpt.capstone.edu360managementsystem.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private List<String> roles;
    private Boolean active;
    // classCount removed from bulk user response (use /api/teachers/by-user/{userId} for realtime)

}
