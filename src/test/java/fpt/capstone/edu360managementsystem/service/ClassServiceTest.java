package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import fpt.capstone.edu360managementsystem.dto.request.CreateClassRequest;
import fpt.capstone.edu360managementsystem.dto.request.ScheduleItemRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.entity.Room;
import fpt.capstone.edu360managementsystem.entity.Semester;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.TimeSlot;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.enums.RoomStatus;
import fpt.capstone.edu360managementsystem.enums.SemesterStatus;
import fpt.capstone.edu360managementsystem.enums.SessionStatus;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
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

/**
 * ClassService Unit Tests - 80 Cases
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassServiceTest {
    @Mock private SemesterRepository semesterRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ClazzRepository clazzRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ClassMapper classMapper;
    @Mock private fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository classEnrollmentRepository;
    @InjectMocks private ClassService classService;

    private CreateClassRequest validRequest;
    private Semester semester;
    private Subject subject;
    private Teacher teacher;
    private User teacherUser;
    private Room room;
    private TimeSlot timeSlot;
    private Course course;

    @BeforeEach
    void setUp() {
        semester = new Semester();
        semester.setId(1L);
        semester.setStartDate(LocalDate.of(2024, 9, 1));
        semester.setEndDate(LocalDate.of(2024, 12, 31));
        semester.setStatus(SemesterStatus.OPEN);
        subject = new Subject();
        subject.setId(1L);
        subject.setStatus(SubjectStatus.AVAILABLE);
        teacherUser = new User();
        teacherUser.setId(1L);
        teacherUser.setActive(true);
        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setUser(teacherUser);
        teacher.setSubject(subject);
        teacher.setSubjects(new HashSet<>(Arrays.asList(subject)));
        room = new Room();
        room.setId(1L);
        room.setCapacity(30);
        room.setStatus(RoomStatus.AVAILABLE);
        timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setStartTime(java.sql.Time.valueOf("08:00:00"));
        timeSlot.setEndTime(java.sql.Time.valueOf("10:00:00"));
        course = new Course();
        course.setId(1L);
        course.setSubject(subject);
        course.setStatus(CourseStatus.APPROVED);
        validRequest = new CreateClassRequest();
        validRequest.setName("Test Class");
        validRequest.setSemesterId(1L);
        validRequest.setSubjectId(1L);
        validRequest.setTeacherId(1L);
        validRequest.setRoomId(1L);
        validRequest.setStartDate(LocalDate.of(2024, 9, 1));
        validRequest.setEndDate(LocalDate.of(2024, 12, 31));
        validRequest.setTotalSessions(30);
        validRequest.setMaxStudents(25);
        ScheduleItemRequest scheduleItem = new ScheduleItemRequest();
        scheduleItem.setDayOfWeek(1);
        scheduleItem.setTimeSlotId(1L);
        validRequest.setSchedule(Arrays.asList(scheduleItem));
    }

    @Test void test01_semesterNotFound() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Semester not found");
    }
    @Test void test02_subjectNotFound() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Subject not found");
    }
    @Test void test03_teacherNotFound() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessageContaining("Teacher not found");
    }
    @Test void test04_roomNotFound() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Room not found");
    }
    @Test void test05_subjectNotAvailable() {
        subject.setStatus(SubjectStatus.UNAVAILABLE);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Subject is not available");
    }
    @Test void test06_teacherNotActive() {
        teacherUser.setActive(false);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Teacher account is not active");
    }
    @Test void test07_teacherDoesNotTeachSubject() {
        Subject other = new Subject(); other.setId(2L);
        teacher.setSubject(other); teacher.setSubjects(new HashSet<>(Arrays.asList(other)));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Teacher does not teach the selected subject");
    }
    @Test void test08_scheduleEmpty() {
        validRequest.setSchedule(Collections.emptyList());
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Schedule cannot be empty");
    }
    @Test void test09_courseNotFound() {
        validRequest.setCourseId(99L);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Course not found");
    }
    @Test void test10_courseNotBelongToSubject() {
        Subject other = new Subject(); other.setId(2L); course.setSubject(other);
        validRequest.setCourseId(1L);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Course does not belong to selected subject");
    }
    @Test void test11_onlineClassSuccess() {
        validRequest.setRoomId(null); validRequest.setMaxStudents(50);
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
        verify(roomRepository, never()).findById(anyLong());
    }
    @Test void test12_onlineClassNoMaxStudents() {
        validRequest.setRoomId(null); validRequest.setMaxStudents(null);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("maxStudents is required for online classes");
    }
    @Test void test13_onlineClassInvalidMaxStudents() {
        validRequest.setRoomId(null); validRequest.setMaxStudents(0);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("maxStudents is required for online classes");
    }
    @Test void test14_offlineRoomNotAvailable() {
        room.setStatus(RoomStatus.UNAVAILABLE);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Room is not available");
    }
    @Test void test15_offlineUseRoomCapacity() {
        validRequest.setMaxStudents(null);
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getMaxStudents() == 30));
    }
    @Test void test16_maxStudentsExceedsCapacity() {
        validRequest.setMaxStudents(50);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("maxStudents cannot exceed room capacity");
    }
    @Test void test17_maxStudentsEqualsCapacity() {
        validRequest.setMaxStudents(30);
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test18_maxStudentsLessThanCapacity() {
        validRequest.setMaxStudents(20);
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test19_offlineWithMeetingLink() {
        validRequest.setMeetingLink("https://zoom.us/123");
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getMeetingLink() != null));
    }
    @Test void test20_onlineWithMeetingLink() {
        validRequest.setRoomId(null); validRequest.setMaxStudents(50);
        validRequest.setMeetingLink("https://zoom.us/123");
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }

    @Test void test21_weekday1Slot() { mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test22_weekday3Slots() {
        validRequest.setSchedule(Arrays.asList(schedule(1,1L), schedule(1,2L), schedule(1,3L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test23_weekday4SlotsError() {
        validRequest.setSchedule(Arrays.asList(schedule(2,1L), schedule(2,2L), schedule(2,3L), schedule(2,4L)));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessageContaining("không được dạy quá 3 slot vào ngày thường");
    }
    @Test void test24_saturday5Slots() {
        validRequest.setSchedule(Arrays.asList(schedule(6,1L), schedule(6,2L), schedule(6,3L), schedule(6,4L), schedule(6,5L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test25_saturday6SlotsError() {
        validRequest.setSchedule(Arrays.asList(schedule(6,1L), schedule(6,2L), schedule(6,3L), schedule(6,4L), schedule(6,5L), schedule(6,6L)));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessageContaining("không được dạy quá 5 slot vào cuối tuần");
    }
    @Test void test26_sunday5Slots() {
        validRequest.setSchedule(Arrays.asList(schedule(7,1L), schedule(7,2L), schedule(7,3L), schedule(7,4L), schedule(7,5L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test27_sunday6SlotsError() {
        validRequest.setSchedule(Arrays.asList(schedule(7,1L), schedule(7,2L), schedule(7,3L), schedule(7,4L), schedule(7,5L), schedule(7,6L)));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessageContaining("không được dạy quá 5 slot vào cuối tuần");
    }
    @Test void test28_mixedScheduleValid() {
        validRequest.setSchedule(Arrays.asList(schedule(1,1L), schedule(1,2L), schedule(1,3L), schedule(6,4L), schedule(6,5L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test29_multipleDaysValid() {
        validRequest.setSchedule(Arrays.asList(schedule(1,1L), schedule(3,2L), schedule(5,3L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test30_allWeekdays() {
        validRequest.setSchedule(Arrays.asList(schedule(1,1L), schedule(2,1L), schedule(3,1L), schedule(4,1L), schedule(5,1L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test31_weekendOnly() {
        validRequest.setSchedule(Arrays.asList(schedule(6,1L), schedule(7,1L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test32_duplicateScheduleAllowed() {
        validRequest.setSchedule(Arrays.asList(schedule(1,1L), schedule(1,1L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test33_teacherConflict() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(timeSlotRepository.findById(anyLong())).thenReturn(Optional.of(timeSlot));
        Clazz conflictClass = new Clazz(); conflictClass.setId(99L);
        when(clazzRepository.findTeacherConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Arrays.asList(conflictClass));
        ClassSchedule cs = new ClassSchedule(); cs.setDayOfWeek(1); cs.setTimeSlot(timeSlot);
        when(classScheduleRepository.findByClazz_Id(99L)).thenReturn(Arrays.asList(cs));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessageContaining("xung đột");
    }
    @Test void test34_teacherNoConflict() { mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test35_roomConflict() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(timeSlotRepository.findById(anyLong())).thenReturn(Optional.of(timeSlot));
        when(clazzRepository.findTeacherConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Collections.emptyList());
        Clazz conflictClass = new Clazz(); conflictClass.setId(99L);
        when(clazzRepository.findRoomConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Arrays.asList(conflictClass));
        ClassSchedule cs = new ClassSchedule(); cs.setDayOfWeek(1); cs.setTimeSlot(timeSlot);
        when(classScheduleRepository.findByClazz_Id(99L)).thenReturn(Arrays.asList(cs));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessageContaining("xung đột");
    }
    @Test void test36_roomNoConflict() { mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test37_onlineNoRoomConflictCheck() {
        validRequest.setRoomId(null); validRequest.setMaxStudents(50);
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository, never()).findRoomConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet());
    }
    @Test void test38_generateCorrectSessionCount() {
        validRequest.setTotalSessions(10);
        mockValidCreate();
        classService.createClass(validRequest);
        verify(classSessionRepository).saveAll(argThat(list -> {
            List<ClassSession> sessions = new ArrayList<>();
            list.forEach(sessions::add);
            return sessions.size() == 10;
        }));
    }
    @Test void test39_respectDateBoundaries() {
        validRequest.setStartDate(LocalDate.of(2024, 9, 2));
        validRequest.setEndDate(LocalDate.of(2024, 9, 30));
        validRequest.setTotalSessions(4);
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test40_matchSchedulePattern() {
        validRequest.setSchedule(Arrays.asList(schedule(1,1L), schedule(3,2L)));
        validRequest.setTotalSessions(8);
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test41_sessionWithAllFields() {
        mockValidCreate();
        classService.createClass(validRequest);
        verify(classSessionRepository).saveAll(argThat(list -> {
            List<ClassSession> sessions = new ArrayList<>();
            list.forEach(sessions::add);
            return !sessions.isEmpty() && sessions.get(0).getStatus() == SessionStatus.PLANNED;
        }));
    }
    @Test void test42_calculateEndDate() {
        validRequest.setStartDate(LocalDate.of(2024, 9, 2));
        validRequest.setTotalSessions(4);
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getEndDate() != null));
    }
    @Test void test43_longDurationClass() { validRequest.setTotalSessions(50); mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test44_shortDurationClass() { validRequest.setTotalSessions(3); mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test45_singleDayPerWeek() { validRequest.setTotalSessions(8); mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test46_multipleDaysPerWeek() {
        validRequest.setSchedule(Arrays.asList(schedule(1,1L), schedule(3,2L), schedule(5,3L)));
        validRequest.setTotalSessions(12);
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test47_sessionsSavedCorrectly() { mockValidCreate(); classService.createClass(validRequest); verify(classSessionRepository).saveAll(anyList()); }
    
    private Clazz createClazzWithTeacher() {
        Clazz c = new Clazz();
        c.setId(1L);
        c.setTeacher(teacher);
        return c;
    }
    private Clazz createClazzWithTeacher(Long id) {
        Clazz c = new Clazz();
        c.setId(id);
        c.setTeacher(teacher);
        return c;
    }
    
    @Test void test48_listAllClasses() {
        when(clazzRepository.findAllWithFilters(null)).thenReturn(Arrays.asList(createClazzWithTeacher(1L), createClazzWithTeacher(2L)));
        when(classScheduleRepository.findAll()).thenReturn(Collections.emptyList());
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.listClasses(null, null)).hasSize(2);
    }
    @Test void test49_listByTeacher() {
        Clazz clazz = createClazzWithTeacher(1L);
        when(clazzRepository.findAllWithFilters(1L)).thenReturn(Arrays.asList(clazz));
        when(classScheduleRepository.findAll()).thenReturn(Collections.emptyList());
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.listClasses(1L, null)).hasSize(1);
    }
    @Test void test50_listByTimeSlot() {
        Clazz clazz = createClazzWithTeacher(1L);
        ClassSchedule schedule = new ClassSchedule(); schedule.setClazz(clazz); schedule.setTimeSlot(timeSlot);
        when(clazzRepository.findAllWithFilters(null)).thenReturn(Arrays.asList(clazz));
        when(classScheduleRepository.findAll()).thenReturn(Arrays.asList(schedule));
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.listClasses(null, 1L)).hasSize(1);
    }

    @Test void test51_listByBothFilters() {
        Clazz clazz = createClazzWithTeacher(1L);
        ClassSchedule schedule = new ClassSchedule(); schedule.setClazz(clazz); schedule.setTimeSlot(timeSlot);
        when(clazzRepository.findAllWithFilters(1L)).thenReturn(Arrays.asList(clazz));
        when(classScheduleRepository.findAll()).thenReturn(Arrays.asList(schedule));
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.listClasses(1L, 1L)).hasSize(1);
    }
    @Test void test52_listEmptyWhenNoClasses() {
        when(clazzRepository.findAllWithFilters(null)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.findAll()).thenReturn(Collections.emptyList());
        assertThat(classService.listClasses(null, null)).isEmpty();
    }
    @Test void test53_listEmptyWhenTeacherHasNoClasses() {
        when(clazzRepository.findAllWithFilters(1L)).thenReturn(Collections.emptyList());
        when(classScheduleRepository.findAll()).thenReturn(Collections.emptyList());
        assertThat(classService.listClasses(1L, null)).isEmpty();
    }
    @Test void test54_listEmptyWhenTimeSlotNotUsed() {
        when(clazzRepository.findAllWithFilters(null)).thenReturn(Arrays.asList(createClazzWithTeacher(1L)));
        when(classScheduleRepository.findAll()).thenReturn(Collections.emptyList());
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.listClasses(null, 99L)).isEmpty();
    }
    @Test void test55_listMultipleClassesSameTeacher() {
        when(clazzRepository.findAllWithFilters(1L)).thenReturn(Arrays.asList(createClazzWithTeacher(1L), createClazzWithTeacher(2L), createClazzWithTeacher(3L)));
        when(classScheduleRepository.findAll()).thenReturn(Collections.emptyList());
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.listClasses(1L, null)).hasSize(3);
    }
    @Test void test56_listMultipleClassesSameTimeSlot() {
        Clazz c1 = createClazzWithTeacher(1L);
        Clazz c2 = createClazzWithTeacher(2L);
        ClassSchedule s1 = new ClassSchedule(); s1.setClazz(c1); s1.setTimeSlot(timeSlot);
        ClassSchedule s2 = new ClassSchedule(); s2.setClazz(c2); s2.setTimeSlot(timeSlot);
        when(clazzRepository.findAllWithFilters(null)).thenReturn(Arrays.asList(c1, c2));
        when(classScheduleRepository.findAll()).thenReturn(Arrays.asList(s1, s2));
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.listClasses(null, 1L)).hasSize(2);
    }
    @Test void test57_listLoadsSchedulesCorrectly() {
        when(clazzRepository.findAllWithFilters(null)).thenReturn(Arrays.asList(createClazzWithTeacher(1L)));
        when(classScheduleRepository.findAll()).thenReturn(Collections.emptyList());
        when(classSessionRepository.countByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.countCompletedByClazzIdIn(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt(), anyInt())).thenReturn(new ClassResponse());
        classService.listClasses(null, null);
        verify(classScheduleRepository).findAll();
    }
    @Test void test58_semesterNull() {
        validRequest.setSemesterId(null);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(timeSlotRepository.findById(anyLong())).thenReturn(Optional.of(timeSlot));
        when(clazzRepository.findTeacherConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Collections.emptyList());
        when(clazzRepository.findRoomConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Collections.emptyList());
        when(clazzRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(classScheduleRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt())).thenReturn(new ClassResponse());
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test59_courseNotApproved() {
        course.setStatus(CourseStatus.PENDING);
        validRequest.setCourseId(1L);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        assertThatThrownBy(() -> classService.createClass(validRequest)).hasMessage("Course is not approved");
    }
    @Test void test60_courseApproved() {
        validRequest.setCourseId(1L);
        mockValidCreate();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test61_teacherTeachesViaCollection() {
        Subject other = new Subject(); other.setId(2L);
        teacher.setSubject(other);
        teacher.setSubjects(new HashSet<>(Arrays.asList(other, subject)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test62_saveWithAllFields() {
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getName() != null && c.getSubject() != null && c.getTeacher() != null));
    }
    @Test void test63_saveSchedules() { mockValidCreate(); classService.createClass(validRequest); verify(classScheduleRepository).saveAll(argThat(list -> list.iterator().hasNext())); }
    @Test void test64_saveSessions() { mockValidCreate(); classService.createClass(validRequest); verify(classSessionRepository).saveAll(argThat(list -> list.iterator().hasNext())); }
    @Test void test65_descriptionField() {
        validRequest.setDescription("Test");
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> "Test".equals(c.getDescription())));
    }
    @Test void test66_meetingLinkField() {
        validRequest.setRoomId(null); validRequest.setMaxStudents(50);
        validRequest.setMeetingLink("https://zoom.us/123");
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getMeetingLink() != null));
    }
    @Test void test67_mapperCalled() { mockValidCreate(); classService.createClass(validRequest); verify(classMapper).toResponse(any(), anyList(), eq(30)); }
    @Test void test68_statusDraftOnCreate() {
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getStatus() == ClassStatus.DRAFT));
    }
    @Test void test69_statusDraftDefault() {
        semester.setStartDate(LocalDate.now().minusDays(100));
        semester.setEndDate(LocalDate.now().minusDays(10));
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getStatus() == ClassStatus.DRAFT));
    }
    @Test void test70_statusDraftWithFutureDates() {
        semester.setStartDate(LocalDate.now().plusDays(10));
        semester.setEndDate(LocalDate.now().plusDays(100));
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getStatus() == ClassStatus.DRAFT));
    }
    @Test void test71_statusDraftWhenNoSemester() {
        validRequest.setSemesterId(null);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(timeSlotRepository.findById(anyLong())).thenReturn(Optional.of(timeSlot));
        when(clazzRepository.findTeacherConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Collections.emptyList());
        when(clazzRepository.findRoomConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Collections.emptyList());
        when(clazzRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(classScheduleRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt())).thenReturn(new ClassResponse());
        classService.createClass(validRequest);
        verify(clazzRepository).save(argThat(c -> c.getStatus() == ClassStatus.DRAFT));
    }
    @Test void test72_minimumMaxStudents() { validRequest.setMaxStudents(1); mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test73_largeMaxStudents() { room.setCapacity(200); validRequest.setMaxStudents(200); mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test74_multipleTimeSlotsPerDay() {
        validRequest.setSchedule(Arrays.asList(schedule(6,1L), schedule(6,2L)));
        mockValidCreate();
        assertThat(classService.createClass(validRequest)).isNotNull();
    }
    @Test void test75_conflictCheckParameters() {
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).findTeacherConflictsByDateRange(eq(1L), any(), any(), anySet(), anySet());
    }
    @Test void test76_roomConflictCheckParameters() {
        mockValidCreate();
        classService.createClass(validRequest);
        verify(clazzRepository).findRoomConflictsByDateRange(eq(1L), any(), any(), anySet(), anySet());
    }
    @Test void test77_noConflictDifferentTimeSlot() { mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test78_noConflictDifferentDay() { mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test79_noConflictDifferentDateRange() { mockValidCreate(); assertThat(classService.createClass(validRequest)).isNotNull(); }
    @Test void test80_completeSuccessfulCreation() {
        mockValidCreate();
        ClassResponse response = classService.createClass(validRequest);
        assertThat(response).isNotNull();
        verify(clazzRepository).save(any());
        verify(classScheduleRepository).saveAll(anyList());
        verify(classSessionRepository).saveAll(anyList());
        verify(classMapper).toResponse(any(), anyList(), anyInt());
    }

    private ScheduleItemRequest schedule(int day, Long slotId) {
        ScheduleItemRequest s = new ScheduleItemRequest();
        s.setDayOfWeek(day);
        s.setTimeSlotId(slotId);
        return s;
    }

    private void mockValidCreate() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(timeSlotRepository.findById(anyLong())).thenReturn(Optional.of(timeSlot));
        when(clazzRepository.findTeacherConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Collections.emptyList());
        when(clazzRepository.findRoomConflictsByDateRange(anyLong(), any(), any(), anySet(), anySet())).thenReturn(Collections.emptyList());
        when(clazzRepository.save(any(Clazz.class))).thenAnswer(i -> i.getArgument(0));
        when(classScheduleRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(classSessionRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(classMapper.toResponse(any(), anyList(), anyInt())).thenReturn(new ClassResponse());
    }
}
