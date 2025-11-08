package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.CreateClassRequest;
import fpt.capstone.edu360managementsystem.dto.request.ScheduleItemRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.enums.SessionStatus;
import fpt.capstone.edu360managementsystem.mapper.ClassMapper;
import fpt.capstone.edu360managementsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassService {

    @Autowired private SemesterRepository semesterRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ClazzRepository clazzRepository;
    @Autowired private ClassScheduleRepository classScheduleRepository;
    @Autowired private ClassSessionRepository classSessionRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private ClassMapper classMapper;

    @Transactional
    public ClassResponse createClass(CreateClassRequest req) {
        // 1) Load & validate
        Semester semester = semesterRepository.findById(req.getSemesterId())
                .orElseThrow(() -> new RuntimeException("Semester not found"));
        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        Teacher teacher = teacherRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (clazzRepository.existsByNameAndSubject_IdAndSemester_Id(req.getName(), subject.getId(), semester.getId())) {
            throw new RuntimeException("Duplicated class: same name + subject + semester");
        }

        // Teacher phải dạy đúng subject
        if (teacher.getSubject() == null || !teacher.getSubject().getId().equals(subject.getId())) {
            throw new RuntimeException("Teacher does not teach the selected subject");
        }

        if (req.getSchedule() == null || req.getSchedule().isEmpty()) {
            throw new RuntimeException("Schedule cannot be empty");
        }

        // Tập giá trị dayOfWeek & timeSlotIds để check xung đột
        Set<Integer> dows = req.getSchedule().stream()
                .map(ScheduleItemRequest::getDayOfWeek).collect(Collectors.toSet());
        Set<Long> slotIds = req.getSchedule().stream()
                .map(ScheduleItemRequest::getTimeSlotId).collect(Collectors.toSet());

        // Check xung đột giáo viên/phòng
        var teacherConflicts = clazzRepository.findTeacherConflicts(teacher.getId(), semester.getId(), dows, slotIds);
        if (!teacherConflicts.isEmpty())
            throw new RuntimeException("Teacher has conflicting class schedules in this semester");

        var roomConflicts = clazzRepository.findRoomConflicts(room.getId(), semester.getId(), dows, slotIds);
        if (!roomConflicts.isEmpty())
            throw new RuntimeException("Room has conflicting class schedules in this semester");

        // Xác định maxStudents
        int maxStudents = Optional.ofNullable(req.getMaxStudents()).orElse(room.getCapacity());
        if (maxStudents > room.getCapacity())
            throw new RuntimeException("maxStudents cannot exceed room capacity");

        // startDate: ngày hợp lệ đầu tiên theo lịch
        LocalDate classStart = firstClassDate(semester.getStartDate(), semester.getEndDate(), req.getSchedule());

        // Tạo lớp
        Clazz clazz = Clazz.builder()
                .name(req.getName())
                .code(req.getCode())
                .semester(semester)
                .subject(subject)
                .teacher(teacher)
                .room(room)
                .startDate(classStart)
                .endDate(semester.getEndDate()) // tạm, sẽ cập nhật sau
                .maxStudents(maxStudents)
                .description(req.getDescription())
                .status(deriveClassStatus(semester))
                .build();
        clazzRepository.save(clazz);

        // Lưu lịch lặp (ClassSchedule)
        List<ClassSchedule> schedules = req.getSchedule().stream().map(si -> {
            TimeSlot slot = timeSlotRepository.findById(si.getTimeSlotId())
                    .orElseThrow(() -> new RuntimeException("Invalid time slot id: " + si.getTimeSlotId()));
            return ClassSchedule.builder()
                    .clazz(clazz)
                    .dayOfWeek(si.getDayOfWeek())
                    .timeSlot(slot)
                    .build();
        }).toList();
        classScheduleRepository.saveAll(schedules);

        // Sinh các buổi học (ClassSession)
        int total = req.getTotalSessions();
        List<ClassSession> sessions = generateSessions(clazz, room, semester, schedules, total);
        classSessionRepository.saveAll(sessions);

        // Cập nhật endDate = ngày buổi cuối
        LocalDate last = sessions.get(sessions.size()-1).getDate();
        clazz.setEndDate(last);
        clazzRepository.save(clazz);

        return classMapper.toResponse(clazz, schedules, sessions.size());
    }

    private LocalDate firstClassDate(LocalDate start, LocalDate end, List<ScheduleItemRequest> schedule) {
        Set<Integer> dows = schedule.stream().map(ScheduleItemRequest::getDayOfWeek).collect(Collectors.toSet());
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (dows.contains(d.getDayOfWeek().getValue())) return d;
            d = d.plusDays(1);
        }
        return start;
    }

    private List<ClassSession> generateSessions(Clazz clazz, Room room, Semester sem,
                                                List<ClassSchedule> schedules, int totalSessions) {
        Map<Integer, List<TimeSlot>> map = schedules.stream()
                .collect(Collectors.groupingBy(ClassSchedule::getDayOfWeek,
                        Collectors.mapping(ClassSchedule::getTimeSlot, Collectors.toList())));

        List<ClassSession> out = new ArrayList<>();
        LocalDate d = sem.getStartDate();
        while (!d.isAfter(sem.getEndDate()) && out.size() < totalSessions) {
            int dow = d.getDayOfWeek().getValue(); // 1..7
            if (map.containsKey(dow)) {
                for (TimeSlot slot : map.get(dow)) {
                    if (out.size() >= totalSessions) break;
                    out.add(ClassSession.builder()
                            .clazz(clazz)
                            .date(d)
                            .dayOfWeek(dow)
                            .timeSlot(slot)
                            .room(room)
                            .status(SessionStatus.PLANNED)
                            .build());
                }
            }
            d = d.plusDays(1);
        }
        if (out.size() < totalSessions) {
            throw new RuntimeException("Not enough days in semester to generate required sessions");
        }
        return out;
    }

    private ClassStatus deriveClassStatus(Semester sem) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(sem.getStartDate())) return ClassStatus.COMING_SOON;
        if (today.isAfter(sem.getEndDate()))  return ClassStatus.COMPLETE;
        return ClassStatus.STUDYING;
    }
}
