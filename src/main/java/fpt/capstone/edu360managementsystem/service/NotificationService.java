package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.response.NotificationResponse;
import fpt.capstone.edu360managementsystem.entity.Notification;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.NotificationType;
import fpt.capstone.edu360managementsystem.repository.NotificationRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Tạo thông báo mới cho user
     */
    @Transactional
    public Notification createNotification(Long userId, String title, String message, NotificationType type, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Tạo thông báo cho nhiều user cùng lúc
     */
    @Transactional
    public void createNotificationForUsers(List<Long> userIds, String title, String message, NotificationType type, String link) {
        List<User> users = userRepository.findAllById(userIds);

        List<Notification> notifications = users.stream()
                .map(user -> Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .isRead(false)
                .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    /**
     * Lấy danh sách thông báo của user (có phân trang)
     */
    public Page<NotificationResponse> getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::toResponse);
    }

    /**
     * Lấy thông báo chưa đọc
     */
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    public long countUnread(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu 1 thông báo đã đọc
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
    }

    /**
     * Đánh dấu tất cả đã đọc
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    /**
     * Xóa 1 thông báo
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Cannot delete other user's notification");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Lấy thống kê thông báo
     */
    public Map<String, Object> getStats(Long userId) {
        long unread = notificationRepository.countByUser_IdAndIsReadFalse(userId);
        return Map.of(
                "unreadCount", unread
        );
    }

    // ===================== HELPER METHODS FOR OTHER SERVICES =====================
    /**
     * Gửi thông báo khi student được thêm vào lớp mới
     */
    public void notifyEnrolledNewClass(Long userId, String className, Long classId) {
        createNotification(
                userId,
                "Đăng ký lớp thành công",
                "Bạn đã được thêm vào lớp: " + className,
                NotificationType.ENROLLED_NEW_CLASS,
                "/home/my-classes/" + classId
        );
    }

    /**
     * Gửi thông báo khi thanh toán thành công
     */
    public void notifyPaymentSuccess(Long userId, String className, Long amount) {
        createNotification(
                userId,
                "Thanh toán thành công",
                "Bạn đã thanh toán " + String.format("%,d", amount) + "đ cho lớp " + className,
                NotificationType.PAYMENT_SUCCESS,
                "/home/my-classes"
        );
    }

    /**
     * Gửi thông báo khi lịch học thay đổi
     */
    public void notifyScheduleChanged(Long userId, String className, String details) {
        createNotification(
                userId,
                "Lịch học thay đổi",
                "Lịch học lớp " + className + " đã thay đổi. " + details,
                NotificationType.SCHEDULE_CHANGED,
                "/home/my-classes"
        );
    }

    /**
     * Gửi thông báo khi bị xóa khỏi lớp
     */
    public void notifyRemovedFromClass(Long userId, String className) {
        createNotification(
                userId,
                "Đã rời khỏi lớp học",
                "Bạn đã bị xóa khỏi lớp: " + className,
                NotificationType.REMOVED_FROM_CLASS,
                "/home/classes"
        );
    }

    /**
     * Gửi thông báo hệ thống
     */
    public void notifySystemAnnouncement(Long userId, String title, String message) {
        createNotification(
                userId,
                title,
                message,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                null
        );
    }

    /**
     * Gửi thông báo cho tất cả admin khi có student đăng ký lớp và tạo payment
     * mới
     *
     * @param studentName Tên học sinh
     * @param className Tên lớp học
     * @param amount Số tiền cần thanh toán
     * @param paymentId ID của payment để admin có thể xác nhận
     */
    public void notifyAdminsNewPaymentPending(String studentName, String className, Long amount, Long paymentId) {
        // Lấy tất cả users có role ADMIN
        List<User> admins = userRepository.findAllByRole(fpt.capstone.edu360managementsystem.enums.ERole.ROLE_ADMIN);

        if (admins.isEmpty()) {
            System.err.println("No admin found to notify about new payment");
            return;
        }

        String formattedAmount = String.format("%,d", amount).replace(",", ".");
        String title = "🔔 Có đăng ký lớp mới chờ thanh toán";
        String message = "Học sinh " + studentName + " đã đăng ký lớp " + className
                + " với học phí " + formattedAmount + "đ. Vui lòng kiểm tra khi nhận được chuyển khoản.";

        List<Long> adminIds = admins.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        createNotificationForUsers(
                adminIds,
                title,
                message,
                NotificationType.NEW_PAYMENT_PENDING,
                "/admin/payments?highlight=" + paymentId
        );
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .link(n.getLink())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
