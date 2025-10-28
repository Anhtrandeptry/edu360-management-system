package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.RoomResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserResponse;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
