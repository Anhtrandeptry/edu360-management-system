package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.RoomRequest;
import fpt.capstone.edu360managementsystem.dto.response.BusySlotResponse;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.dto.response.RoomResponse;
import fpt.capstone.edu360managementsystem.repository.RoomRepository;
import fpt.capstone.edu360managementsystem.service.RoomService;
import fpt.capstone.edu360managementsystem.service.ScheduleService;
import jakarta.validation.Valid;

/**
 * REST controller for classroom/room management.
 * Provides endpoints for CRUD operations on physical classrooms.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleService scheduleService;

    /**
     * Retrieves all rooms.
     *
     * @return list of all rooms
     */
    @GetMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    /**
     * Retrieves paginated rooms with filters and sorting.
     *
     * @param search optional search term
     * @param status status filter
     * @param page   page number
     * @param size   page size
     * @param sortBy sort field
     * @param order  sort order
     * @return paginated room list
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RoomResponse>> getRoomsPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order
    ) {
        return ResponseEntity.ok(roomService.getRoomsWithPagination(search, status, page, size, sortBy, order));
    }

    /**
     * Creates a new room.
     *
     * @param request room creation data
     * @return created room or error if name exists
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoom(@Valid @RequestBody RoomRequest request) {
        if (roomRepository.existsByNameIgnoreCase(request.getName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Phòng học đã tồn tại"));
        }
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    /**
     * Retrieves room details by ID.
     *
     * @param id the room ID
     * @return room details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    /**
     * Updates an existing room.
     *
     * @param id      the room ID
     * @param request updated room data
     * @return updated room
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    /**
     * Disables a room.
     *
     * @param id the room ID
     * @return success message
     */
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> disableRoom(@PathVariable Long id) {
        roomService.disableRoom(id);
        return ResponseEntity.ok("Room disabled successfully");
    }

    /**
     * Enables a room.
     *
     * @param id the room ID
     * @return success message
     */
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> enableRoom(@PathVariable Long id) {
        roomService.enableRoom(id);
        return ResponseEntity.ok("Room enabled successfully");
    }

    /**
     * Retrieves busy time slots for a room.
     *
     * @param id   the room ID
     * @param from optional start date filter
     * @param to   optional end date filter
     * @return list of busy time slots
     */
    @GetMapping("/{id}/free-busy")
    public ResponseEntity<List<BusySlotResponse>> getRoomFreeBusy(
            @PathVariable Long id,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to
    ) {
        List<BusySlotResponse> busySlots = scheduleService.getRoomBusySlots(id, from, to);
        return ResponseEntity.ok(busySlots);
    }
}
