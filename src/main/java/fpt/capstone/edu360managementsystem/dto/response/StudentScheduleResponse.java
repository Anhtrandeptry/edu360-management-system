package fpt.capstone.edu360managementsystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StudentScheduleResponse {
    private Long sessionId;
    private Long classId;
    private String className;
    private String subjectName;
    private String teacherName;
    private String roomName;
    private LocalDate date;
    private String timeStart;
    private String timeEnd;
    private Integer dayOfWeek; // 1=Mon, 7=Sun
    private String attendanceStatus; // PRESENT, ABSENT, LATE, UNMARKED
    
    // Nội dung bài học của buổi này
    private String lessonContent;
    
    // Các chapter/lesson được gán cho buổi học này
    private List<SessionChapterInfo> linkedChapters;
    private List<SessionLessonInfo> linkedLessons;
    
    // Thông tin course của lớp
    private Long courseId;
    private String courseTitle;
    
    // Tài liệu đính kèm của buổi học
    private List<SessionMaterialInfo> materials;
    
    @Data
    @Builder
    public static class SessionChapterInfo {
        private Long id;
        private String title;
        private String description;
        private Integer orderIndex;
    }
    
    @Data
    @Builder
    public static class SessionLessonInfo {
        private Long id;
        private Long chapterId;
        private String chapterTitle;
        private String title;
        private String description;
        private Integer orderIndex;
    }
    
    @Data
    @Builder
    public static class SessionMaterialInfo {
        private Long id;
        private String fileName;
        private String fileUrl;
        private String fileType;
        private Long fileSize;
        private String description;
        private String uploadedAt;
        private String uploadedByName;
    }
}