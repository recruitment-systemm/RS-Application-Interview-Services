package org.example.applicationinterviewservices.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.applicationinterviewservices.exception.FileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx");
    private final Cloudinary cloudinary;
    public String uploadDocument(MultipartFile file) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        String resourceType = extension.equals("pdf") ? "image" : "raw";
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", resourceType,
                            "folder", "organizations/documents",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
            return result.get("secure_url").toString();
        } catch (IOException exception) {
            throw new FileUploadException("Failed to upload document", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size must not exceed 10 MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new FileUploadException("Invalid file name");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        String contentType = file.getContentType();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileUploadException("Only PDF and DOCX files are allowed");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileUploadException("Invalid file content type. Only PDF and DOCX files are allowed");
        }
    }
}