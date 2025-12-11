package fpt.capstone.edu360managementsystem.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.request.RoomRequest;
import fpt.capstone.edu360managementsystem.dto.response.RoomResponse;
import fpt.capstone.edu360managementsystem.entity.Room;
import fpt.capstone.edu360managementsystem.enums.RoomStatus;
import fpt.capstone.edu360managementsystem.mapper.RoomMapper;
import fpt.capstone.edu360managementsystem.repository.RoomRepository;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMapper roomMapper;

    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(r -> {
                    RoomResponse resp = roomMapper.toResponse(r);
                    resp.setClassCount(clazzRepository.countActiveByRoom(r.getId()));
                    return resp;
                })
                .toList();
    }

    /**
     * Lấy danh sách rooms với phân trang và filter
     *
     * @param search từ khóa tìm kiếm (name)
     * @param status filter theo RoomStatus (AVAILABLE, UNAVAILABLE, ALL)
     * @param page số trang (bắt đầu từ 0)
     * @param size số phần tử mỗi trang
     * @param sortBy trường để sắp xếp
     * @param order thứ tự sắp xếp (asc, desc)
     * @return Page<RoomResponse>
     */
    public Page<RoomResponse> getRoomsWithPagination(
            String search,
            String status,
            int page,
            int size,
            String sortBy,
            String order
    ) {
        // Xử lý sort
        Sort sort = Sort.by(sortBy != null ? sortBy : "id");
        if ("desc".equalsIgnoreCase(order)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        // Xử lý status filter
        RoomStatus roomStatus = null;
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            try {
                roomStatus = RoomStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, keep null to get all
            }
        }

        // Query với pagination
        Page<Room> roomPage = roomRepository.findBySearchAndStatus(search, roomStatus, pageable);

        // Map to response with classCount
        return roomPage.map(r -> {
            RoomResponse resp = roomMapper.toResponse(r);
            resp.setClassCount(clazzRepository.countActiveByRoom(r.getId()));
            return resp;
        });
    }

    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        RoomResponse resp = roomMapper.toResponse(room);
        resp.setClassCount(clazzRepository.countActiveByRoom(room.getId()));
        return resp;
    }

    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByNameIgnoreCase(request.getName())) {
            throw new RuntimeException("Phòng học đã tồn tại");
        }
        Room room = roomMapper.toEntity(request);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        roomMapper.updateEntityFromDto(request, room);
        if (roomRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new RuntimeException("Phòng học đã tồn tại");
        }
        return roomMapper.toResponse(roomRepository.save(room));
    }

    public void disableRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        long used = clazzRepository.countActiveByRoom(room.getId());
        if (used > 0) {
            throw new RuntimeException("Room is used by active classes, cannot disable");
        }
        room.setStatus(RoomStatus.UNAVAILABLE);
        roomRepository.save(room);
    }

    public void enableRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        room.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(room);
    }

    @Autowired
    private fpt.capstone.edu360managementsystem.repository.ClazzRepository clazzRepository;
}
