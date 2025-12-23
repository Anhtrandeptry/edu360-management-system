package fpt.capstone.edu360managementsystem.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.response.UserResponse;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.service.UserService;

/**
 * REST controller for user management. Provides endpoints for user CRUD
 * operations and status management.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Retrieves all users.
     *
     * @return list of all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Retrieves users with pagination and filtering.
     *
     * @param search optional search term
     * @param role role filter (ALL or specific role)
     * @param page page number
     * @param size page size
     * @param sortBy sort field
     * @param order sort order (asc/desc)
     * @return paginated list of users
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getUsersPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order
    ) {
        return ResponseEntity.ok(userService.getUsersWithPagination(search, role, page, size, sortBy, order));
    }

    /**
     * Updates the active status of a user.
     *
     * @param id the user ID
     * @param body request body containing active status
     * @return empty response on success
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean active = body.get("active");
        if (active == null) {
            return ResponseEntity.badRequest().build();
        }
        userService.updateUserStatus(id, active);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets the count of active classes (PUBLIC status) for a student by user
     * ID. Used by admin to check before deactivating a student.
     *
     * @param id the user ID
     * @return active class count, -1 if user is not a student
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/active-class-count")
    public ResponseEntity<Map<String, Long>> getActiveClassCount(@PathVariable Long id) {
        long count = userService.getActiveClassCountByUserId(id);
        logger.info("[ACTIVE CLASS COUNT] userId={} count={}", id, count);
        return ResponseEntity.ok(Map.of("activeClassCount", count));
    }

}
