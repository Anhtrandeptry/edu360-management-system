package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.RoomRequest;
import fpt.capstone.edu360managementsystem.dto.response.RoomResponse;
import fpt.capstone.edu360managementsystem.entity.Room;
import fpt.capstone.edu360managementsystem.enums.RoomStatus;
import fpt.capstone.edu360managementsystem.mapper.RoomMapper;
import fpt.capstone.edu360managementsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        RoomResponse resp = roomMapper.toResponse(room);
        resp.setClassCount(clazzRepository.countActiveByRoom(room.getId()));
        return resp;
    }

    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByName(request.getName())) {
            throw new RuntimeException("Room name already exists!");
        }
        Room room = roomMapper.toEntity(request);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        roomMapper.updateEntityFromDto(request, room);
        if (roomRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new RuntimeException("Room name already exists!");
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
