package fpt.capstone.edu360managementsystem.mapper;

import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ClassMapper {

    ClassMapper INSTANCE = Mappers.getMapper(ClassMapper.class);

    default ClassResponse toResponse(Clazz entity, List<ClassSchedule> schedules, int sessionsGenerated) {
        ClassResponse resp = ClassResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .semesterId(entity.getSemester().getId())
                .subjectId(entity.getSubject().getId())
                .teacherId(entity.getTeacher().getId())
                .roomId(entity.getRoom().getId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .maxStudents(entity.getMaxStudents())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .sessionsGenerated(sessionsGenerated)
                .subjectName(entity.getSubject().getName())
                .teacherFullName(entity.getTeacher().getUser().getFullName())
                .roomName(entity.getRoom().getName())
                .online(entity.getMeetingLink() != null && !entity.getMeetingLink().isBlank())
                .build();

        if (schedules != null) {
            resp.setSchedule(
                    schedules.stream()
                            .map(s -> new ClassResponse.ScheduleItemView(
                            s.getDayOfWeek(),
                            s.getTimeSlot().getId(),
                            s.getTimeSlot().getStartTime().toString(),
                            s.getTimeSlot().getEndTime().toString()
                    ))
                            .toList()
            );
        }
        return resp;
    }
}
