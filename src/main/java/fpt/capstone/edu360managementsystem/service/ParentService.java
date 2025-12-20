package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.ParentEmailNotification;
import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.AttendanceRepository;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.ParentEmailNotificationRepository;
import fpt.capstone.edu360managementsystem.repository.ParentRepository;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;

/**
 * Service for parent portal features
 *
 * @author 360edu
 * @version 1.0
 */
@Service
public class ParentService {

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ParentEmailNotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClazzRepository clazzRepository;

    /**
     * Get dashboard data for parent
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(Long userId) {
        Parent parent = getParentByUserId(userId);
        List<Student> children = parent.getChildren();

        long totalClasses = children.stream()
                .mapToLong(child -> classEnrollmentRepository.countByStudent_Id(child.getId()))
                .sum();

        // Count attendance this month
        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        long attendanceCount = 0;
        for (Student child : children) {
            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(child.getId());
            List<Long> classIds = enrollments.stream().map(e -> e.getClazz().getId()).collect(Collectors.toList());

            if (!classIds.isEmpty()) {
                List<ClassSession> sessions = classSessionRepository
                        .findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                                classIds, startOfMonth, endOfMonth);
                List<Long> sessionIds = sessions.stream().map(ClassSession::getId).collect(Collectors.toList());

                if (!sessionIds.isEmpty()) {
                    attendanceCount += attendanceRepository.findBySession_IdInAndStudent_Id(sessionIds, child.getId()).size();
                }
            }
        }

        // Count unread notifications
        long unreadNotifications = notificationRepository.countByParentAndReadFalse(parent);

        Map<String, Object> result = new HashMap<>();
        result.put("childrenCount", children.size());
        result.put("totalClasses", totalClasses);
        result.put("attendanceThisMonth", attendanceCount);
        result.put("unreadNotifications", unreadNotifications);

        return result;
    }

    /**
     * Get list of children
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChildren(Long userId) {
        Parent parent = getParentByUserId(userId);
        List<Student> children = parent.getChildren();

        return children.stream().map(child -> {
            Map<String, Object> childData = new HashMap<>();
            childData.put("id", child.getId());
            childData.put("name", child.getUser().getFullName());
            childData.put("email", child.getUser().getEmail());
            childData.put("dateOfBirth", child.getDob());
            childData.put("grade", child.getGrade());
            childData.put("school", child.getSchool());
            childData.put("avatar", child.getAvatarUrl());
            return childData;
        }).collect(Collectors.toList());
    }

    /**
     * Get attendance data for a child
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChildAttendance(Long userId, Long childId, Integer month, Integer year) {
        Parent parent = getParentByUserId(userId);
        validateChildBelongsToParent(parent, childId);

        // Validate student exists
        studentRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(childId);
        List<Long> classIds = enrollments.stream().map(e -> e.getClazz().getId()).collect(Collectors.toList());

        List<Map<String, Object>> attendanceList = new ArrayList<>();
        int totalCount = 0;
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;

        if (!classIds.isEmpty()) {
            List<ClassSession> sessions = classSessionRepository
                    .findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                            classIds, startDate, endDate);
            List<Long> sessionIds = sessions.stream().map(ClassSession::getId).collect(Collectors.toList());

            if (!sessionIds.isEmpty()) {
                List<Attendance> attendances = attendanceRepository.findBySession_IdInAndStudent_Id(sessionIds, childId);

                Map<Long, ClassSession> sessionMap = sessions.stream()
                        .collect(Collectors.toMap(ClassSession::getId, s -> s));

                for (Attendance att : attendances) {
                    if (att.getStatus() != AttendanceStatus.UNMARKED) {
                        ClassSession session = sessionMap.get(att.getSession().getId());

                        Map<String, Object> attData = new HashMap<>();
                        attData.put("id", att.getId());
                        attData.put("date", session.getDate());
                        attData.put("className", session.getClazz().getName());
                        attData.put("subjectName", session.getClazz().getSubject().getName());
                        attData.put("teacherName", session.getClazz().getTeacher().getUser().getFullName());
                        attData.put("status", att.getStatus().name());
                        attData.put("note", att.getNote());
                        attData.put("startTime", session.getTimeSlot().getStartTime());
                        attData.put("endTime", session.getTimeSlot().getEndTime());

                        attendanceList.add(attData);
                        totalCount++;

                        if (att.getStatus() == AttendanceStatus.PRESENT) {
                            presentCount++;
                        } else if (att.getStatus() == AttendanceStatus.ABSENT) {
                            absentCount++;
                        } else if (att.getStatus() == AttendanceStatus.LATE) {
                            lateCount++;
                        }
                    }
                }
            }
        }

        double attendanceRate = totalCount > 0 ? (double) presentCount / totalCount * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("present", presentCount);
        stats.put("absent", absentCount);
        stats.put("late", lateCount);
        stats.put("rate", Math.round(attendanceRate * 10) / 10.0);

        Map<String, Object> result = new HashMap<>();
        result.put("attendances", attendanceList);
        result.put("stats", stats);

        return result;
    }

    /**
     * Get schedule for a child
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChildSchedule(Long userId, Long childId, String startDate, String endDate) {
        Parent parent = getParentByUserId(userId);
        validateChildBelongsToParent(parent, childId);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(childId);
        List<Long> classIds = enrollments.stream().map(e -> e.getClazz().getId()).collect(Collectors.toList());

        System.out.println("=== getChildSchedule Debug ===");
        System.out.println("Child ID: " + childId);
        System.out.println("Date range: " + startDate + " to " + endDate);
        System.out.println("Enrolled class IDs: " + classIds);

        if (classIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<ClassSession> sessions = classSessionRepository
                .findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                        classIds, start, end);

        System.out.println("Found sessions count: " + sessions.size());

        // Get all session IDs to fetch attendance records
        List<Long> sessionIds = sessions.stream().map(ClassSession::getId).collect(Collectors.toList());

        // Fetch attendance records for this child in these sessions
        Map<Long, Attendance> attendanceBySession = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<Attendance> attendances = attendanceRepository.findBySession_IdInAndStudent_Id(sessionIds, childId);
            for (Attendance att : attendances) {
                attendanceBySession.put(att.getSession().getId(), att);
            }
        }

        return sessions.stream().map(session -> {
            Map<String, Object> scheduleItem = new HashMap<>();
            scheduleItem.put("id", session.getId());
            scheduleItem.put("date", session.getDate());
            scheduleItem.put("dayOfWeek", session.getDayOfWeek());
            scheduleItem.put("startTime", session.getTimeSlot().getStartTime());
            scheduleItem.put("endTime", session.getTimeSlot().getEndTime());
            scheduleItem.put("className", session.getClazz().getName());
            scheduleItem.put("subjectName", session.getClazz().getSubject().getName());
            scheduleItem.put("teacherName", session.getClazz().getTeacher().getUser().getFullName());
            scheduleItem.put("room", session.getRoom() != null ? session.getRoom().getName() : null);
            scheduleItem.put("status", session.getStatus().name());

            // Add attendance status for this child
            Attendance attendance = attendanceBySession.get(session.getId());
            if (attendance != null && attendance.getStatus() != AttendanceStatus.UNMARKED) {
                scheduleItem.put("attendanceStatus", attendance.getStatus().name());
                scheduleItem.put("attendanceNote", attendance.getNote());
            } else {
                scheduleItem.put("attendanceStatus", "UNMARKED");
                scheduleItem.put("attendanceNote", null);
            }

            return scheduleItem;
        }).collect(Collectors.toList());
    }

    /**
     * Get classes for a child
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChildClasses(Long userId, Long childId) {
        Parent parent = getParentByUserId(userId);
        validateChildBelongsToParent(parent, childId);

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(childId);

        return enrollments.stream().map(enrollment -> {
            Clazz clazz = enrollment.getClazz();
            long totalSessions = classSessionRepository.countByClazz_Id(clazz.getId());
            long completedSessions = classSessionRepository.countCompletedByClazzId(clazz.getId());

            Map<String, Object> classData = new HashMap<>();
            classData.put("id", clazz.getId());
            classData.put("name", clazz.getName());
            classData.put("subject", clazz.getSubject().getName());
            classData.put("description", clazz.getDescription());
            classData.put("startDate", clazz.getStartDate());
            classData.put("endDate", clazz.getEndDate());
            classData.put("status", clazz.getStatus().name());
            classData.put("totalSessions", totalSessions);
            classData.put("completedSessions", completedSessions);

            Map<String, Object> teacher = new HashMap<>();
            teacher.put("id", clazz.getTeacher().getId());
            teacher.put("name", clazz.getTeacher().getUser().getFullName());
            teacher.put("email", clazz.getTeacher().getUser().getEmail());
            teacher.put("phone", clazz.getTeacher().getUser().getPhoneNumber());
            classData.put("teacher", teacher);

            // Get schedule from class sessions
            List<ClassSession> sessions = classSessionRepository
                    .findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                            Collections.singletonList(clazz.getId()),
                            clazz.getStartDate(),
                            clazz.getEndDate());

            // Group sessions by day of week and timeslot to get unique schedules
            Map<String, Map<String, String>> uniqueSchedules = new HashMap<>();
            for (ClassSession session : sessions) {
                String key = session.getDayOfWeek() + "_"
                        + session.getTimeSlot().getStartTime() + "_"
                        + session.getTimeSlot().getEndTime();
                if (!uniqueSchedules.containsKey(key)) {
                    Map<String, String> scheduleItem = new HashMap<>();
                    scheduleItem.put("dayOfWeek", String.valueOf(session.getDayOfWeek()));
                    scheduleItem.put("startTime", session.getTimeSlot().getStartTime().toString());
                    scheduleItem.put("endTime", session.getTimeSlot().getEndTime().toString());
                    scheduleItem.put("room", session.getRoom() != null ? session.getRoom().getName() : "N/A");
                    uniqueSchedules.put(key, scheduleItem);
                }
            }

            List<Map<String, String>> schedule = new ArrayList<>(uniqueSchedules.values());
            classData.put("schedule", schedule);

            return classData;
        }).collect(Collectors.toList());
    }

    /**
     * Get class detail
     */
    /**
     * Get class detail
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getClassDetail(Long userId, Long classId) {
        Parent parent = getParentByUserId(userId);

        // Verify parent has a child in this class
        List<Student> children = parent.getChildren();
        boolean hasAccess = children.stream().anyMatch(child
                -> classEnrollmentRepository.findByStudent_Id(child.getId()).stream()
                        .anyMatch(e -> e.getClazz().getId().equals(classId))
        );

        if (!hasAccess) {
            throw new RuntimeException("Bạn không có quyền xem lớp này");
        }

        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        long totalSessions = classSessionRepository.countByClazz_Id(classId);
        long completedSessions = classSessionRepository.countCompletedByClazzId(classId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", clazz.getId());
        result.put("name", clazz.getName());
        result.put("subject", clazz.getSubject().getName());
        result.put("description", clazz.getDescription());
        result.put("startDate", clazz.getStartDate());
        result.put("endDate", clazz.getEndDate());
        result.put("status", clazz.getStatus().name());
        result.put("totalSessions", totalSessions);
        result.put("completedSessions", completedSessions);

        Map<String, Object> teacher = new HashMap<>();
        teacher.put("id", clazz.getTeacher().getId());
        teacher.put("name", clazz.getTeacher().getUser().getFullName());
        teacher.put("email", clazz.getTeacher().getUser().getEmail());
        teacher.put("phone", clazz.getTeacher().getUser().getPhoneNumber());
        result.put("teacher", teacher);

        // Get sessions
        List<ClassSession> sessions = classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                Collections.singletonList(classId),
                clazz.getStartDate(),
                clazz.getEndDate());

        List<Map<String, Object>> sessionList = sessions.stream().map(session -> {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("id", session.getId());
            sessionData.put("date", session.getDate());
            sessionData.put("title", session.getLessonContent() != null ? session.getLessonContent() : "Buổi học");
            sessionData.put("content", session.getLessonContent());
            sessionData.put("startTime", session.getTimeSlot().getStartTime().toString());
            sessionData.put("endTime", session.getTimeSlot().getEndTime().toString());
            sessionData.put("status", session.getStatus().name());

            // Get materials for this session - TODO: implement when LessonMaterial relationship exists
            sessionData.put("materials", new ArrayList<>());

            return sessionData;
        }).collect(Collectors.toList());

        result.put("sessions", sessionList);
        result.put("materials", new ArrayList<>()); // TODO: implement class-level materials

        return result;
    }

    /**
     * Get session detail with attendance
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionDetail(Long userId, Long sessionId) {
        Parent parent = getParentByUserId(userId);

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học"));

        // Verify parent has a child in this class
        List<Student> children = parent.getChildren();
        Student enrolledChild = children.stream()
                .filter(child -> classEnrollmentRepository.findByStudent_Id(child.getId()).stream()
                .anyMatch(e -> e.getClazz().getId().equals(session.getClazz().getId())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền xem buổi học này"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", session.getId());
        result.put("date", session.getDate());
        result.put("content", session.getLessonContent());
        result.put("startTime", session.getTimeSlot().getStartTime().toString());
        result.put("endTime", session.getTimeSlot().getEndTime().toString());
        result.put("status", session.getStatus().name());

        // Get attendance for the child
        java.util.Optional<Attendance> attendanceOpt = attendanceRepository.findBySessionAndStudent(session, enrolledChild);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("studentName", enrolledChild.getUser().getFullName());
            attendanceData.put("status", attendance.getStatus().name());
            attendanceData.put("note", attendance.getNote());
            result.put("attendance", attendanceData);
        } else {
            // No attendance record yet
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("studentName", enrolledChild.getUser().getFullName());
            attendanceData.put("status", "PENDING");
            attendanceData.put("note", "Chưa điểm danh");
            result.put("attendance", attendanceData);
        }

        return result;
    }

    /**
     * Get notifications for parent
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotifications(Long userId, String filter, Long childId) {
        Parent parent = getParentByUserId(userId);

        List<ParentEmailNotification> notifications;

        if (childId != null) {
            validateChildBelongsToParent(parent, childId);
            Student student = studentRepository.findById(childId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

            if ("read".equals(filter)) {
                notifications = notificationRepository.findByParentAndStudentAndReadTrueOrderByCreatedAtDesc(parent, student);
            } else if ("unread".equals(filter)) {
                notifications = notificationRepository.findByParentAndStudentAndReadFalseOrderByCreatedAtDesc(parent, student);
            } else {
                notifications = notificationRepository.findByParentAndStudentOrderByCreatedAtDesc(parent, student);
            }
        } else {
            if ("read".equals(filter)) {
                notifications = notificationRepository.findByParentAndReadTrueOrderByCreatedAtDesc(parent);
            } else if ("unread".equals(filter)) {
                notifications = notificationRepository.findByParentAndReadFalseOrderByCreatedAtDesc(parent);
            } else {
                notifications = notificationRepository.findByParentOrderByCreatedAtDesc(parent);
            }
        }

        return notifications.stream().map(notif -> {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("id", notif.getId());
            notifData.put("title", notif.getSubject());
            notifData.put("message", notif.getBody());
            notifData.put("type", determineNotificationType(notif.getSubject()));
            notifData.put("date", notif.getCreatedAt());
            notifData.put("read", notif.isRead());
            notifData.put("childName", notif.getStudent().getUser().getFullName());
            return notifData;
        }).collect(Collectors.toList());
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markNotificationAsRead(Long userId, Long notificationId) {
        Parent parent = getParentByUserId(userId);
        ParentEmailNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (!notification.getParent().getId().equals(parent.getId())) {
            throw new RuntimeException("Bạn không có quyền đánh dấu thông báo này");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        Parent parent = getParentByUserId(userId);
        List<ParentEmailNotification> unreadNotifications
                = notificationRepository.findByParentAndReadFalseOrderByCreatedAtDesc(parent);

        unreadNotifications.forEach(notif -> notif.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Get payment history
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentHistory(Long userId, Long childId, String status) {
        Parent parent = getParentByUserId(userId);
        List<Student> children = childId != null
                ? Collections.singletonList(studentRepository.findById(childId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh")))
                : parent.getChildren();

        List<Payment> allPayments = new ArrayList<>();
        for (Student child : children) {
            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(child.getId());
            for (ClassEnrollment enrollment : enrollments) {
                paymentRepository.findByClazz_IdAndStudent_Id(enrollment.getClazz().getId(), child.getId())
                        .ifPresent(allPayments::add);
            }
        }

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
            allPayments = allPayments.stream()
                    .filter(p -> p.getStatus() == paymentStatus)
                    .collect(Collectors.toList());
        }

        // Calculate stats
        long total = allPayments.size();
        long completed = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.PAID).count();
        long pending = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count();
        long totalAmount = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .mapToLong(Payment::getAmount)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("pending", pending);
        stats.put("totalAmount", totalAmount);

        List<Map<String, Object>> paymentList = allPayments.stream().map(payment -> {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("id", payment.getId());
            paymentData.put("className", payment.getClazz().getName());
            paymentData.put("childName", payment.getStudent().getUser().getFullName());
            paymentData.put("amount", payment.getAmount());
            paymentData.put("status", payment.getStatus().name());
            paymentData.put("createdAt", payment.getCreatedAt());
            paymentData.put("paidAt", payment.getPaidAt());
            paymentData.put("content", payment.getContent());
            return paymentData;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("payments", paymentList);
        result.put("stats", stats);

        return result;
    }

    /**
     * Get parent profile
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Long userId) {
        Parent parent = getParentByUserId(userId);
        User user = parent.getUser();

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", parent.getId());
        profile.put("name", user.getFullName());
        profile.put("email", user.getEmail());
        profile.put("phone", parent.getPhone() != null ? parent.getPhone() : user.getPhoneNumber());
        profile.put("address", parent.getAddress());
        profile.put("occupation", parent.getOccupation());
        profile.put("avatar", null);

        List<Map<String, Object>> childrenData = parent.getChildren().stream().map(child -> {
            Map<String, Object> childData = new HashMap<>();
            childData.put("id", child.getId());
            childData.put("name", child.getUser().getFullName());
            childData.put("dateOfBirth", child.getDob());
            childData.put("grade", child.getGrade());
            childData.put("school", child.getSchool());
            return childData;
        }).collect(Collectors.toList());

        profile.put("children", childrenData);

        return profile;
    }

    /**
     * Update parent profile
     */
    @Transactional
    public Map<String, Object> updateProfile(Long userId, Map<String, Object> profileData) {
        Parent parent = getParentByUserId(userId);
        User user = parent.getUser();

        if (profileData.containsKey("name")) {
            user.setFullName((String) profileData.get("name"));
        }
        if (profileData.containsKey("phone")) {
            parent.setPhone((String) profileData.get("phone"));
        }
        if (profileData.containsKey("address")) {
            parent.setAddress((String) profileData.get("address"));
        }
        if (profileData.containsKey("occupation")) {
            parent.setOccupation((String) profileData.get("occupation"));
        }

        userRepository.save(user);
        parentRepository.save(parent);

        return getProfile(userId);
    }

    // Helper methods
    private Parent getParentByUserId(Long userId) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return parentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin phụ huynh"));
    }

    private void validateChildBelongsToParent(Parent parent, Long childId) {
        boolean belongs = parent.getChildren().stream()
                .anyMatch(child -> child.getId().equals(childId));

        if (!belongs) {
            throw new RuntimeException("Học sinh không thuộc quyền quản lý của bạn");
        }
    }

    private String determineNotificationType(String subject) {
        if (subject.contains("điểm danh") || subject.contains("vắng")) {
            return "ATTENDANCE";
        } else if (subject.contains("đăng ký") || subject.contains("lớp học")) {
            return "ENROLLMENT";
        } else if (subject.contains("thanh toán")) {
            return "PAYMENT";
        } else {
            return "GENERAL";
        }
    }
}
