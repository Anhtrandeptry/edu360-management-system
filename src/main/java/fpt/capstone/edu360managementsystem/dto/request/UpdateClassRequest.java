package fpt.capstone.edu360managementsystem.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateClassRequest {

    // For draft, these fields are editable; for public/active only a subset is used.
    private String name;
    private String description;
    private String meetingLink;
    private LocalDate startDate;
    private LocalDate endDate; // optionally accepted

    private Long roomId; // null means online

    @Min(1)
    private Integer maxStudents;

    // Giá mỗi buổi (chỉ cho phép cập nhật khi lớp đang ở trạng thái DRAFT/upcoming)
    private Long pricePerSession;

    // Mở rộng cho lớp DRAFT/upcoming: cho phép sửa các trường này
    private Long subjectId;     // subject mới (nếu đổi)
    private Long courseId;      // optional, phải thuộc subject
    private Long teacherId;     // teacher userId
    private Integer totalSessions; // tổng số buổi nếu muốn tính lại endDate & regenerate sessions
    private List<ScheduleItemRequest> schedule; // lịch lặp mới (dayOfWeek 1..7 hoặc 0..6? FE gửi 1..7 Mon..Sun)

    // Xác nhận xóa toàn bộ nội dung buổi học và khóa học của giáo viên khi cần regen
    // Mặc định = false. FE sẽ đặt true sau khi hiển thị confirm cho Admin.
    private Boolean forceDeleteContentAndCourse;
}
