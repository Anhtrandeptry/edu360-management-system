package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.BulkEnrollRequest;
import fpt.capstone.edu360managementsystem.dto.request.EnrollStudentRequest;
import fpt.capstone.edu360managementsystem.dto.response.EnrolledStudentResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    @Autowired private ClazzRepository clazzRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ClassEnrollmentRepository classEnrollmentRepository;
    @Autowired private ClassScheduleRepository classScheduleRepository;
    @Autowired private ClassSessionRepository classSessionRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private NotificationService notificationService;

    /** Chỉ ADMIN hoặc giáo viên chủ lớp được thao tác */
    private void ensureOwnerOrAdmin(Long userId, Clazz clazz, boolean isAdmin) {
        if (isAdmin) return;
        if (!clazz.getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Forbidden: not class owner");
        }
    }

    public List<EnrolledStudentResponse> listStudents(Long classId, Long userId, boolean isAdmin) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        ensureOwnerOrAdmin(userId, clazz, isAdmin);

        return classEnrollmentRepository.findByClazz_Id(classId).stream()
                .map(en -> new EnrolledStudentResponse(
                        en.getStudent().getId(),
                        en.getStudent().getUser().getFullName(),
                        en.getStudent().getUser().getEmail(),
                        en.getStudent().getUser().getPhoneNumber()
                ))
                .toList();
    }

    @Transactional
    public void enrollOne(Long classId, EnrollStudentRequest req, Long userId, boolean isAdmin) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        ensureOwnerOrAdmin(userId, clazz, isAdmin);

        Student student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Capacity
        int current = classEnrollmentRepository.countByClazz_Id(classId);
        if (current >= clazz.getMaxStudents()) {
            throw new RuntimeException("Class is full");
        }
        // Duplicate
        boolean exists = classEnrollmentRepository.existsByClazzAndStudent(clazz, student);
        if (exists) {
            throw new RuntimeException("Student already enrolled in this class");
        }

        // Schedule conflicts in same semester
        var schedules = classScheduleRepository.findByClazz_Id(classId);
        var dows = schedules.stream().map(ClassSchedule::getDayOfWeek).collect(Collectors.toSet());
        var slotIds = schedules.stream().map(s -> s.getTimeSlot().getId()).collect(Collectors.toSet());

        var conflicts = classEnrollmentRepository.findScheduleConflicts(
                student.getId(), clazz.getSemester().getId(), dows, slotIds
        );
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Schedule conflict with other enrolled classes");
        }

        classEnrollmentRepository.save(
                ClassEnrollment.builder().clazz(clazz).student(student).build()
        );
    }

    @Transactional
    public Map<Long, String> enrollBulk(Long classId, BulkEnrollRequest req, Long userId, boolean isAdmin) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        ensureOwnerOrAdmin(userId, clazz, isAdmin);

        var schedules = classScheduleRepository.findByClazz_Id(classId);
        var dows = schedules.stream().map(ClassSchedule::getDayOfWeek).collect(Collectors.toSet());
        var slotIds = schedules.stream().map(s -> s.getTimeSlot().getId()).collect(Collectors.toSet());

        int current = classEnrollmentRepository.countByClazz_Id(classId);
        int remaining = clazz.getMaxStudents() - current;

        Map<Long, String> result = new LinkedHashMap<>();
        for (Long studentId : req.getStudentIds()) {
            if (remaining <= 0) {
                result.put(studentId, "Class is full");
                continue;
            }
            var studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                result.put(studentId, "Student not found");
                continue;
            }
            var student = studentOpt.get();

            if (classEnrollmentRepository.existsByClazzAndStudent(clazz, student)) {
                result.put(studentId, "Already enrolled");
                continue;
            }

            var conflicts = classEnrollmentRepository.findScheduleConflicts(
                    studentId, clazz.getSemester().getId(), dows, slotIds
            );
            if (!conflicts.isEmpty()) {
                result.put(studentId, "Schedule conflict");
                continue;
            }

            classEnrollmentRepository.save(
                    ClassEnrollment.builder().clazz(clazz).student(student).build()
            );
            remaining--;
            result.put(studentId, "OK");
        }
        return result;
    }

    @Transactional
    public void removeOne(Long classId, Long studentId, Long userId, boolean isAdmin) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        ensureOwnerOrAdmin(userId, clazz, isAdmin);

        classEnrollmentRepository.deleteByClazz_IdAndStudent_Id(classId, studentId);
    }


//    @Transactional
//    public void selfEnroll(Long classId, Long userId) {
//        Clazz clazz = clazzRepository.findById(classId)
//                .orElseThrow(() -> new RuntimeException("Class not found"));
//
//        // map user -> student
//        Student student = studentRepository.findByUser_Id(userId)
//                .orElseThrow(() -> new RuntimeException("Student profile not found"));
//
//        // Capacity
//        int current = classEnrollmentRepository.countByClazz_Id(classId);
//        if (current >= clazz.getMaxStudents()) {
//            throw new RuntimeException("Class is full");
//        }
//
//        // Duplicate
//        boolean exists = classEnrollmentRepository.existsByClazzAndStudent(clazz, student);
//        if (exists) {
//            throw new RuntimeException("You are already enrolled in this class");
//        }
//
//        // Schedule conflicts trong cùng học kỳ (chỉ check nếu class có semester)
//        if (clazz.getSemester() != null) {
//            var schedules = classScheduleRepository.findByClazz_Id(classId);
//            var dows = schedules.stream().map(ClassSchedule::getDayOfWeek).collect(java.util.stream.Collectors.toSet());
//            var slotIds = schedules.stream().map(s -> s.getTimeSlot().getId()).collect(java.util.stream.Collectors.toSet());
//
//            var conflicts = classEnrollmentRepository.findScheduleConflicts(
//                    student.getId(), clazz.getSemester().getId(), dows, slotIds
//            );
//            if (!conflicts.isEmpty()) {
//                throw new RuntimeException("Schedule conflict with your other enrolled classes");
//            }
//        }
//
//        classEnrollmentRepository.save(
//                ClassEnrollment.builder().clazz(clazz).student(student).build()
//        );
//    }

    @Transactional
    public void selfEnroll(Long classId, Long userId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // Chỉ cho phép self-enroll khi lớp ở trạng thái PUBLIC
        if (clazz.getStatus() != ClassStatus.PUBLIC) {
            throw new RuntimeException("Lớp chưa được mở đăng ký (không ở trạng thái PUBLIC)");
        }

        // map user -> student
        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        //1. Thanh toán: chỉ enforce khi học phí > 0
        long sessionsCount = classSessionRepository.countByClazz_Id(classId);
        long unitPrice = clazz.getPricePerSession() == null ? 0L : clazz.getPricePerSession();
        long totalFee = unitPrice * sessionsCount;
        
        System.out.println("🔍 [selfEnroll] classId=" + classId + ", sessionsCount=" + sessionsCount + ", pricePerSession=" + unitPrice + ", totalFee=" + totalFee);
        
        if (totalFee > 0) {
            boolean paid = paymentRepository.existsByClazz_IdAndStudent_IdAndStatus(
                    classId, student.getId(), PaymentStatus.PAID
            );
            System.out.println("🔍 [selfEnroll] totalFee > 0, checking payment... paid=" + paid);
            if (!paid) {
                throw new RuntimeException("Bạn chưa thanh toán học phí cho lớp này.");
            }
        } else {
            System.out.println("🔍 [selfEnroll] totalFee = 0, skipping payment check (free class or no sessions)");
        }

        // 2. Capacity
        int current = classEnrollmentRepository.countByClazz_Id(classId);
        if (current >= clazz.getMaxStudents()) {
            throw new RuntimeException("Class is full");
        }

        // 3. Duplicate
        boolean exists = classEnrollmentRepository.existsByClazzAndStudent(clazz, student);
        if (exists) {
            throw new RuntimeException("You are already enrolled in this class");
        }

        // 4. Schedule conflicts trong cùng học kỳ (nếu có semester)
        if (clazz.getSemester() != null) {
            var schedules = classScheduleRepository.findByClazz_Id(classId);
            var dows = schedules.stream().map(ClassSchedule::getDayOfWeek).collect(Collectors.toSet());
            var slotIds = schedules.stream().map(s -> s.getTimeSlot().getId()).collect(Collectors.toSet());

            var conflicts = classEnrollmentRepository.findScheduleConflicts(
                    student.getId(), clazz.getSemester().getId(), dows, slotIds
            );
            if (!conflicts.isEmpty()) {
                throw new RuntimeException("Schedule conflict with your other enrolled classes");
            }
        }

        classEnrollmentRepository.save(
                ClassEnrollment.builder().clazz(clazz).student(student).build()
        );
    }

    /**
     * Enroll học sinh vào lớp sau khi đã thanh toán thành công.
     * Được gọi bởi PaymentService sau khi admin xác nhận hoặc webhook callback.
     * Bỏ qua kiểm tra payment vì đã xác nhận rồi.
     */
    @Transactional
    public void enrollAfterPayment(Long classId, Long studentId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 1. Check duplicate - nếu đã enroll thì bỏ qua (không throw lỗi)
        boolean exists = classEnrollmentRepository.existsByClazzAndStudent(clazz, student);
        if (exists) {
            // Đã enroll rồi, không cần làm gì
            return;
        }

        // 2. Capacity check
        int current = classEnrollmentRepository.countByClazz_Id(classId);
        if (current >= clazz.getMaxStudents()) {
            throw new RuntimeException("Class is full - cannot auto-enroll after payment");
        }

        // 3. Schedule conflicts (nếu có semester)
        if (clazz.getSemester() != null) {
            var schedules = classScheduleRepository.findByClazz_Id(classId);
            var dows = schedules.stream().map(ClassSchedule::getDayOfWeek).collect(Collectors.toSet());
            var slotIds = schedules.stream().map(s -> s.getTimeSlot().getId()).collect(Collectors.toSet());

            var conflicts = classEnrollmentRepository.findScheduleConflicts(
                    student.getId(), clazz.getSemester().getId(), dows, slotIds
            );
            if (!conflicts.isEmpty()) {
                throw new RuntimeException("Schedule conflict - cannot auto-enroll after payment");
            }
        }

        // 4. Enroll
        classEnrollmentRepository.save(
                ClassEnrollment.builder().clazz(clazz).student(student).build()
        );

        // 5. Gửi thông báo cho student
        try {
            notificationService.notifyEnrolledNewClass(
                    student.getUser().getId(),
                    clazz.getName(),
                    clazz.getId()
            );
        } catch (Exception e) {
            // Không throw lỗi nếu gửi notification thất bại
            System.err.println("Failed to send enrollment notification: " + e.getMessage());
        }
    }


}
