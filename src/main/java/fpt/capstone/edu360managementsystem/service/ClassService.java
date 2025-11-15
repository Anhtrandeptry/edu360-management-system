package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.CreateClassRequest;
import fpt.capstone.edu360managementsystem.dto.request.ScheduleItemRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Room;
import fpt.capstone.edu360managementsystem.entity.Semester;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.TimeSlot;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.enums.SessionStatus;
import fpt.capstone.edu360managementsystem.mapper.ClassMapper;
import fpt.capstone.edu360managementsystem.repository.ClassScheduleRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.RoomRepository;
import fpt.capstone.edu360managementsystem.repository.SemesterRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.TimeSlotRepository;

@Service
public class ClassService {

    @Autowired
    private SemesterRepository semesterRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ClazzRepository clazzRepository;
    @Autowired
    private ClassScheduleRepository classScheduleRepository;
    @Autowired
    private ClassSessionRepository classSessionRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private ClassMapper classMapper;

    @Transactional
    public ClassResponse createClass(CreateClassRequest req) {
        // 1) Load & validate
        Semester semester = null;
        if (req.getSemesterId() != null) {
            semester = semesterRepository.findById(req.getSemesterId())
                    .orElseThrow(() -> new RuntimeException("Semester not found"));
        }

        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        // Note: teacherId from frontend is actually userId, so we need to find Teacher by userId
        Teacher teacher = teacherRepository.findByUserId(req.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found with userId: " + req.getTeacherId()));

        // Room is optional (null for online classes)
        Room room = null;
        boolean isOnline = (req.getRoomId() == null);

        if (!isOnline) {
            room = roomRepository.findById(req.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));
            if (room.getStatus() != fpt.capstone.edu360managementsystem.enums.RoomStatus.AVAILABLE) {
                throw new RuntimeException("Room is not available");
            }
        }

        // Enforce active/enabled states (backend safety, FE already checks but must not rely solely on FE)
        if (subject.getStatus() != fpt.capstone.edu360managementsystem.enums.SubjectStatus.AVAILABLE) {
            throw new RuntimeException("Subject is not available");
        }
        if (teacher.getUser() == null || Boolean.FALSE.equals(teacher.getUser().getActive())) {
            throw new RuntimeException("Teacher account is not active");
        }

        // Teacher phải dạy đúng subject: ưu tiên subject chính, sau đó xem thêm danh sách subjects mở rộng
        boolean teachesSubject = false;
        if (teacher.getSubject() != null && teacher.getSubject().getId().equals(subject.getId())) {
            teachesSubject = true;
        } else if (teacher.getSubjects() != null) {
            teachesSubject = teacher.getSubjects().stream().anyMatch(s -> s.getId().equals(subject.getId()));
        }
        if (!teachesSubject) {
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

        // Check xung đột giáo viên (theo khoảng thời gian startDate-endDate)
        var teacherConflicts = clazzRepository.findTeacherConflictsByDateRange(
                teacher.getId(), req.getStartDate(), req.getEndDate(), dows, slotIds);
        if (!teacherConflicts.isEmpty()) {
            throw new RuntimeException("Teacher has conflicting class schedules in this date range");
        }

        // Check xung đột phòng (chỉ khi offline)
        if (!isOnline && room != null) {
            var roomConflicts = clazzRepository.findRoomConflictsByDateRange(
                    room.getId(), req.getStartDate(), req.getEndDate(), dows, slotIds);
            if (!roomConflicts.isEmpty()) {
                throw new RuntimeException("Room has conflicting class schedules in this date range");
            }
        }

        // Xác định maxStudents
        int maxStudents;
        if (isOnline) {
            // Online: bắt buộc nhập maxStudents
            if (req.getMaxStudents() == null || req.getMaxStudents() < 1) {
                throw new RuntimeException("maxStudents is required for online classes");
            }
            maxStudents = req.getMaxStudents();
        } else {
            // Offline: dùng room capacity nếu không nhập
            if (room == null) {
                throw new RuntimeException("Room must not be null for offline classes");
            }
            maxStudents = Optional.ofNullable(req.getMaxStudents()).orElse(room.getCapacity());
            if (maxStudents > room.getCapacity()) {
                throw new RuntimeException("maxStudents cannot exceed room capacity");
            }
        }

        // startDate/endDate: sử dụng từ request
        LocalDate classStart = req.getStartDate();
        LocalDate classEnd = req.getEndDate();

        // Tạo lớp
        Clazz clazz = Clazz.builder()
                .name(req.getName())
                .semester(semester)
                .subject(subject)
                .teacher(teacher)
                .room(room)
                .startDate(classStart)
                .endDate(classEnd)
                .maxStudents(maxStudents)
                .description(req.getDescription())
                .meetingLink(req.getMeetingLink()) // for online classes
                .status(semester != null ? deriveClassStatus(semester) : ClassStatus.AVAILABLE)
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
        List<ClassSession> sessions = generateSessionsByDateRange(clazz, room, classStart, classEnd, schedules, total);
        classSessionRepository.saveAll(sessions);

        return classMapper.toResponse(clazz, schedules, sessions.size());
    }

    /**
     * List classes with optional filters: teacherUserId & timeSlotId. TimeSlot
     * filter applied in-memory after fetching schedules due to simple JPA
     * query.
     */
    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses(Long teacherUserId, Long timeSlotId) {
        // fetch base classes with teacher filter
        List<Clazz> classes = clazzRepository.findAllWithFilters(teacherUserId);

        // load schedules for each class (N+1 acceptable for now; can optimize later)
        List<ClassSchedule> allSchedules = classScheduleRepository.findAll();
        Map<Long, List<ClassSchedule>> schedulesByClass = allSchedules.stream()
                .collect(Collectors.groupingBy(cs -> cs.getClazz().getId()));

        return classes.stream()
                .filter(c -> {
                    if (timeSlotId == null) {
                        return true;
                    }
                    var list = schedulesByClass.getOrDefault(c.getId(), List.of());
                    return list.stream().anyMatch(s -> s.getTimeSlot().getId().equals(timeSlotId));
                })
                .map(c -> classMapper.toResponse(c, schedulesByClass.get(c.getId()), 0))
                .toList();
    }

    private List<ClassSession> generateSessionsByDateRange(Clazz clazz, Room room,
            LocalDate startDate, LocalDate endDate,
            List<ClassSchedule> schedules, int totalSessions) {
        Map<Integer, List<TimeSlot>> map = schedules.stream()
                .collect(Collectors.groupingBy(ClassSchedule::getDayOfWeek,
                        Collectors.mapping(ClassSchedule::getTimeSlot, Collectors.toList())));

        List<ClassSession> out = new ArrayList<>();
        LocalDate d = startDate;
        while (!d.isAfter(endDate) && out.size() < totalSessions) {
            int dow = d.getDayOfWeek().getValue(); // 1..7
            if (map.containsKey(dow)) {
                for (TimeSlot slot : map.get(dow)) {
                    if (out.size() >= totalSessions) {
                        break;
                    }
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
            throw new RuntimeException("Not enough days in date range to generate required sessions");
        }
        return out;
    }

    private ClassStatus deriveClassStatus(Semester sem) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(sem.getStartDate())) {
            return ClassStatus.COMING_SOON;
        }
        if (today.isAfter(sem.getEndDate())) {
            return ClassStatus.COMPLETE;
        }
        return ClassStatus.STUDYING;
    }
}
