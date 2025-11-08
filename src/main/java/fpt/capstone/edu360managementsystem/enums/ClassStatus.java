package fpt.capstone.edu360managementsystem.enums;

public enum ClassStatus {
    AVAILABLE,      // có thể đăng ký nhưng chưa diễn ra
    COMING_SOON,    // sắp bắt đầu (chưa đến startDate)
    STUDYING,       // đang học (giữa startDate..endDate)
    COMPLETE        // đã kết thúc (sau endDate)
}
