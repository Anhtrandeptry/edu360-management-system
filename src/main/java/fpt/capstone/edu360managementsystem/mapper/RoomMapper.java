package fpt.capstone.edu360managementsystem.mapper;

import fpt.capstone.edu360managementsystem.dto.request.RoomRequest;
import fpt.capstone.edu360managementsystem.dto.response.RoomResponse;
import fpt.capstone.edu360managementsystem.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RoomMapper {
//    RoomMapper INSTANCE = Mappers.getMapper(RoomMapper.class);

    Room toEntity(RoomRequest dto);
    RoomResponse toResponse(Room entity);
    void updateEntityFromDto(RoomRequest dto, @MappingTarget Room entity);
}
