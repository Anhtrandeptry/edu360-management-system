package fpt.capstone.edu360managementsystem.testbuilder;

import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Test Data Builder for creating test entities
 */
public class TestDataBuilder {

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static TeacherBuilder teacher() {
        return new TeacherBuilder();
    }

    public static StudentBuilder student() {
        return new StudentBuilder();
    }

    public static ClazzBuilder clazz() {
        return new ClazzBuilder();
    }

    public static PaymentBuilder payment() {
        return new PaymentBuilder();
    }

    public static ClassEnrollmentBuilder enrollment() {
        return new ClassEnrollmentBuilder();
    }

    public static ClassSessionBuilder session() {
        return new ClassSessionBuilder();
    }

    public static SubjectBuilder subject() {
        return new SubjectBuilder();
    }

    public static RoomBuilder room() {
        return new RoomBuilder();
    }

    public static TimeSlotBuilder timeSlot() {
        return new TimeSlotBuilder();
    }

    public static CourseBuilder course() {
        return new CourseBuilder();
    }

    public static SemesterBuilder semester() {
        return new SemesterBuilder();
    }

    // ==================== BUILDERS ====================

    public static class UserBuilder {
        private Long id = 1L;
        private String username = "testuser";
        private String email = "test@example.com";
        private String fullName = "Test User";
        private String phoneNumber = "0123456789";
        private Boolean active = true;
        private Set<Role> roles = new HashSet<>();

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UserBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public UserBuilder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public User build() {
            User user = new User();
            user.setId(id);
            user.setUsername(username);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhoneNumber(phoneNumber);
            user.setActive(active);
            user.setRoles(roles);
            return user;
        }
    }

    public static class TeacherBuilder {
        private Long id = 1L;
        private User user;
        private Subject subject;
        private String bio = "Test bio";
        private String workplace = "Test School";

        public TeacherBuilder() {
            this.user = TestDataBuilder.user().id(1L).fullName("Teacher User").build();
            this.subject = TestDataBuilder.subject().build();
        }

        public TeacherBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TeacherBuilder user(User user) {
            this.user = user;
            return this;
        }

        public TeacherBuilder subject(Subject subject) {
            this.subject = subject;
            return this;
        }

        public TeacherBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public Teacher build() {
            Teacher teacher = new Teacher();
            teacher.setId(id);
            teacher.setUser(user);
            teacher.setSubject(subject);
            teacher.setBio(bio);
            teacher.setWorkplace(workplace);
            return teacher;
        }
    }

    public static class StudentBuilder {
        private Long id = 1L;
        private User user;

        public StudentBuilder() {
            this.user = TestDataBuilder.user().id(2L).fullName("Student User").build();
        }

        public StudentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public StudentBuilder user(User user) {
            this.user = user;
            return this;
        }

        public Student build() {
            Student student = new Student();
            student.setId(id);
            student.setUser(user);
            return student;
        }
    }

    public static class ClazzBuilder {
        private Long id = 1L;
        private String name = "Test Class";
        private Subject subject;
        private Teacher teacher;
        private Room room;
        private LocalDate startDate = LocalDate.now().plusDays(7);
        private LocalDate endDate = LocalDate.now().plusDays(37);
        private Integer maxStudents = 30;
        private Long pricePerSession = 100000L;
        private ClassStatus status = ClassStatus.PUBLIC;
        private Course course = null;
        private Semester semester = null;

        public ClazzBuilder() {
            this.subject = TestDataBuilder.subject().build();
            this.teacher = TestDataBuilder.teacher().build();
            this.room = TestDataBuilder.room().build();
        }

        public ClazzBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ClazzBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ClazzBuilder subject(Subject subject) {
            this.subject = subject;
            return this;
        }

        public ClazzBuilder teacher(Teacher teacher) {
            this.teacher = teacher;
            return this;
        }

        public ClazzBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public ClazzBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public ClazzBuilder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public ClazzBuilder maxStudents(Integer maxStudents) {
            this.maxStudents = maxStudents;
            return this;
        }

        public ClazzBuilder pricePerSession(Long pricePerSession) {
            this.pricePerSession = pricePerSession;
            return this;
        }

        public ClazzBuilder status(ClassStatus status) {
            this.status = status;
            return this;
        }

        public ClazzBuilder course(Course course) {
            this.course = course;
            return this;
        }

        public ClazzBuilder semester(Semester semester) {
            this.semester = semester;
            return this;
        }

        public Clazz build() {
            return Clazz.builder()
                    .id(id)
                    .name(name)
                    .subject(subject)
                    .teacher(teacher)
                    .room(room)
                    .startDate(startDate)
                    .endDate(endDate)
                    .maxStudents(maxStudents)
                    .pricePerSession(pricePerSession)
                    .status(status)
                    .course(course)
                    .semester(semester)
                    .build();
        }
    }

    public static class PaymentBuilder {
        private Long id = 1L;
        private Clazz clazz;
        private Student student;
        private Long amount = 1000000L;
        private String content = "Test payment content #PAY-1-1-123456";
        private String orderCode = "PAY-1-1-123456";
        private PaymentStatus status = PaymentStatus.PENDING;
        private String bankTransactionId = null;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime paidAt = null;

        public PaymentBuilder() {
            this.clazz = TestDataBuilder.clazz().build();
            this.student = TestDataBuilder.student().build();
        }

        public PaymentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentBuilder clazz(Clazz clazz) {
            this.clazz = clazz;
            return this;
        }

        public PaymentBuilder student(Student student) {
            this.student = student;
            return this;
        }

        public PaymentBuilder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PaymentBuilder orderCode(String orderCode) {
            this.orderCode = orderCode;
            return this;
        }

        public PaymentBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder bankTransactionId(String bankTransactionId) {
            this.bankTransactionId = bankTransactionId;
            return this;
        }

        public PaymentBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public Payment build() {
            return Payment.builder()
                    .id(id)
                    .clazz(clazz)
                    .student(student)
                    .amount(amount)
                    .content(content)
                    .orderCode(orderCode)
                    .status(status)
                    .bankTransactionId(bankTransactionId)
                    .createdAt(createdAt)
                    .paidAt(paidAt)
                    .build();
        }
    }

    public static class ClassEnrollmentBuilder {
        private Long id = 1L;
        private Clazz clazz;
        private Student student;

        public ClassEnrollmentBuilder() {
            this.clazz = TestDataBuilder.clazz().build();
            this.student = TestDataBuilder.student().build();
        }

        public ClassEnrollmentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ClassEnrollmentBuilder clazz(Clazz clazz) {
            this.clazz = clazz;
            return this;
        }

        public ClassEnrollmentBuilder student(Student student) {
            this.student = student;
            return this;
        }

        public ClassEnrollment build() {
            return ClassEnrollment.builder()
                    .id(id)
                    .clazz(clazz)
                    .student(student)
                    .build();
        }
    }

    public static class ClassSessionBuilder {
        private Long id = 1L;
        private Clazz clazz;
        private LocalDate date = LocalDate.now().plusDays(7);
        private Integer dayOfWeek = 2;
        private TimeSlot timeSlot;
        private Room room;
        private SessionStatus status = SessionStatus.PLANNED;

        public ClassSessionBuilder() {
            this.clazz = TestDataBuilder.clazz().build();
            this.timeSlot = TestDataBuilder.timeSlot().build();
            this.room = TestDataBuilder.room().build();
        }

        public ClassSessionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ClassSessionBuilder clazz(Clazz clazz) {
            this.clazz = clazz;
            return this;
        }

        public ClassSessionBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public ClassSessionBuilder timeSlot(TimeSlot timeSlot) {
            this.timeSlot = timeSlot;
            return this;
        }

        public ClassSession build() {
            return ClassSession.builder()
                    .id(id)
                    .clazz(clazz)
                    .date(date)
                    .dayOfWeek(dayOfWeek)
                    .timeSlot(timeSlot)
                    .room(room)
                    .status(status)
                    .build();
        }
    }

    public static class SubjectBuilder {
        private Long id = 1L;
        private String name = "Mathematics";
        private SubjectStatus status = SubjectStatus.AVAILABLE;

        public SubjectBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SubjectBuilder name(String name) {
            this.name = name;
            return this;
        }

        public Subject build() {
            Subject subject = new Subject();
            subject.setId(id);
            subject.setName(name);
            subject.setStatus(status);
            return subject;
        }
    }

    public static class RoomBuilder {
        private Long id = 1L;
        private String name = "Room A101";
        private Integer capacity = 30;
        private RoomStatus status = RoomStatus.AVAILABLE;

        public RoomBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RoomBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RoomBuilder capacity(Integer capacity) {
            this.capacity = capacity;
            return this;
        }

        public Room build() {
            Room room = new Room();
            room.setId(id);
            room.setName(name);
            room.setCapacity(capacity);
            room.setStatus(status);
            return room;
        }
    }

    public static class TimeSlotBuilder {
        private Long id = 1L;
        private LocalTime startTime = LocalTime.of(8, 0);
        private LocalTime endTime = LocalTime.of(10, 0);

        public TimeSlotBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TimeSlot build() {
            TimeSlot slot = new TimeSlot();
            slot.setId(id);
            slot.setStartTime(java.sql.Time.valueOf(startTime));
            slot.setEndTime(java.sql.Time.valueOf(endTime));
            return slot;
        }
    }

    public static class CourseBuilder {
        private Long id = 1L;
        private Subject subject = subject().build();
        private String title = "Test Course";
        private String description = "Test Description";
        private CourseStatus status = CourseStatus.APPROVED;

        public CourseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CourseBuilder status(CourseStatus status) {
            this.status = status;
            return this;
        }

        public Course build() {
            return Course.builder()
                    .id(id)
                    .subject(subject)
                    .title(title)
                    .description(description)
                    .status(status)
                    .build();
        }
    }

    public static class SemesterBuilder {
        private Long id = 1L;
        private String name = "Fall 2024";
        private LocalDate startDate = LocalDate.now();
        private LocalDate endDate = LocalDate.now().plusMonths(4);

        public SemesterBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public Semester build() {
            Semester semester = new Semester();
            semester.setId(id);
            semester.setName(name);
            semester.setStartDate(startDate);
            semester.setEndDate(endDate);
            return semester;
        }
    }
}
