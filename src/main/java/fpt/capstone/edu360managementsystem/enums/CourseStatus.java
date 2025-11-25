package fpt.capstone.edu360managementsystem.enums;

public enum CourseStatus {
    DRAFT,      // (tùy chọn, nếu sau này muốn)
    PENDING,    // giáo viên tạo => pending chờ duyệt
    APPROVED,   // đã duyệt, được public & chọn cho class
    REJECTED,   // bị từ chối
    ARCHIVED    // ngừng sử dụng
}
