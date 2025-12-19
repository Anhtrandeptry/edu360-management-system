package fpt.capstone.edu360managementsystem.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

/**
 * Service for uploading files to Cloudinary cloud storage. Supports images,
 * documents, and other file types.
 */
@Service
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Cloudinary initialized with cloud_name: {}", cloudName);
    }

    /**
     * Upload an image file to Cloudinary
     *
     * @param file The image file to upload
     * @param folder The folder path in Cloudinary (e.g., "avatars",
     * "news-images")
     * @return The secure URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        validateImageFile(file);

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "360edu_system/" + folder,
                "resource_type", "image",
                "use_filename", true,
                "unique_filename", true
        );

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) result.get("secure_url");

        log.info("Image uploaded to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    /**
     * Upload any file (document, PDF, etc.) to Cloudinary
     *
     * @param file The file to upload
     * @param folder The folder path in Cloudinary (e.g.,
     * "materials/session-123")
     * @return The secure URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Determine resource type based on file type
        String resourceType = "auto"; // Let Cloudinary determine the type

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "360edu_system/" + folder,
                "resource_type", resourceType,
                "use_filename", true,
                "unique_filename", true
        );

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) result.get("secure_url");

        log.info("File uploaded to Cloudinary: {} (type: {})", secureUrl, file.getContentType());
        return secureUrl;
    }

    /**
     * Upload raw file (any type) to Cloudinary
     *
     * @param file The file to upload
     * @param folder The folder path in Cloudinary
     * @return The secure URL of the uploaded file
     */
    public String uploadRawFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", "360edu_system/" + folder,
                "resource_type", "raw",
                "use_filename", true,
                "unique_filename", true
        );

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) result.get("secure_url");

        log.info("Raw file uploaded to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    /**
     * Delete a file from Cloudinary by its public ID
     *
     * @param publicId The public ID of the file to delete
     */
    public void deleteFile(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("File deleted from Cloudinary: {} - Result: {}", publicId, result.get("result"));
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, e);
        }
    }

    /**
     * Extract public ID from Cloudinary URL for deletion
     *
     * @param cloudinaryUrl The full Cloudinary URL
     * @return The public ID
     */
    public String extractPublicIdFromUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return null;
        }

        try {
            // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
            String[] parts = cloudinaryUrl.split("/upload/");
            if (parts.length > 1) {
                String pathWithVersion = parts[1];
                // Remove version (v1234567890/)
                if (pathWithVersion.startsWith("v")) {
                    int slashIndex = pathWithVersion.indexOf('/');
                    if (slashIndex > 0) {
                        pathWithVersion = pathWithVersion.substring(slashIndex + 1);
                    }
                }
                // Remove file extension
                int lastDot = pathWithVersion.lastIndexOf('.');
                if (lastDot > 0) {
                    return pathWithVersion.substring(0, lastDot);
                }
                return pathWithVersion;
            }
        } catch (Exception e) {
            log.warn("Could not extract public ID from URL: {}", cloudinaryUrl);
        }
        return null;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image (PNG, JPG, JPEG, GIF, WebP)");
        }

        // Max 10MB for images
        long maxSize = 10L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Image size exceeds 10MB limit");
        }
    }
}
