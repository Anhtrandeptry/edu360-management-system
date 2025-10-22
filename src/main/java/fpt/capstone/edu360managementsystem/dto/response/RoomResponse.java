package fpt.capstone.edu360managementsystem.dto.response;

import fpt.capstone.edu360managementsystem.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private RoomStatus status;
}
