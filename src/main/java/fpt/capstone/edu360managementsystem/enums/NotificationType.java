package fpt.capstone.edu360managementsystem.enums;

/**
 * Các loại thông báo trong hệ thống
 */
public enum NotificationType {
    // Enrollment & Class
    ENROLLED_NEW_CLASS,
    REMOVED_FROM_CLASS,
    CLASS_CANCELLED,
    
    // Schedule
    SCHEDULE_CHANGED,
    CLASS_REMINDER,
    
    // Attendance
    ATTENDANCE_PRESENT,
    ATTENDANCE_ABSENT,
    ATTENDANCE_LATE,
    
    // Payment
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_REMINDER,
    
    // Course & Content
    NEW_LESSON_AVAILABLE,
    ASSIGNMENT_DUE,
    
    // System
    SYSTEM_ANNOUNCEMENT,
    ACCOUNT_UPDATE,
    
    // News
    NEW_NEWS_POSTED
}
