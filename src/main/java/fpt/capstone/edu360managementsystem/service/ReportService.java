package fpt.capstone.edu360managementsystem.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.ReportClassPerformanceDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportOverviewDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportRevenueByTimeDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportSubjectRevenueDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportTeacherRevenueDTO;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final ClazzRepository clazzRepository;
    private final TeacherRepository teacherRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    /**
     * Báo cáo tổng quan
     */
    public ReportOverviewDTO getOverview() {
        LocalDateTime now = LocalDateTime.now();

        // Đầu tuần (Thứ 2)
        LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime weekStart = startOfWeek.atStartOfDay();

        // Đầu tháng
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = startOfMonth.atStartOfDay();

        // Đầu ngày
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        // Tháng trước
        LocalDate lastMonthStart = startOfMonth.minusMonths(1);
        LocalDate lastMonthEnd = startOfMonth.minusDays(1);

        // Doanh thu
        Long totalRevenue = paymentRepository.sumPaidAmount();
        Long pendingRevenue = paymentRepository.sumPendingAmount();
        Long monthlyRevenue = paymentRepository.sumPaidAmountBetween(monthStart, now);
        Long weeklyRevenue = paymentRepository.sumPaidAmountBetween(weekStart, now);
        Long todayRevenue = paymentRepository.sumPaidAmountBetween(todayStart, todayEnd);
        Long lastMonthRevenue = paymentRepository.sumPaidAmountBetween(
                lastMonthStart.atStartOfDay(),
                lastMonthEnd.atTime(LocalTime.MAX)
        );

        // Tính % tăng trưởng
        Double growthPercent = 0.0;
        if (lastMonthRevenue != null && lastMonthRevenue > 0) {
            growthPercent = ((monthlyRevenue - lastMonthRevenue) * 100.0) / lastMonthRevenue;
        }

        // Thống kê học sinh - chỉ đếm active
        Long totalStudents = studentRepository.countActive();
        // Note: User/ClassEnrollment không có createdAt, đếm payment mới thay thế (đại diện cho đăng ký mới)
        Long newStudentsThisMonth = paymentRepository.countDistinctStudentsCreatedAfter(monthStart);
        Long activeEnrollments = classEnrollmentRepository.countActiveEnrollments();

        // Thống kê lớp học
        Long totalClasses = clazzRepository.count();
        Long publicClasses = clazzRepository.countByStatus(ClassStatus.PUBLIC);
        Long draftClasses = clazzRepository.countByStatus(ClassStatus.DRAFT);

        // Thống kê giáo viên - chỉ đếm active
        Long totalTeachers = teacherRepository.countActive();
        Long activeTeachers = clazzRepository.countDistinctTeachersWithPublicClasses();

        // Thống kê thanh toán
        Long paidPayments = paymentRepository.countByStatus(PaymentStatus.PAID);
        Long pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING);
        Double paymentSuccessRate = 0.0;
        Long totalPayments = paidPayments + pendingPayments;
        if (totalPayments > 0) {
            paymentSuccessRate = (paidPayments * 100.0) / totalPayments;
        }

        return ReportOverviewDTO.builder()
                .totalRevenue(totalRevenue)
                .pendingRevenue(pendingRevenue)
                .monthlyRevenue(monthlyRevenue)
                .weeklyRevenue(weeklyRevenue)
                .todayRevenue(todayRevenue)
                .lastMonthRevenue(lastMonthRevenue)
                .monthGrowthPercent(Math.round(growthPercent * 100.0) / 100.0)
                .totalStudents(totalStudents)
                .newStudentsThisMonth(newStudentsThisMonth)
                .activeEnrollments(activeEnrollments)
                .totalClasses(totalClasses)
                .publicClasses(publicClasses)
                .draftClasses(draftClasses)
                .totalTeachers(totalTeachers)
                .activeTeachers(activeTeachers)
                .paidPayments(paidPayments)
                .pendingPayments(pendingPayments)
                .paymentSuccessRate(Math.round(paymentSuccessRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Doanh thu theo giáo viên
     */
    public List<ReportTeacherRevenueDTO> getTeacherRevenue() {
        List<Object[]> rawData = paymentRepository.getRevenueByTeacher();
        List<ReportTeacherRevenueDTO> result = new ArrayList<>();

        for (Object[] row : rawData) {
            Long teacherId = (Long) row[0];
            Long teacherUserId = (Long) row[1];
            String teacherName = (String) row[2];
            String teacherEmail = (String) row[3];
            Long totalRevenue = (Long) row[4];
            Long pendingRevenue = (Long) row[5];

            // Đếm số lớp và học sinh của giáo viên
            Integer totalClasses = clazzRepository.countByTeacherIdAndStatus(teacherId, ClassStatus.PUBLIC);
            Integer totalStudents = classEnrollmentRepository.countStudentsByTeacherId(teacherId);
            Integer paidStudents = (int) paymentRepository.countByStatus(PaymentStatus.PAID); // Simplified

            result.add(ReportTeacherRevenueDTO.builder()
                    .teacherId(teacherId)
                    .teacherUserId(teacherUserId)
                    .teacherName(teacherName)
                    .teacherAvatar(null) // User không có avatarUrl
                    .teacherEmail(teacherEmail)
                    .totalRevenue(totalRevenue)
                    .pendingRevenue(pendingRevenue)
                    .totalClasses(totalClasses)
                    .totalStudents(totalStudents)
                    .paidStudents(paidStudents)
                    .build());
        }

        return result;
    }

    /**
     * Doanh thu theo môn học
     */
    public List<ReportSubjectRevenueDTO> getSubjectRevenue() {
        List<Object[]> rawData = paymentRepository.getRevenueBySubject();
        List<ReportSubjectRevenueDTO> result = new ArrayList<>();

        for (Object[] row : rawData) {
            Long subjectId = (Long) row[0];
            String subjectName = (String) row[1];
            Long totalRevenue = (Long) row[2];

            Integer totalClasses = clazzRepository.countBySubjectIdAndStatus(subjectId, ClassStatus.PUBLIC);
            Integer totalStudents = classEnrollmentRepository.countStudentsBySubjectId(subjectId);

            result.add(ReportSubjectRevenueDTO.builder()
                    .subjectId(subjectId)
                    .subjectName(subjectName)
                    .totalRevenue(totalRevenue)
                    .totalClasses(totalClasses)
                    .totalStudents(totalStudents)
                    .build());
        }

        return result;
    }

    /**
     * Doanh thu theo ngày (30 ngày gần nhất)
     */
    public List<ReportRevenueByTimeDTO> getRevenueByDay(Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDate.now().minusDays(days - 1).atStartOfDay();

        List<Object[]> rawData = paymentRepository.getRevenueByDay(startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        // Create map for quick lookup
        var revenueMap = rawData.stream().collect(Collectors.toMap(
                row -> ((LocalDate) row[0]).toString(),
                row -> row
        ));

        List<ReportRevenueByTimeDTO> result = new ArrayList<>();
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        while (!current.isAfter(end)) {
            String key = current.toString();
            Long revenue = 0L;
            Integer count = 0;

            if (revenueMap.containsKey(key)) {
                Object[] row = revenueMap.get(key);
                revenue = (Long) row[1];
                count = ((Long) row[2]).intValue();
            }

            result.add(ReportRevenueByTimeDTO.builder()
                    .date(current)
                    .label(current.format(formatter))
                    .revenue(revenue)
                    .paymentCount(count)
                    .build());

            current = current.plusDays(1);
        }

        return result;
    }

    /**
     * Hiệu suất lớp học
     */
    public List<ReportClassPerformanceDTO> getClassPerformance() {
        List<Object[]> rawData = paymentRepository.getRevenueByClass();
        List<ReportClassPerformanceDTO> result = new ArrayList<>();

        for (Object[] row : rawData) {
            Long classId = (Long) row[0];
            String className = (String) row[1];
            String teacherName = (String) row[2];
            String subjectName = (String) row[3];
            Integer maxStudents = (Integer) row[4];
            Long totalRevenue = (Long) row[5];
            Long pendingRevenue = (Long) row[6];
            Long enrolledStudents = (Long) row[7];
            Long paidStudents = (Long) row[8];
            String meetingLink = (String) row[9];

            Double fillRate = maxStudents > 0 ? (enrolledStudents * 100.0) / maxStudents : 0.0;

            result.add(ReportClassPerformanceDTO.builder()
                    .classId(classId)
                    .className(className)
                    .teacherName(teacherName)
                    .subjectName(subjectName)
                    .maxStudents(maxStudents)
                    .enrolledStudents(enrolledStudents.intValue())
                    .paidStudents(paidStudents.intValue())
                    .fillRate(Math.round(fillRate * 100.0) / 100.0)
                    .totalRevenue(totalRevenue)
                    .pendingRevenue(pendingRevenue)
                    .isOnline(meetingLink != null && !meetingLink.isEmpty())
                    .build());
        }

        return result;
    }

    /**
     * Top giáo viên doanh thu cao nhất
     */
    public ReportTeacherRevenueDTO getTopTeacher() {
        List<ReportTeacherRevenueDTO> teachers = getTeacherRevenue();
        if (teachers.isEmpty()) {
            return null;
        }
        return teachers.get(0); // Đã sort DESC theo revenue
    }
}
