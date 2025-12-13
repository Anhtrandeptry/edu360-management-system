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
    private Integer dayOfWeek;
    private String attendanceStatus;

    private String lessonContent;

    // Link Google Meet cho lớp học online
    private String meetingLink;
    private Boolean isOnline;

    private List<SessionChapterInfo> linkedChapters;
    private List<SessionLessonInfo> linkedLessons;

    private Long courseId;
    private String courseTitle;

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
