package fpt.capstone.edu360managementsystem.mapper;

import fpt.capstone.edu360managementsystem.dto.request.SubjectRequest;
import fpt.capstone.edu360managementsystem.dto.response.SubjectResponse;
import fpt.capstone.edu360managementsystem.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SubjectMapper {
    SubjectMapper INSTANCE = Mappers.getMapper(SubjectMapper.class);

    Subject toEntity(SubjectRequest dto);
    SubjectResponse toResponse(Subject entity);
    void updateEntityFromDto(SubjectRequest dto, @MappingTarget Subject entity);
}
