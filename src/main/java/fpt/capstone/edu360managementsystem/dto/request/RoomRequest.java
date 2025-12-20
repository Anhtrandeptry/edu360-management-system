package fpt.capstone.edu360managementsystem.dto.request;

import fpt.capstone.edu360managementsystem.enums.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "Room name is required")
    private String name;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    @Max(value = 60, message = "Sức chứa tối đa là 60 người")
    private Integer capacity;

    private RoomStatus status = RoomStatus.AVAILABLE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
