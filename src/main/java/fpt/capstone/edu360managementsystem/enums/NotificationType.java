package fpt.capstone.edu360managementsystem.enums;

/**
 * Các loại thông báo trong hệ thống
 */
public enum NotificationType {
    // Enrollment & Class
    ENROLLED_NEW_CLASS,      // Bạn đã được thêm vào lớp mới
    REMOVED_FROM_CLASS,      // Bạn đã bị xóa khỏi lớp
    CLASS_CANCELLED,         // Lớp học đã bị hủy
    
    // Schedule
    SCHEDULE_CHANGED,        // Lịch học đã thay đổi
    CLASS_REMINDER,          // Nhắc nhở buổi học sắp diễn ra
    
    // Attendance
    ATTENDANCE_PRESENT,      // Điểm danh có mặt
    ATTENDANCE_ABSENT,       // Điểm danh vắng mặt
    ATTENDANCE_LATE,         // Điểm danh đi muộn
    
    // Payment
    PAYMENT_SUCCESS,         // Thanh toán thành công
    PAYMENT_FAILED,          // Thanh toán thất bại
    PAYMENT_REMINDER,        // Nhắc thanh toán
    
    // Course & Content
    NEW_LESSON_AVAILABLE,    // Bài học mới
    ASSIGNMENT_DUE,          // Deadline bài tập
    
    // System
    SYSTEM_ANNOUNCEMENT,     // Thông báo hệ thống
    ACCOUNT_UPDATE,          // Cập nhật tài khoản
    
    // News
    NEW_NEWS_POSTED          // Tin tức mới
}
