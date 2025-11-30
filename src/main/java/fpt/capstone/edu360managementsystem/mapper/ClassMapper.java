package fpt.capstone.edu360managementsystem.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.Clazz;

@Mapper(componentModel = "spring")
public interface ClassMapper {

    ClassMapper INSTANCE = Mappers.getMapper(ClassMapper.class);

    default ClassResponse toResponse(Clazz entity, List<ClassSchedule> schedules, int sessionsGenerated) {
        ClassResponse resp = ClassResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .semesterId(entity.getSemester() != null ? entity.getSemester().getId() : null)
                .subjectId(entity.getSubject().getId())
                .teacherId(entity.getTeacher().getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .maxStudents(entity.getMaxStudents())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .sessionsGenerated(sessionsGenerated)
                .subjectName(entity.getSubject().getName())
                .teacherFullName(entity.getTeacher().getUser().getFullName())
                .teacherAvatarUrl(entity.getTeacher().getAvatarUrl())
                .teacherUserId(entity.getTeacher().getUser().getId())
                .roomName(entity.getRoom() != null ? entity.getRoom().getName() : null)
                .online(entity.getMeetingLink() != null && !entity.getMeetingLink().isBlank())
                .meetingLink(entity.getMeetingLink())
                .courseId(entity.getCourse() != null ? entity.getCourse().getId() : null)
                .courseTitle(entity.getCourse() != null ? entity.getCourse().getTitle() : null)
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

