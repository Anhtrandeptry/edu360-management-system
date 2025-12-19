package fpt.capstone.edu360managementsystem.service;

import java.sql.Time;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.SessionChapter;
import fpt.capstone.edu360managementsystem.entity.SessionLesson;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.AttendanceRepository;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.SessionChapterRepository;
import fpt.capstone.edu360managementsystem.repository.SessionLessonRepository;

@Service
public class ParentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ParentNotificationService.class);

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SessionChapterRepository sessionChapterRepository;

    @Autowired
    private SessionLessonRepository sessionLessonRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    /**
     * Gửi thông báo cho phụ huynh khi giáo viên click nút "Gửi thông báo"
     *
     * @param sessionId ID của session
     * @param userId ID của user (giáo viên) để verify ownership
     * @return Số lượng email đã gửi thành công
     */
    @Transactional
    public int sendParentNotificationManual(Long sessionId, Long userId) {
        log.info("[ParentNotification] Teacher {} manually sending notification for session {}", userId, sessionId);

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học"));

        // Verify teacher ownership
        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền gửi thông báo cho buổi học này");
        }

        return sendNotificationsForSession(session);
    }

    /**
     * Gửi thông báo cho phụ huynh theo classId và date (khi không có sessionId)
     */
    @Transactional
    public int sendParentNotificationByClassAndDate(Long classId, String dateStr, Long slotId, Long userId) {
        LocalDate date = LocalDate.parse(dateStr);

        ClassSession session;
        if (slotId != null) {
            session = classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(classId, date, slotId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học"));
        } else {
            List<ClassSession> sessions = classSessionRepository.findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            if (sessions.isEmpty()) {
                throw new RuntimeException("Không tìm thấy buổi học cho ngày " + dateStr);
            }
            session = sessions.get(0);
        }

        // Verify teacher ownership
        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền gửi thông báo cho buổi học này");
        }

        return sendNotificationsForSession(session);
    }

    /**
     * Gửi thông báo cho tất cả phụ huynh trong session
     */
    private int sendNotificationsForSession(ClassSession session) {
        LocalDate sessionDate = session.getDate();

        // Lấy danh sách học sinh enrolled trong lớp
        var enrollments = classEnrollmentRepository.findByClazz_Id(session.getClazz().getId());
        if (enrollments.isEmpty()) {
            log.info("No students enrolled in class {}", session.getClazz().getId());
            return 0;
        }

        // Lấy danh sách attendance của session
        List<Attendance> attendances = attendanceRepository.findBySession_Id(session.getId());
        Map<Long, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a1, a2) -> a1));

        int sentCount = 0;
        int failCount = 0;

        for (var enrollment : enrollments) {
            Student student = enrollment.getStudent();
            if (student == null || student.getParent() == null) {
                continue;
            }

            String parentEmail = getParentEmail(student);
            if (parentEmail == null || parentEmail.isBlank()) {
                log.warn("Student {} has no parent email", student.getUser().getFullName());
                continue;
            }

            Attendance attendance = attendanceMap.get(student.getId());

            try {
                sendNotificationEmail(student, session, attendance, sessionDate);
                sentCount++;
                log.info("Email sent to {} for student {}", parentEmail, student.getUser().getFullName());
            } catch (Exception e) {
                failCount++;
                log.error("Failed to send email to {}: {}", parentEmail, e.getMessage());
            }
        }

        log.info("[ParentNotification] Completed: {} sent, {} failed for session {}", sentCount, failCount, session.getId());
        return sentCount;
    }

    /**
     * Gửi email thông báo cho phụ huynh
     */
    private void sendNotificationEmail(Student student, ClassSession session, Attendance attendance, LocalDate date) {
        String parentEmail = getParentEmail(student);
        if (parentEmail == null) {
            return;
        }

        String studentName = student.getUser().getFullName();
        String parentName = student.getParent().getUser().getFullName();
        String className = session.getClazz().getName();
        String subjectName = session.getClazz().getSubject().getName();
        String teacherName = session.getClazz().getTeacher().getUser().getFullName();
        String timeSlot = formatTime(session.getTimeSlot().getStartTime()) + " - " + formatTime(session.getTimeSlot().getEndTime());
        String dateFormatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Attendance info
        String attendanceStatus = "Chưa điểm danh";
        String attendanceColor = "#6b7280";
        String attendanceIcon = "⏳";
        String attendanceNote = "";

        if (attendance != null) {
            switch (attendance.getStatus()) {
                case PRESENT -> {
                    attendanceStatus = "Có mặt";
                    attendanceColor = "#10b981";
                    attendanceIcon = "✅";
                }
                case ABSENT -> {
                    attendanceStatus = "Vắng mặt";
                    attendanceColor = "#ef4444";
                    attendanceIcon = "❌";
                }
                case LATE -> {
                    attendanceStatus = "Đi muộn";
                    attendanceColor = "#f59e0b";
                    attendanceIcon = "⚠️";
                }
                case UNMARKED -> {
                    attendanceStatus = "Chưa điểm danh";
                    attendanceColor = "#6b7280";
                    attendanceIcon = "⏳";
                }
            }
            if (attendance.getNote() != null && !attendance.getNote().isBlank()) {
                attendanceNote = attendance.getNote();
            }
        }

        // Lesson content
        List<SessionChapter> chapters = sessionChapterRepository.findBySession_Id(session.getId());
        List<SessionLesson> lessons = sessionLessonRepository.findBySession_Id(session.getId());

        StringBuilder lessonContentHtml = new StringBuilder();
        if (!chapters.isEmpty() || !lessons.isEmpty()) {
            for (SessionChapter sc : chapters) {
                lessonContentHtml.append("<div style=\"display: flex; align-items: center; margin-bottom: 8px;\">")
                        .append("<span style=\"background: #dbeafe; color: #1d4ed8; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 500;\">📚 Chương: ")
                        .append(escapeHtml(sc.getChapter().getTitle()))
                        .append("</span></div>");
            }
            for (SessionLesson sl : lessons) {
                lessonContentHtml.append("<div style=\"display: flex; align-items: center; margin-bottom: 8px;\">")
                        .append("<span style=\"background: #fef3c7; color: #b45309; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 500;\">📖 Bài: ")
                        .append(escapeHtml(sl.getLesson().getTitle()))
                        .append("</span></div>");
            }
        }

        String detailContent = "";
        if (session.getLessonContent() != null && !session.getLessonContent().isBlank()) {
            detailContent = escapeHtml(session.getLessonContent()).replace("\n", "<br>");
        }

        String htmlContent = buildHtmlTemplate(
                parentName, studentName, dateFormatted, className, subjectName,
                teacherName, timeSlot, attendanceStatus, attendanceColor, attendanceIcon,
                attendanceNote, lessonContentHtml.toString(), detailContent
        );

        String subject = "[360EDU] Báo cáo buổi học - " + studentName + " - " + dateFormatted;

        emailService.sendHtmlMessage(parentEmail, subject, htmlContent);
    }

    private String buildHtmlTemplate(String parentName, String studentName, String date,
            String className, String subjectName, String teacherName, String timeSlot,
            String attendanceStatus, String attendanceColor, String attendanceIcon,
            String attendanceNote, String lessonContentHtml, String detailContent) {

        String noteHtml = attendanceNote.isEmpty() ? ""
                : "<p style=\"color: #92400e; font-size: 14px; margin: 16px 0 0 0; text-align: center;\">📌 Ghi chú: " + escapeHtml(attendanceNote) + "</p>";

        String lessonHtml = lessonContentHtml.isEmpty()
                ? "<p style=\"color: #6b7280; font-style: italic;\">Chưa cập nhật chương/bài học</p>"
                : lessonContentHtml;

        String detailHtml = detailContent.isEmpty() ? ""
                : "<div style=\"background-color: #ffffff; border-radius: 8px; padding: 16px; margin-top: 16px;\"><p style=\"color: #374151; font-size: 14px; margin: 0; line-height: 1.7;\"><strong>Chi tiết:</strong><br>" + detailContent + "</p></div>";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"vi\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("</head>");
        html.append("<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f3f4f6;\">");
        html.append("<table role=\"presentation\" style=\"width: 100%; border-collapse: collapse;\">");
        html.append("<tr>");
        html.append("<td align=\"center\" style=\"padding: 40px 20px;\">");
        html.append("<table role=\"presentation\" style=\"width: 100%; max-width: 600px; border-collapse: collapse;\">");

        // Header
        html.append("<tr>");
        html.append("<td style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 30px; border-radius: 16px 16px 0 0; text-align: center;\">");
        html.append("<div style=\"font-size: 48px; margin-bottom: 16px;\">🎓</div>");
        html.append("<h1 style=\"color: #ffffff; margin: 0; font-size: 28px; font-weight: 700;\">360EDU</h1>");
        html.append("<p style=\"color: rgba(255,255,255,0.9); margin: 8px 0 0 0; font-size: 14px;\">Hệ thống Quản lý Giáo dục</p>");
        html.append("</td>");
        html.append("</tr>");

        // Main Content
        html.append("<tr>");
        html.append("<td style=\"background-color: #ffffff; padding: 40px 30px;\">");

        // Greeting
        html.append("<p style=\"color: #374151; font-size: 16px; margin: 0 0 24px 0; line-height: 1.6;\">");
        html.append("Kính gửi Phụ huynh <strong style=\"color: #4f46e5;\">").append(escapeHtml(parentName)).append("</strong>,");
        html.append("</p>");

        html.append("<p style=\"color: #6b7280; font-size: 15px; margin: 0 0 32px 0; line-height: 1.7;\">");
        html.append("Hệ thống 360EDU xin gửi đến Quý Phụ huynh thông tin buổi học của con em:");
        html.append("</p>");

        // Student Card
        html.append("<div style=\"background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); border-radius: 12px; padding: 24px; margin-bottom: 24px; border-left: 4px solid #0ea5e9;\">");
        html.append("<table style=\"width: 100%; border-collapse: collapse;\"><tr>");
        html.append("<td style=\"width: 72px; vertical-align: middle;\">");
        html.append("<div style=\"width: 56px; height: 56px; background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%); border-radius: 50%; text-align: center; line-height: 56px;\">");
        html.append("<span style=\"font-size: 28px;\">👨‍🎓</span>");
        html.append("</div>");
        html.append("</td>");
        html.append("<td style=\"vertical-align: middle;\">");
        html.append("<p style=\"margin: 0; color: #0c4a6e; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; font-weight: 600;\">Học sinh</p>");
        html.append("<p style=\"margin: 4px 0 0 0; color: #0369a1; font-size: 20px; font-weight: 700;\">").append(escapeHtml(studentName)).append("</p>");
        html.append("</td>");
        html.append("</tr></table>");
        html.append("</div>");

        // Session Info
        html.append("<div style=\"background-color: #f9fafb; border-radius: 12px; padding: 24px; margin-bottom: 24px;\">");
        html.append("<h3 style=\"margin: 0 0 20px 0; color: #111827; font-size: 16px; font-weight: 600;\">Thông tin buổi học</h3>");
        html.append("<table style=\"width: 100%; border-collapse: collapse;\">");

        // Date
        html.append("<tr>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb;\"><span style=\"color: #6b7280; font-size: 14px;\">Ngày học</span></td>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb; text-align: right;\"><strong style=\"color: #111827; font-size: 14px;\">").append(escapeHtml(date)).append("</strong></td>");
        html.append("</tr>");

        // Class
        html.append("<tr>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb;\"><span style=\"color: #6b7280; font-size: 14px;\">Lớp học</span></td>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb; text-align: right;\"><strong style=\"color: #111827; font-size: 14px;\">").append(escapeHtml(className)).append("</strong></td>");
        html.append("</tr>");

        // Subject
        html.append("<tr>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb;\"><span style=\"color: #6b7280; font-size: 14px;\">Môn học</span></td>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb; text-align: right;\"><strong style=\"color: #4f46e5; font-size: 14px;\">").append(escapeHtml(subjectName)).append("</strong></td>");
        html.append("</tr>");

        // Teacher
        html.append("<tr>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb;\"><span style=\"color: #6b7280; font-size: 14px;\">Giáo viên</span></td>");
        html.append("<td style=\"padding: 12px 0; border-bottom: 1px solid #e5e7eb; text-align: right;\"><strong style=\"color: #111827; font-size: 14px;\">").append(escapeHtml(teacherName)).append("</strong></td>");
        html.append("</tr>");

        // Time
        html.append("<tr>");
        html.append("<td style=\"padding: 12px 0;\"><span style=\"color: #6b7280; font-size: 14px;\">Thời gian</span></td>");
        html.append("<td style=\"padding: 12px 0; text-align: right;\"><strong style=\"color: #111827; font-size: 14px;\">").append(escapeHtml(timeSlot)).append("</strong></td>");
        html.append("</tr>");

        html.append("</table>");
        html.append("</div>");

        // Attendance Status
        html.append("<div style=\"background: linear-gradient(135deg, #fefce8 0%, #fef9c3 100%); border-radius: 12px; padding: 24px; margin-bottom: 24px; border: 1px solid #fde047;\">");
        html.append("<h3 style=\"margin: 0 0 16px 0; color: #854d0e; font-size: 16px; font-weight: 600;\">Điểm danh</h3>");
        html.append("<div style=\"text-align: center;\">");
        html.append("<div style=\"display: inline-block; background-color: #ffffff; border-radius: 12px; padding: 20px 40px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.08);\">");
        html.append("<span style=\"font-size: 36px; display: block; margin-bottom: 8px;\">").append(attendanceIcon).append("</span>");
        html.append("<span style=\"color: ").append(attendanceColor).append("; font-size: 18px; font-weight: 700;\">").append(escapeHtml(attendanceStatus)).append("</span>");
        html.append("</div>");
        html.append("</div>");
        html.append(noteHtml);
        html.append("</div>");

        // Lesson Content
        html.append("<div style=\"background-color: #f0fdf4; border-radius: 12px; padding: 24px; margin-bottom: 24px; border: 1px solid #bbf7d0;\">");
        html.append("<h3 style=\"margin: 0 0 20px 0; color: #166534; font-size: 16px; font-weight: 600;\">Nội dung buổi học</h3>");
        html.append(lessonHtml);
        html.append(detailHtml);
        html.append("</div>");

        html.append("</td>");
        html.append("</tr>");

        // Footer
        html.append("<tr>");
        html.append("<td style=\"background-color: #1f2937; padding: 32px 30px; border-radius: 0 0 16px 16px; text-align: center;\">");
        html.append("<p style=\"color: #9ca3af; font-size: 14px; margin: 0 0 16px 0; line-height: 1.6;\">Trân trọng cảm ơn Quý Phụ huynh đã tin tưởng và đồng hành cùng 360EDU!</p>");
        html.append("<div style=\"border-top: 1px solid #374151; padding-top: 20px; margin-top: 20px;\">");
        html.append("<p style=\"color: #6b7280; font-size: 12px; margin: 0;\">© 2025 360EDU - Hệ thống Quản lý Giáo dục</p>");
        html.append("<p style=\"color: #6b7280; font-size: 12px; margin: 8px 0 0 0;\">Email này được gửi tự động, vui lòng không trả lời trực tiếp.</p>");
        html.append("</div>");
        html.append("</td>");
        html.append("</tr>");

        html.append("</table>");
        html.append("</td>");
        html.append("</tr>");
        html.append("</table>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getParentEmail(Student student) {
        if (student == null) {
            return null;
        }
        Parent parent = student.getParent();
        if (parent == null) {
            return null;
        }
        User parentUser = parent.getUser();
        if (parentUser == null) {
            return null;
        }
        return parentUser.getEmail();
    }

    private String formatTime(Time time) {
        return time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
