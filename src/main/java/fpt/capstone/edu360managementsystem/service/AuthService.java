package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> registerStudentWithParent(RegisterStudentWithParentRequest request);
}
