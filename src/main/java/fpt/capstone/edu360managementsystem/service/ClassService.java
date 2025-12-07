package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.CreateClassRequest;
import fpt.capstone.edu360managementsystem.dto.request.ScheduleItemRequest;
import fpt.capstone.edu360managementsystem.dto.request.UpdateClassRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassPublicDetailResponse;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.entity.CourseChapter;
import fpt.capstone.edu360managementsystem.entity.CourseLesson;
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
import fpt.capstone.edu360managementsystem.repository.CourseRepository;
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

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private fpt.capstone.edu360managementsystem.repository.CourseChapterRepository courseChapterRepository;
    @Autowired
    private fpt.capstone.edu360managementsystem.repository.CourseLessonRepository courseLessonRepository;

    @Autowired
    private fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository classEnrollmentRepository;
    @Autowired
    private fpt.capstone.edu360managementsystem.repository.SessionChapterRepository sessionChapterRepository;
    @Autowired
    private fpt.capstone.edu360managementsystem.repository.SessionLessonRepository sessionLessonRepository;
    @Autowired
    private fpt.capstone.edu360managementsystem.repository.SessionContentConfigRepository sessionContentConfigRepository;

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

        // Load course nếu có
        Course course = null;
        if (req.getCourseId() != null) {
            course = courseRepository.findById(req.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            if (!course.getSubject().getId().equals(subject.getId())) {
                throw new RuntimeException("Course does not belong to selected subject");
            }
            if (course.getStatus() != fpt.capstone.edu360managementsystem.enums.CourseStatus.APPROVED) {
                throw new RuntimeException("Course is not approved");
            }
        }

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

        // Validate: Giáo viên không được dạy quá 3 slot/ngày thường, 5 slot/ngày cuối tuần
        Map<Integer, Long> slotsPerDay = req.getSchedule().stream()
                .collect(Collectors.groupingBy(ScheduleItemRequest::getDayOfWeek, Collectors.counting()));
        for (Map.Entry<Integer, Long> entry : slotsPerDay.entrySet()) {
            int dayOfWeek = entry.getKey();
            long slotCount = entry.getValue();
            String dayName = getDayName(dayOfWeek);

            // Thứ 7 (6) và Chủ nhật (7): tối đa 5 slot
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                if (slotCount > 5) {
                    throw new RuntimeException("Giáo viên không được dạy quá 5 slot vào cuối tuần (vi phạm: " + dayName + " có " + slotCount + " slot)");
                }
            } else {
                // Các ngày thường (Thứ 2-6): tối đa 3 slot
                if (slotCount > 3) {
                    throw new RuntimeException("Giáo viên không được dạy quá 3 slot vào ngày thường (vi phạm: " + dayName + " có " + slotCount + " slot)");
                }
            }
        }

        // Check xung đột giáo viên (theo khoảng thời gian startDate-endDate)
        var teacherConflictsRaw = clazzRepository.findTeacherConflictsByDateRange(
                teacher.getId(), req.getStartDate(), req.getEndDate(), dows, slotIds);
        // Lọc lại theo cặp (dayOfWeek,timeSlotId) chính xác để tránh false-positive
        Set<String> requestedPairs = req.getSchedule().stream()
                .map(si -> si.getDayOfWeek() + "-" + si.getTimeSlotId())
                .collect(Collectors.toSet());
        var teacherConflicts = teacherConflictsRaw.stream()
                .filter(c -> classScheduleRepository.findByClazz_Id(c.getId()).stream()
                .anyMatch(s -> requestedPairs.contains(s.getDayOfWeek() + "-" + s.getTimeSlot().getId())))
                .toList();
        if (!teacherConflicts.isEmpty()) {
            System.out.println(" [CONFLICT] Teacher conflict detected (filtered exact pairs)!");
            System.out.println("   Teacher: " + teacher.getUser().getFullName() + " (ID: " + teacher.getId() + ")");
            System.out.println("   Requested date range: " + req.getStartDate() + " -> " + req.getEndDate());
            System.out.println("   Requested schedule pairs: " + requestedPairs);
            System.out.println("   Conflicting classes:");
            teacherConflicts.forEach(c -> {
                var schedules = classScheduleRepository.findByClazz_Id(c.getId());
                String scheduleInfo = schedules.stream()
                        .map(s -> getDayName(s.getDayOfWeek()) + " slot-" + s.getTimeSlot().getId())
                        .collect(Collectors.joining(", "));
                System.out.println("      - Class ID " + c.getId() + ": " + c.getName()
                        + " (" + c.getStartDate() + " -> " + c.getEndDate() + ")"
                        + " [" + scheduleInfo + "]");
            });
            throw new RuntimeException("Giáo viên " + teacher.getUser().getFullName()
                    + " đã có lớp xung đột. Vui lòng chọn khung giờ hoặc ngày khác.");
        }

        // Check xung đột phòng (chỉ khi offline)
        if (!isOnline && room != null) {
            var roomConflictsRaw = clazzRepository.findRoomConflictsByDateRange(
                    room.getId(), req.getStartDate(), req.getEndDate(), dows, slotIds);
            Set<String> requestedPairsRoom = req.getSchedule().stream()
                    .map(si -> si.getDayOfWeek() + "-" + si.getTimeSlotId())
                    .collect(Collectors.toSet());
            var roomConflicts = roomConflictsRaw.stream()
                    .filter(c -> classScheduleRepository.findByClazz_Id(c.getId()).stream()
                    .anyMatch(s -> requestedPairsRoom.contains(s.getDayOfWeek() + "-" + s.getTimeSlot().getId())))
                    .toList();
            if (!roomConflicts.isEmpty()) {
                System.out.println(" [CONFLICT] Room conflict detected (filtered exact pairs)!");
                System.out.println("   Room: " + room.getName() + " (ID: " + room.getId() + ")");
                System.out.println("   Requested date range: " + req.getStartDate() + " -> " + req.getEndDate());
                System.out.println("   Requested schedule pairs: " + requestedPairsRoom);
                System.out.println("   Conflicting classes:");
                roomConflicts.forEach(c -> {
                    var schedules = classScheduleRepository.findByClazz_Id(c.getId());
                    String scheduleInfo = schedules.stream()
                            .map(s -> getDayName(s.getDayOfWeek()) + " slot-" + s.getTimeSlot().getId())
                            .collect(Collectors.joining(", "));
                    System.out.println("      - Class ID " + c.getId() + ": " + c.getName()
                            + " (" + c.getStartDate() + " -> " + c.getEndDate() + ")"
                            + " [" + scheduleInfo + "]");
                });
                throw new RuntimeException("Phòng " + room.getName()
                        + " đã có lớp xung đột. Vui lòng chọn phòng, khung giờ hoặc ngày khác.");
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

        // Giá mỗi buổi: nếu FE không gửi, đặt mặc định 0
        Long pricePerSession = Optional.ofNullable(req.getPricePerSession()).orElse(0L);

        // startDate: sử dụng từ request
        // endDate: tự động tính dựa trên startDate, totalSessions và schedule
        LocalDate classStart = req.getStartDate();
        LocalDate classEnd = calculateEndDate(classStart, req.getTotalSessions(), slotsPerDay);

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
                .pricePerSession(pricePerSession)
                .description(req.getDescription())
                .meetingLink(req.getMeetingLink())
                // New lifecycle: default to DRAFT on creation
                .status(ClassStatus.DRAFT)
                .course(course)
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

        // Auto-create ClassCourse (clone course template to teacher-owned course for this class)
        if (course != null) {
            createClassCourseForClass(clazz, teacher, course);
        }

        return classMapper.toResponse(clazz, schedules, sessions.size());
    }

    /**
     * Clone toàn bộ chương/bài từ course template sang một course mới thuộc
     * giáo viên (ClassCourse). Tiêu đề gợi ý: "<course.title> - <clazz.name>".
     * Course mới sẽ có subject giống template, ownerTeacher là giáo viên của
     * lớp, createdBy là user của giáo viên. SAU ĐÓ GÁN course mới cho lớp.
     */
    private void createClassCourseForClass(Clazz clazz, Teacher teacher, Course template) {
        try {
            String newTitle = template.getTitle() + " - " + clazz.getName();
            // Thêm SOURCE tag vào description để FE có thể tìm lại baseCourseId
            String newDescription = (template.getDescription() != null ? template.getDescription() : "")
                    + "\n[[SOURCE:" + template.getId() + "]]";
            Course newCourse = Course.builder()
                    .subject(template.getSubject())
                    .title(newTitle)
                    .description(newDescription)
                    .status(fpt.capstone.edu360managementsystem.enums.CourseStatus.APPROVED)
                    .createdBy(teacher.getUser())
                    .ownerTeacher(teacher)
                    .build();
            courseRepository.save(newCourse);

            // Clone chapters
            var chapters = courseChapterRepository.findByCourse_IdOrderByOrderIndexAsc(template.getId());
            for (var ch : chapters) {
                CourseChapter newChapter = CourseChapter.builder()
                        .course(newCourse)
                        .title(ch.getTitle())
                        .description(ch.getDescription())
                        .orderIndex(ch.getOrderIndex())
                        .build();
                courseChapterRepository.save(newChapter);

                // Clone lessons for this chapter
                var lessons = courseLessonRepository.findByChapter_IdOrderByOrderIndexAsc(ch.getId());
                for (var ls : lessons) {
                    CourseLesson newLesson = CourseLesson.builder()
                            .chapter(newChapter)
                            .title(ls.getTitle())
                            .description(ls.getDescription())
                            .orderIndex(ls.getOrderIndex())
                            .build();
                    courseLessonRepository.save(newLesson);
                }
            }

            // GÁN course mới cho lớp (thay thế course base)
            clazz.setCourse(newCourse);
            clazzRepository.save(clazz);

            System.out.println(" [ClassService] Created class course '" + newTitle + "' (id=" + newCourse.getId() + ") for class " + clazz.getId());
        } catch (Exception e) {
            System.out.println(" [ClassService] Failed to create class course: " + e.getMessage());
            // Do not fail class creation due to class-course cloning; log and continue
        }
    }

    /**
     * List classes with optional filters: teacherUserId & timeSlotId. TimeSlot
     * filter applied in-memory after fetching schedules due to simple JPA
     * query.
     */
    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses(Long teacherUserId, Long timeSlotId) {
        System.out.println(" [LIST_CLASSES] Called with filters - teacherUserId: " + teacherUserId + ", timeSlotId: " + timeSlotId);

        // fetch base classes with teacher filter
        List<Clazz> classes = clazzRepository.findAllWithFilters(teacherUserId);
        System.out.println(" [LIST_CLASSES] Found " + classes.size() + " classes after teacher filter");

        // Log first few classes for debugging
        classes.stream().limit(5).forEach(c
                -> System.out.println("   Class: id=" + c.getId() + ", name=" + c.getName()
                        + ", teacher=" + c.getTeacher().getUser().getFullName()
                        + " (userId=" + c.getTeacher().getUser().getId() + ")")
        );

        // Load ALL schedules once to avoid N+1 queries
        List<ClassSchedule> allSchedules = classScheduleRepository.findAll();
        Map<Long, List<ClassSchedule>> schedulesByClass = allSchedules.stream()
                .collect(Collectors.groupingBy(cs -> cs.getClazz().getId()));

        // Debug: Log schedule data
        System.out.println(" Total classes: " + classes.size());
        System.out.println(" Total schedules: " + allSchedules.size());
        schedulesByClass.forEach((classId, schedules) -> {
            System.out.println("  Class " + classId + " has " + schedules.size() + " schedule items");
        });

        return classes.stream()
                .filter(c -> {
                    if (timeSlotId == null) {
                        return true;
                    }
                    var list = schedulesByClass.getOrDefault(c.getId(), List.of());
                    return list.stream().anyMatch(s -> s.getTimeSlot().getId().equals(timeSlotId));
                })
                .map(c -> {
                    List<ClassSchedule> classSchedules = schedulesByClass.getOrDefault(c.getId(), List.of());
                    // Count current enrolled students
                    int currentStudents = classEnrollmentRepository.countByClazz_Id(c.getId());
                    ClassResponse response = classMapper.toResponse(c, classSchedules, 0);
                    response.setCurrentStudents(currentStudents);

                    // Log each class being returned
                    System.out.println("    Returning class: id=" + c.getId() + ", name=" + c.getName()
                            + ", teacher=" + c.getTeacher().getUser().getFullName()
                            + ", schedules=" + classSchedules.size()
                            + ", students=" + currentStudents);

                    return response;
                })
                .toList();
    }

    /**
     * Lấy danh sách classes với phân trang và filter
     *
     * @param search từ khóa tìm kiếm (name, teacherName, subjectName)
     * @param status filter theo ClassStatus (DRAFT, PUBLIC, ARCHIVED)
     * @param online filter theo hình thức học (true=online, false=offline)
     * @param teacherUserId filter theo giáo viên
     * @param page số trang (bắt đầu từ 0)
     * @param size số phần tử mỗi trang
     * @param sortBy trường để sắp xếp
     * @param order thứ tự sắp xếp (asc, desc)
     * @return Page<ClassResponse>
     */
    @Transactional(readOnly = true)
    public Page<ClassResponse> getClassesWithPagination(
            String search,
            String status,
            String online,
            Long teacherUserId,
            int page,
            int size,
            String sortBy,
            String order
    ) {
        // Xử lý sort
        Sort sort = Sort.by(sortBy != null ? sortBy : "id");
        if ("desc".equalsIgnoreCase(order)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        // Xử lý status filter
        ClassStatus statusEnum = null;
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusEnum = ClassStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }

        // Xử lý online filter
        Boolean onlineBool = null;
        if (online != null && !online.isEmpty() && !"ALL".equalsIgnoreCase(online)) {
            onlineBool = Boolean.parseBoolean(online);
        }

        // Query với pagination
        Page<Clazz> classPage = clazzRepository.findBySearchAndFilters(
                search, statusEnum, onlineBool, teacherUserId, pageable
        );

        // Load ALL schedules for these classes để tránh N+1
        List<Long> classIds = classPage.getContent().stream().map(Clazz::getId).toList();
        List<ClassSchedule> allSchedules = classScheduleRepository.findByClazz_IdIn(classIds);
        Map<Long, List<ClassSchedule>> schedulesByClass = allSchedules.stream()
                .collect(Collectors.groupingBy(cs -> cs.getClazz().getId()));

        // Map to response with schedules and studentCount
        return classPage.map(c -> {
            List<ClassSchedule> classSchedules = schedulesByClass.getOrDefault(c.getId(), List.of());
            int currentStudents = classEnrollmentRepository.countByClazz_Id(c.getId());
            ClassResponse response = classMapper.toResponse(c, classSchedules, 0);
            response.setCurrentStudents(currentStudents);
            return response;
        });
    }

    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long id) {
        var clazz = clazzRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        var schedules = classScheduleRepository.findByClazz_Id(id);
        int currentStudents = classEnrollmentRepository.countByClazz_Id(id);
        int totalSessions = (int) classSessionRepository.countByClazz_Id(id);
        ClassResponse response = classMapper.toResponse(clazz, schedules, totalSessions);
        response.setCurrentStudents(currentStudents);
        return response;
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

    // private ClassStatus deriveClassStatus(Semester sem) {
    //     // For new lifecycle, classes created are DRAFT by default; this helper is not used for status anymore
    //     return ClassStatus.DRAFT;
    // }
    public void publishClass(Long id) {
        System.out.println("\uD83D\uDD0D [ClassService] publishClass id=" + id);
        Clazz clazz = clazzRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        System.out.println("   -> Current status=" + clazz.getStatus());
        clazz.setStatus(ClassStatus.PUBLIC);
        clazzRepository.save(clazz);
        System.out.println("   -> New status=" + clazz.getStatus());
    }

    public void revertToDraft(Long id) {
        System.out.println("\uD83D\uDD0D [ClassService] revertToDraft id=" + id);
        Clazz clazz = clazzRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        System.out.println("   -> Current status=" + clazz.getStatus());
        LocalDate today = LocalDate.now();
        // Guard: if any session date is before today, cannot revert
        boolean hasPastSession = classSessionRepository.existsByClazz_IdAndDateBefore(clazz.getId(), today);
        System.out.println("   -> hasPastSessionBeforeToday=" + hasPastSession + ", today=" + today);
        if (hasPastSession) {
            System.out.println("    Revert blocked: sessions have started.");
            throw new IllegalStateException("Cannot revert to DRAFT after sessions have started");
        }
        clazz.setStatus(ClassStatus.DRAFT);
        clazzRepository.save(clazz);
        System.out.println("   -> New status=" + clazz.getStatus());
    }

    @Transactional
    public ClassResponse updateClass(Long id, UpdateClassRequest req) {
        var clazz = clazzRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));

        // Capture pre-change references for teacher/course to detect changes later
        Teacher oldTeacherRef = clazz.getTeacher();
        Course oldCourseRef = clazz.getCourse();

        boolean isDraft = clazz.getStatus() == ClassStatus.DRAFT;

        // Only a subset allowed for NON-DRAFT (PUBLIC or others)
        if (!isDraft) {
            // Update room (null => online)
            Room room = null;
            if (req.getRoomId() != null) {
                room = roomRepository.findById(req.getRoomId()).orElseThrow(() -> new RuntimeException("Room not found"));
            }
            clazz.setRoom(room);

            // Update maxStudents
            if (req.getMaxStudents() != null) {
                int max = req.getMaxStudents();
                if (room != null && max > room.getCapacity()) {
                    throw new IllegalStateException("maxStudents cannot exceed room capacity");
                }
                clazz.setMaxStudents(max);
            }

            // Allow meetingLink update for PUBLIC online classes
            if (req.getMeetingLink() != null) {
                clazz.setMeetingLink(req.getMeetingLink());
            }
        } else {
            // Full edit for drafts (allow regardless of date as per business rule)
            if (req.getName() != null) {
                clazz.setName(req.getName());
            }
            if (req.getDescription() != null) {
                clazz.setDescription(req.getDescription());
            }
            if (req.getMeetingLink() != null) {
                clazz.setMeetingLink(req.getMeetingLink());
            }
            // Allow updating price per session for DRAFT
            if (req.getPricePerSession() != null) {
                Long p = req.getPricePerSession();
                if (p < 0) {
                    throw new IllegalArgumentException("pricePerSession must be >= 0");
                }
                clazz.setPricePerSession(p);
            }

            // Subject / Course / Teacher updates (chỉ khi DRAFT và chưa bắt đầu)
            if (req.getSubjectId() != null) {
                Subject subject = subjectRepository.findById(req.getSubjectId())
                        .orElseThrow(() -> new RuntimeException("Subject not found"));
                if (subject.getStatus() != fpt.capstone.edu360managementsystem.enums.SubjectStatus.AVAILABLE) {
                    throw new RuntimeException("Subject is not available");
                }
                clazz.setSubject(subject);
                // Course phải thuộc subject
                if (req.getCourseId() != null) {
                    Course course = courseRepository.findById(req.getCourseId())
                            .orElseThrow(() -> new RuntimeException("Course not found"));
                    if (!course.getSubject().getId().equals(subject.getId())) {
                        throw new RuntimeException("Course does not belong to selected subject");
                    }
                    if (course.getStatus() != fpt.capstone.edu360managementsystem.enums.CourseStatus.APPROVED) {
                        throw new RuntimeException("Course is not approved");
                    }
                    clazz.setCourse(course);
                } else {
                    clazz.setCourse(null);
                }
            } else if (req.getCourseId() != null) {
                // Nếu không đổi subject nhưng đổi course, vẫn kiểm tra quan hệ
                Course course = courseRepository.findById(req.getCourseId())
                        .orElseThrow(() -> new RuntimeException("Course not found"));
                if (!course.getSubject().getId().equals(clazz.getSubject().getId())) {
                    throw new RuntimeException("Course does not belong to current subject");
                }
                if (course.getStatus() != fpt.capstone.edu360managementsystem.enums.CourseStatus.APPROVED) {
                    throw new RuntimeException("Course is not approved");
                }
                clazz.setCourse(course);
            }

            if (req.getTeacherId() != null) {
                Teacher newTeacher = teacherRepository.findByUserId(req.getTeacherId())
                        .orElseThrow(() -> new RuntimeException("Teacher not found with userId: " + req.getTeacherId()));
                // Kiểm tra teacher dạy được subject hiện tại
                Subject subject = clazz.getSubject();
                boolean teachesSubject = false;
                if (newTeacher.getSubject() != null && newTeacher.getSubject().getId().equals(subject.getId())) {
                    teachesSubject = true;
                } else if (newTeacher.getSubjects() != null) {
                    teachesSubject = newTeacher.getSubjects().stream().anyMatch(s -> s.getId().equals(subject.getId()));
                }
                if (!teachesSubject) {
                    throw new RuntimeException("Teacher does not teach the selected subject");
                }
                clazz.setTeacher(newTeacher);

                // If teacher actually changed, handle teacher-course migration:
                if (oldTeacherRef != null && !oldTeacherRef.getId().equals(newTeacher.getId())) {
                    // 1) Determine template course for cloning to the new teacher
                    Course templateCourse = null;
                    if (req.getCourseId() != null) {
                        templateCourse = courseRepository.findById(req.getCourseId())
                                .orElseThrow(() -> new RuntimeException("Course not found"));
                    } else if (oldCourseRef != null) {
                        templateCourse = oldCourseRef;
                    }

                    // 2) Clear all session content to avoid FK conflicts with course chapters/lessons
                    var oldSessionsForClass = classSessionRepository.findByClazz_Id(id);
                    for (var s : oldSessionsForClass) {
                        Long sid = s.getId();
                        try {
                            sessionChapterRepository.deleteBySession_Id(sid);
                            sessionLessonRepository.deleteBySession_Id(sid);
                            sessionContentConfigRepository.findBySession_Id(sid)
                                    .ifPresent(sessionContentConfigRepository::delete);
                            s.setLessonContent(null);
                        } catch (Exception ignore) {
                            // ignore per-session clean errors; proceed best-effort
                        }
                    }
                    classSessionRepository.saveAll(oldSessionsForClass);

                    // 3) Create new teacher-owned course for this class (clone from template if available)
                    if (templateCourse != null) {
                        createClassCourseForClass(clazz, newTeacher, templateCourse);
                    }

                    // 4) Delete the old teacher-owned course (only if owned by the old teacher)
                    if (oldCourseRef != null
                            && oldCourseRef.getOwnerTeacher() != null
                            && oldCourseRef.getOwnerTeacher().getId().equals(oldTeacherRef.getId())) {
                        // Detach from class if still linked (createClassCourseForClass should have switched it)
                        if (clazz.getCourse() != null && clazz.getCourse().getId().equals(oldCourseRef.getId())) {
                            clazz.setCourse(null);
                            clazzRepository.save(clazz);
                        }
                        try {
                            var chapters = courseChapterRepository.findByCourse_IdOrderByOrderIndexAsc(oldCourseRef.getId());
                            for (var ch : chapters) {
                                var lessons = courseLessonRepository.findByChapter_IdOrderByOrderIndexAsc(ch.getId());
                                courseLessonRepository.deleteAll(lessons);
                            }
                            courseChapterRepository.deleteAll(chapters);
                            courseRepository.delete(oldCourseRef);
                        } catch (Exception ignore) {
                            // ignore delete issues to avoid blocking the update
                        }
                    }
                }
            }

            // Room/online switch
            Room room = null;
            if (req.getRoomId() != null) {
                room = roomRepository.findById(req.getRoomId()).orElseThrow(() -> new RuntimeException("Room not found"));
            }
            clazz.setRoom(room);

            if (req.getMaxStudents() != null) {
                int max = req.getMaxStudents();
                if (room != null && max > room.getCapacity()) {
                    throw new IllegalStateException("maxStudents cannot exceed room capacity");
                }
                clazz.setMaxStudents(max);
            }

            // Allow adjusting dates simply (without regenerating sessions here)
            if (req.getStartDate() != null) {
                clazz.setStartDate(req.getStartDate());
            }
            if (req.getEndDate() != null) {
                clazz.setEndDate(req.getEndDate());
            }

            // Lịch & totalSessions: chỉ thực sự cập nhật khi THAY ĐỔI so với hiện tại
            Integer totalSessions = req.getTotalSessions();
            // So sánh schedule hiện có và schedule từ request (nếu có) BẰNG THỜI GIAN (start-end)
            var existingSchedules = classScheduleRepository.findByClazz_Id(id);
            java.util.Set<String> existingTimePairs = existingSchedules.stream()
                    .map(s -> s.getDayOfWeek() + "-" + s.getTimeSlot().getStartTime() + "-" + s.getTimeSlot().getEndTime())
                    .collect(Collectors.toSet());
            boolean hasReqSchedule = req.getSchedule() != null && !req.getSchedule().isEmpty();
            java.util.Set<String> requestedTimePairs = new java.util.HashSet<>();
            if (hasReqSchedule) {
                for (var si : req.getSchedule()) {
                    int dow = si.getDayOfWeek();
                    if (dow == 0) {
                        dow = 7; // normalize Sunday

                    }
                    TimeSlot slot = timeSlotRepository.findById(si.getTimeSlotId())
                            .orElse(null);
                    if (slot != null) {
                        requestedTimePairs.add(dow + "-" + slot.getStartTime() + "-" + slot.getEndTime());
                    }
                }
            }
            boolean scheduleChanged = hasReqSchedule && !requestedTimePairs.equals(existingTimePairs);

            // Xác định số sessions hiện tại để tránh xoá/regen không cần thiết
            int currentSessionsCount = (int) classSessionRepository.countByClazz_Id(id);
            boolean totalSessionsChanged = totalSessions != null && totalSessions > 0 && totalSessions != currentSessionsCount;

            if (scheduleChanged) {
                // Xóa lịch cũ
                classScheduleRepository.deleteAll(existingSchedules);
                // Tạo lịch mới
                List<ClassSchedule> newSchedules = req.getSchedule().stream().map(si -> {
                    // FE đang gửi dayOfWeek theo chuẩn 1..7? Nếu FE gửi 1..7 thì convert sang 1..7 cho entity.
                    // Nếu FE gửi 0..6 (0=CN) thì chuyển 0->7.
                    int dow = si.getDayOfWeek();
                    if (dow == 0) {
                        dow = 7; // normalize Sunday

                    }
                    TimeSlot slot = timeSlotRepository.findById(si.getTimeSlotId())
                            .orElseThrow(() -> new RuntimeException("Invalid time slot id: " + si.getTimeSlotId()));
                    return ClassSchedule.builder()
                            .clazz(clazz)
                            .dayOfWeek(dow)
                            .timeSlot(slot)
                            .build();
                }).toList();
                classScheduleRepository.saveAll(newSchedules);

                // Re-calc endDate nếu không được gửi trực tiếp nhưng có totalSessions
                if (totalSessions != null && totalSessions > 0 && (req.getEndDate() == null)) {
                    // Map slots per day
                    var slotsPerDay = newSchedules.stream().collect(Collectors.groupingBy(ClassSchedule::getDayOfWeek, Collectors.counting()));
                    LocalDate newEnd = calculateEndDate(clazz.getStartDate(), totalSessions, slotsPerDay);
                    clazz.setEndDate(newEnd);
                }

                // Regenerate sessions nếu có totalSessions mới VÀ khác với hiện tại
                if (totalSessionsChanged) {
                    // Nếu lớp có nội dung buổi, yêu cầu xác nhận trước khi xoá nội dung + khoá học GV
                    var oldSessions = classSessionRepository.findByClazz_Id(id);
                    boolean hasContent = false;
                    for (var s : oldSessions) {
                        Long sid = s.getId();
                        if (s.getLessonContent() != null && !s.getLessonContent().isBlank()) {
                            hasContent = true;
                            break;
                        }
                        if (!sessionChapterRepository.findBySession_Id(sid).isEmpty()) {
                            hasContent = true;
                            break;
                        }
                        if (!sessionLessonRepository.findBySession_Id(sid).isEmpty()) {
                            hasContent = true;
                            break;
                        }
                        if (sessionContentConfigRepository.existsBySession_Id(sid)) {
                            hasContent = true;
                            break;
                        }
                    }

                    boolean force = Boolean.TRUE.equals(req.getForceDeleteContentAndCourse());
                    if (hasContent && !force) {
                        throw new IllegalStateException("Lớp đang có nội dung. Vui lòng xác nhận để xóa toàn bộ nội dung buổi học và khóa học của giáo viên.");
                    }

                    if (hasContent && force) {
                        // 1) Xoá dữ liệu phụ thuộc của từng session
                        for (var s : oldSessions) {
                            Long sid = s.getId();
                            sessionChapterRepository.deleteBySession_Id(sid);
                            sessionLessonRepository.deleteBySession_Id(sid);
                            sessionContentConfigRepository.findBySession_Id(sid)
                                    .ifPresent(sessionContentConfigRepository::delete);
                            s.setLessonContent(null);
                        }
                        classSessionRepository.saveAll(oldSessions);

                        // 2) Xóa khoá học của giáo viên (nếu là khoá học thuộc giáo viên lớp này)
                        Course currentCourse = clazz.getCourse();
                        if (currentCourse != null
                                && currentCourse.getOwnerTeacher() != null
                                && clazz.getTeacher() != null
                                && currentCourse.getOwnerTeacher().getId().equals(clazz.getTeacher().getId())) {
                            // Gỡ liên kết khỏi lớp trước
                            clazz.setCourse(null);
                            clazzRepository.save(clazz);
                            try {
                                // Xoá lessons -> chapters -> course
                                var chapters = courseChapterRepository.findByCourse_IdOrderByOrderIndexAsc(currentCourse.getId());
                                for (var ch : chapters) {
                                    var lessons = courseLessonRepository.findByChapter_IdOrderByOrderIndexAsc(ch.getId());
                                    courseLessonRepository.deleteAll(lessons);
                                }
                                courseChapterRepository.deleteAll(chapters);
                                courseRepository.delete(currentCourse);
                            } catch (Exception ignore) {
                                // Nếu xoá cascade đã cấu hình ở DB/JPA, có thể bỏ qua lỗi nhỏ
                            }
                        }
                    }

                    // Xóa session cũ và sinh lại theo lịch mới
                    classSessionRepository.deleteAll(oldSessions);
                    var slotsPerDay2 = classScheduleRepository.findByClazz_Id(id).stream().collect(Collectors.groupingBy(ClassSchedule::getDayOfWeek, Collectors.counting()));
                    LocalDate endDateEffective = clazz.getEndDate();
                    // Nếu endDate chưa tính hoặc null, fallback tính lại
                    if (endDateEffective == null) {
                        endDateEffective = calculateEndDate(clazz.getStartDate(), totalSessions, slotsPerDay2);
                        clazz.setEndDate(endDateEffective);
                    }
                    // Generate sessions mới
                    List<ClassSchedule> currentSchedules = classScheduleRepository.findByClazz_Id(id);
                    Room currentRoom = clazz.getRoom();
                    List<ClassSession> regenerated = generateSessionsByDateRange(clazz, currentRoom, clazz.getStartDate(), clazz.getEndDate(), currentSchedules, totalSessions);
                    classSessionRepository.saveAll(regenerated);
                }
            } else if (totalSessionsChanged && req.getStartDate() != null && req.getEndDate() == null) {
                // Không đổi lịch nhưng muốn tính lại endDate dựa trên totalSessions mới
                var slotsPerDay = existingSchedules.stream().collect(Collectors.groupingBy(ClassSchedule::getDayOfWeek, Collectors.counting()));
                LocalDate newEnd = calculateEndDate(clazz.getStartDate(), totalSessions, slotsPerDay);
                clazz.setEndDate(newEnd);
            }
        }

        clazzRepository.save(clazz);
        var schedules = classScheduleRepository.findByClazz_Id(id);
        int currentStudents = classEnrollmentRepository.countByClazz_Id(id);
        ClassResponse response = classMapper.toResponse(clazz, schedules, 0);
        response.setCurrentStudents(currentStudents);
        return response;
    }

    private String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 ->
                "Thứ 2";
            case 2 ->
                "Thứ 3";
            case 3 ->
                "Thứ 4";
            case 4 ->
                "Thứ 5";
            case 5 ->
                "Thứ 6";
            case 6 ->
                "Thứ 7";
            case 7 ->
                "Chủ nhật";
            default ->
                "Ngày không xác định";
        };
    }

    /**
     * Tính ngày kết thúc dựa trên: - Ngày bắt đầu - Tổng số buổi cần dạy - Map
     * slotsPerDay: dayOfWeek -> số slot trong ngày đó
     */
    private LocalDate calculateEndDate(LocalDate startDate, int totalSessions, Map<Integer, Long> slotsPerDay) {
        if (slotsPerDay.isEmpty() || totalSessions <= 0) {
            return startDate;
        }

        int countedSlots = 0;
        LocalDate current = startDate;
        LocalDate lastDate = startDate;
        int maxIterations = (totalSessions * 7) + 14; // Safety limit
        int iterations = 0;

        while (countedSlots < totalSessions && iterations < maxIterations) {
            // Java DayOfWeek: 1=Monday, 7=Sunday (same as our convention)
            int dayOfWeek = current.getDayOfWeek().getValue();
            long slotsOnThisDay = slotsPerDay.getOrDefault(dayOfWeek, 0L);

            if (slotsOnThisDay > 0) {
                countedSlots += slotsOnThisDay;
                lastDate = current;
            }

            current = current.plusDays(1);
            iterations++;
        }

        return lastDate;
    }

    /**
     * Public API: Get class detail for guest/unauthenticated users. Returns
     * class info with base course (from Admin), not teacher's customized
     * version.
     */
    @Transactional(readOnly = true)
    public ClassPublicDetailResponse getClassPublicDetail(Long classId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // Guests must not see DRAFT classes
        if (clazz.getStatus() == ClassStatus.DRAFT) {
            throw new RuntimeException("Class not available");
        }

        var schedules = classScheduleRepository.findByClazz_Id(classId);
        int currentStudents = classEnrollmentRepository.countByClazz_Id(classId);
        long sessionsCount = classSessionRepository.countByClazz_Id(classId);
        int sessionsGenerated = (int) sessionsCount;

        // Build schedule view
        List<ClassPublicDetailResponse.ScheduleItemView> scheduleViews = schedules.stream()
                .map(s -> new ClassPublicDetailResponse.ScheduleItemView(
                s.getDayOfWeek(),
                s.getTimeSlot().getId(),
                s.getTimeSlot().getStartTime().toString(),
                s.getTimeSlot().getEndTime().toString()
        ))
                .toList();

        // Build course lessons view (from base course)
        List<ClassPublicDetailResponse.CourseLessonView> lessonViews = new ArrayList<>();
        if (clazz.getCourse() != null) {
            var chapters = courseChapterRepository.findByCourse_IdOrderByOrderIndexAsc(clazz.getCourse().getId());
            for (var chapter : chapters) {
                var lessons = courseLessonRepository.findByChapter_IdOrderByOrderIndexAsc(chapter.getId());
                for (var lesson : lessons) {
                    lessonViews.add(ClassPublicDetailResponse.CourseLessonView.builder()
                            .id(lesson.getId())
                            .title(lesson.getTitle())
                            .orderIndex(lesson.getOrderIndex())
                            .description(lesson.getDescription())
                            .build());
                }
            }
        }

        // Calculate total price
        Long totalPrice = null;
        if (clazz.getPricePerSession() != null && sessionsGenerated > 0) {
            totalPrice = clazz.getPricePerSession() * sessionsGenerated;
        }

        return ClassPublicDetailResponse.builder()
                .id(clazz.getId())
                .name(clazz.getName())
                .description(clazz.getDescription())
                .startDate(clazz.getStartDate())
                .endDate(clazz.getEndDate())
                .maxStudents(clazz.getMaxStudents())
                .currentStudents(currentStudents)
                .status(clazz.getStatus())
                .online(clazz.getMeetingLink() != null && !clazz.getMeetingLink().isBlank())
                .meetingLink(clazz.getMeetingLink())
                // Subject
                .subjectId(clazz.getSubject().getId())
                .subjectName(clazz.getSubject().getName())
                // Room
                .roomId(clazz.getRoom() != null ? clazz.getRoom().getId() : null)
                .roomName(clazz.getRoom() != null ? clazz.getRoom().getName() : null)
                // Semester
                .semesterId(clazz.getSemester() != null ? clazz.getSemester().getId() : null)
                .semesterName(clazz.getSemester() != null ? clazz.getSemester().getName() : null)
                // Teacher
                .teacherId(clazz.getTeacher().getId())
                .teacherFullName(clazz.getTeacher().getUser().getFullName())
                .teacherAvatarUrl(clazz.getTeacher().getAvatarUrl())
                .teacherBio(clazz.getTeacher().getBio())
                .teacherDepartment(clazz.getTeacher().getWorkplace())
                // Course (base course from Admin)
                .courseId(clazz.getCourse() != null ? clazz.getCourse().getId() : null)
                .courseTitle(clazz.getCourse() != null ? clazz.getCourse().getTitle() : null)
                .courseDescription(clazz.getCourse() != null ? clazz.getCourse().getDescription() : null)
                .courseThumbnail(null)
                .courseLessons(lessonViews)
                // Schedule
                .schedule(scheduleViews)
                .sessionsGenerated(sessionsGenerated)
                // Price
                .pricePerSession(clazz.getPricePerSession())
                .totalPrice(totalPrice)
                .build();
    }
}
