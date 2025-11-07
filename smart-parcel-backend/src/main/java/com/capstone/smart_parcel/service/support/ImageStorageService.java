package com.capstone.smart_parcel.service.support;

import com.capstone.smart_parcel.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ImageStorageService {

    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StorageProperties storageProperties;

    public String store(MultipartFile file, String category, String bucket) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty.");
        }

        Path root = storageProperties.imageDirPath();
        Path targetDir = root;

        if (StringUtils.hasText(category)) {
            targetDir = targetDir.resolve(sanitizeSegment(category));
        }
        if (StringUtils.hasText(bucket)) {
            targetDir = targetDir.resolve(sanitizeSegment(bucket));
        }

        targetDir = targetDir.resolve(LocalDate.now().format(DATE_FOLDER_FORMAT));

        try {
            Files.createDirectories(targetDir);

            String extension = determineExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (!extension.isEmpty()) {
                filename = filename + "." + extension;
            }

            Path targetFile = targetDir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Path relative = root.relativize(targetFile);
            return relative.toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store image file.", ex);
        }
    }

    private String sanitizeSegment(String raw) {
        String trimmed = raw.trim();
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            throw new IllegalArgumentException("Invalid storage path segment: " + raw);
        }
        return trimmed.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String determineExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        String ext = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(ext)) {
            return "";
        }
        return ext.trim().toLowerCase(Locale.ROOT);
    }
}
