package com.atp.printing.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.upload.folder}")
    private String uploadFolder;

    /**
     * Upload file to Cloudinary
     */
    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {
        log.info("Uploading file to Cloudinary: {}", file.getOriginalFilename());

        try {
            String publicId = uploadFolder + "/" + UUID.randomUUID();

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "auto",
                            "folder", uploadFolder
                    )
            );

            log.info("File uploaded successfully to Cloudinary: {}", uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new IOException("Failed to upload file to Cloudinary", e);
        }
    }

    /**
     * Upload file from byte array
     */
    public Map<String, Object> uploadFile(byte[] fileBytes, String filename) throws IOException {
        log.info("Uploading file to Cloudinary: {}", filename);

        try {
            String publicId = uploadFolder + "/" + UUID.randomUUID() + "-" + filename;

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    fileBytes,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "auto",
                            "folder", uploadFolder
                    )
            );

            log.info("File uploaded successfully: {}", uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new IOException("Failed to upload file", e);
        }
    }

    /**
     * Delete file from Cloudinary
     */
    public void deleteFile(String publicId) throws IOException {
        log.info("Deleting file from Cloudinary: {}", publicId);

        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("File deleted from Cloudinary: {}", result);

        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary", e);
            throw new IOException("Failed to delete file", e);
        }
    }

    /**
     * Get file URL
     */
    public String getFileUrl(String publicId) {
        return cloudinary.url().secure(true).generate(publicId);
    }

    /**
     * Extract public ID from Cloudinary URL
     */
    public String extractPublicId(String cloudinaryUrl) {
        try {
            // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
            String[] parts = cloudinaryUrl.split("/");
            String lastPart = parts[parts.length - 1];
            String publicIdWithExt = lastPart.substring(0, lastPart.lastIndexOf('.'));

            // Find the folder path
            int uploadIndex = cloudinaryUrl.indexOf("/upload/");
            if (uploadIndex != -1) {
                String afterUpload = cloudinaryUrl.substring(uploadIndex + 8);
                String[] segments = afterUpload.split("/");
                if (segments.length > 1) {
                    // Skip version number and build public_id
                    StringBuilder publicId = new StringBuilder();
                    for (int i = 1; i < segments.length - 1; i++) {
                        publicId.append(segments[i]).append("/");
                    }
                    publicId.append(publicIdWithExt);
                    return publicId.toString();
                }
            }

            return uploadFolder + "/" + publicIdWithExt;
        } catch (Exception e) {
            log.error("Failed to extract public ID from URL: {}", cloudinaryUrl, e);
            return null;
        }
    }
}