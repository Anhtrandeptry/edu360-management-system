package fpt.capstone.edu360managementsystem.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.enums.NotificationType;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job để nhắc nhở admin về các lớp DRAFT sắp đến ngày bắt đầu.
 *
 * Logic: - Chạy mỗi ngày lúc 8:00 sáng - Kiểm tra các lớp DRAFT có startDate
 * còn 2 hoặc 3 ngày - Gửi thông báo cho tất cả Admin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftClassReminderScheduler {

    private final ClazzRepository clazzRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Chạy mỗi ngày lúc 8:00 sáng Cron: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkDraftClassesApproachingStartDate() {
        log.info("🔔 [DraftClassReminder] Starting daily check for DRAFT classes approaching start date...");

        LocalDate today = LocalDate.now();
        LocalDate twoDaysLater = today.plusDays(2);
        LocalDate threeDaysLater = today.plusDays(3);

        // Lấy danh sách lớp DRAFT có startDate trong khoảng 2-3 ngày tới
        List<Clazz> draftClasses = clazzRepository.findDraftClassesWithStartDateBetween(twoDaysLater, threeDaysLater);

        if (draftClasses.isEmpty()) {
            log.info("🔔 [DraftClassReminder] No DRAFT classes approaching start date.");
            return;
        }

        log.info("🔔 [DraftClassReminder] Found {} DRAFT classes approaching start date", draftClasses.size());

        // Lấy danh sách Admin
        List<User> admins = userRepository.findAllByRole(ERole.ROLE_ADMIN);
        if (admins.isEmpty()) {
            log.warn("🔔 [DraftClassReminder] No admin users found to notify!");
            return;
        }

        List<Long> adminIds = admins.stream().map(User::getId).toList();
        log.info("🔔 [DraftClassReminder] Will notify {} admin(s)", adminIds.size());

        // Gửi thông báo cho từng lớp
        for (Clazz clazz : draftClasses) {
            sendReminderNotification(clazz, adminIds);
        }

        log.info("🔔 [DraftClassReminder] Completed! Sent notifications for {} DRAFT classes to {} admins",
                draftClasses.size(), adminIds.size());
    }

    /**
     * Gửi thông báo nhắc nhở cho một lớp DRAFT
     */
    private void sendReminderNotification(Clazz clazz, List<Long> adminIds) {
        String className = clazz.getName();
        String startDateStr = clazz.getStartDate().format(DATE_FORMATTER);
        long daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), clazz.getStartDate());

        String title = "⚠️ Lớp học sắp đến ngày hoạt động";
        String message = String.format(
                "Lớp \"%s\" đang ở trạng thái \"Bản nháp\" sắp đến ngày hoạt động là ngày %s (còn %d ngày). "
                + "Vui lòng xuất bản hoặc xóa lớp này.",
                className, startDateStr, daysUntilStart
        );
        String link = "/home/admin/class"; // Link đến trang quản lý lớp học

        try {
            notificationService.createNotificationForUsers(
                    adminIds,
                    title,
                    message,
                    NotificationType.DRAFT_CLASS_REMINDER,
                    link
            );
            log.info("🔔 [DraftClassReminder] Sent notification for class '{}' (ID: {}, startDate: {})",
                    className, clazz.getId(), startDateStr);
        } catch (Exception e) {
            log.error("🔔 [DraftClassReminder] Failed to send notification for class '{}': {}",
                    className, e.getMessage());
        }
    }

    /**
     * Method để test thủ công (có thể gọi từ controller nếu cần)
     */
    public void runManually() {
        log.info("🔔 [DraftClassReminder] Manual trigger started...");
        checkDraftClassesApproachingStartDate();
    }
}
