package fpt.capstone.edu360managementsystem.dto.response;

import lombok.*;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionMaterialResponse {
    private Long id;
    private Long sessionId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String description;
    private LocalDateTime uploadedAt;
    private String uploadedByName;
    

    public String getFileSizeDisplay() {
        if (fileSize == null) return "";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
    

    public String getFileIcon() {
        if (fileType == null) return "file";
        if (fileType.startsWith("image/")) return "image";
        if (fileType.equals("application/pdf")) return "pdf";
        if (fileType.contains("word") || fileType.contains("document")) return "word";
        if (fileType.contains("excel") || fileType.contains("spreadsheet")) return "excel";
        if (fileType.contains("powerpoint") || fileType.contains("presentation")) return "powerpoint";
        if (fileType.startsWith("video/")) return "video";
        if (fileType.startsWith("audio/")) return "audio";
        if (fileType.contains("zip") || fileType.contains("rar") || fileType.contains("archive")) return "archive";
        return "file";
    }
}
