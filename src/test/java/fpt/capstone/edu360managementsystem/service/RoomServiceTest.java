package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.RoomRequest;
import fpt.capstone.edu360managementsystem.dto.response.RoomResponse;
import fpt.capstone.edu360managementsystem.entity.Room;
import fpt.capstone.edu360managementsystem.enums.RoomStatus;
import fpt.capstone.edu360managementsystem.mapper.RoomMapper;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomServiceTest {
    @Mock private RoomRepository roomRepository;
    @Mock private RoomMapper roomMapper;
    @Mock private ClazzRepository clazzRepository;
    @InjectMocks private RoomService roomService;

    private Room room;
    private RoomRequest roomRequest;
    private RoomResponse roomResponse;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setId(1L);
        room.setName("Room 101");
        room.setCapacity(30);
        room.setStatus(RoomStatus.AVAILABLE);

        roomRequest = new RoomRequest();
        roomRequest.setName("Room 101");
        roomRequest.setCapacity(30);

        roomResponse = new RoomResponse();
        roomResponse.setId(1L);
        roomResponse.setName("Room 101");
        roomResponse.setCapacity(30);
    }

    // getAllRooms - 5 cases
    @Test void test01_getAllRooms_empty() {
        when(roomRepository.findAll()).thenReturn(List.of());
        assertThat(roomService.getAllRooms()).isEmpty();
    }

    @Test void test02_getAllRooms_hasRooms() {
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(clazzRepository.countActiveByRoom(1L)).thenReturn(5L);
        List<RoomResponse> result = roomService.getAllRooms();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassCount()).isEqualTo(5L);
    }

    @Test void test03_getAllRooms_classCount() {
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(clazzRepository.countActiveByRoom(1L)).thenReturn(3L);
        roomService.getAllRooms();
        verify(clazzRepository).countActiveByRoom(1L);
    }

    @Test void test04_getAllRooms_mapping() {
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(clazzRepository.countActiveByRoom(anyLong())).thenReturn(0L);
        roomService.getAllRooms();
        verify(roomMapper).toResponse(room);
    }

    @Test void test05_getAllRooms_multiple() {
        Room room2 = new Room();
        room2.setId(2L);
        RoomResponse resp2 = new RoomResponse();
        resp2.setId(2L);
        when(roomRepository.findAll()).thenReturn(List.of(room, room2));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(roomMapper.toResponse(room2)).thenReturn(resp2);
        when(clazzRepository.countActiveByRoom(anyLong())).thenReturn(0L);
        assertThat(roomService.getAllRooms()).hasSize(2);
    }

    // getRoomById - 5 cases
    @Test void test06_getRoomById_notFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.getRoomById(1L))
            .hasMessageContaining("Room not found");
    }

    @Test void test07_getRoomById_found() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(clazzRepository.countActiveByRoom(1L)).thenReturn(0L);
        RoomResponse result = roomService.getRoomById(1L);
        assertThat(result).isNotNull();
    }

    @Test void test08_getRoomById_classCount() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(clazzRepository.countActiveByRoom(1L)).thenReturn(7L);
        RoomResponse result = roomService.getRoomById(1L);
        assertThat(result.getClassCount()).isEqualTo(7L);
    }

    @Test void test09_getRoomById_mapping() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(clazzRepository.countActiveByRoom(anyLong())).thenReturn(0L);
        roomService.getRoomById(1L);
        verify(roomMapper).toResponse(room);
    }

    @Test void test10_getRoomById_allFields() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        when(clazzRepository.countActiveByRoom(anyLong())).thenReturn(0L);
        RoomResponse result = roomService.getRoomById(1L);
        assertThat(result.getName()).isEqualTo("Room 101");
    }

    // createRoom - 5 cases
    @Test void test11_createRoom_nameExists() {
        when(roomRepository.existsByNameIgnoreCase("Room 101")).thenReturn(true);
        assertThatThrownBy(() -> roomService.createRoom(roomRequest))
            .hasMessageContaining("đã tồn tại");
    }

    @Test void test12_createRoom_valid() {
        when(roomRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(roomMapper.toEntity(roomRequest)).thenReturn(room);
        when(roomRepository.save(room)).thenReturn(room);
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);
        RoomResponse result = roomService.createRoom(roomRequest);
        assertThat(result).isNotNull();
    }

    @Test void test13_createRoom_saved() {
        when(roomRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(roomMapper.toEntity(roomRequest)).thenReturn(room);
        when(roomRepository.save(any())).thenReturn(room);
        when(roomMapper.toResponse(any())).thenReturn(roomResponse);
        roomService.createRoom(roomRequest);
        verify(roomRepository).save(any());
    }

    @Test void test14_createRoom_mapping() {
        when(roomRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(roomMapper.toEntity(roomRequest)).thenReturn(room);
        when(roomRepository.save(any())).thenReturn(room);
        when(roomMapper.toResponse(any())).thenReturn(roomResponse);
        roomService.createRoom(roomRequest);
        verify(roomMapper).toEntity(roomRequest);
    }

    @Test void test15_createRoom_response() {
        when(roomRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(roomMapper.toEntity(roomRequest)).thenReturn(room);
        when(roomRepository.save(any())).thenReturn(room);
        when(roomMapper.toResponse(any())).thenReturn(roomResponse);
        RoomResponse result = roomService.createRoom(roomRequest);
        assertThat(result.getName()).isEqualTo("Room 101");
    }

    // updateRoom - 5 cases
    @Test void test16_updateRoom_notFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.updateRoom(1L, roomRequest))
            .hasMessageContaining("Không tìm thấy phòng học");
    }

    @Test void test17_updateRoom_nameConflict() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.existsByNameIgnoreCaseAndIdNot("Room 101", 1L)).thenReturn(true);
        assertThatThrownBy(() -> roomService.updateRoom(1L, roomRequest))
            .hasMessageContaining("đã tồn tại");
    }

    @Test void test18_updateRoom_valid() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        doNothing().when(roomMapper).updateEntityFromDto(roomRequest, room);
        when(roomRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(roomRepository.save(any())).thenReturn(room);
        when(roomMapper.toResponse(any())).thenReturn(roomResponse);
        RoomResponse result = roomService.updateRoom(1L, roomRequest);
        assertThat(result).isNotNull();
    }

    @Test void test19_updateRoom_saved() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        doNothing().when(roomMapper).updateEntityFromDto(any(), any());
        when(roomRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(roomRepository.save(any())).thenReturn(room);
        when(roomMapper.toResponse(any())).thenReturn(roomResponse);
        roomService.updateRoom(1L, roomRequest);
        verify(roomRepository).save(any());
    }

    @Test void test20_updateRoom_response() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        doNothing().when(roomMapper).updateEntityFromDto(any(), any());
        when(roomRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(roomRepository.save(any())).thenReturn(room);
        when(roomMapper.toResponse(any())).thenReturn(roomResponse);
        RoomResponse result = roomService.updateRoom(1L, roomRequest);
        assertThat(result).isNotNull();
    }

    // disableRoom - 3 cases
    @Test void test21_disableRoom_notFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.disableRoom(1L))
            .hasMessageContaining("Room not found");
    }

    @Test void test22_disableRoom_inUse() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(clazzRepository.countActiveByRoom(1L)).thenReturn(5L);
        assertThatThrownBy(() -> roomService.disableRoom(1L))
            .hasMessageContaining("used by active classes");
    }

    @Test void test23_disableRoom_notInUse() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(clazzRepository.countActiveByRoom(1L)).thenReturn(0L);
        when(roomRepository.save(any())).thenReturn(room);
        roomService.disableRoom(1L);
        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.UNAVAILABLE));
    }

    // enableRoom - 2 cases
    @Test void test24_enableRoom_notFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roomService.enableRoom(1L))
            .hasMessageContaining("Room not found");
    }

    @Test void test25_enableRoom_success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any())).thenReturn(room);
        roomService.enableRoom(1L);
        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.AVAILABLE));
    }
}
