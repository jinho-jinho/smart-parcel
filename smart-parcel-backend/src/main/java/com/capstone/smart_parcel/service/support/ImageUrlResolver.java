package com.capstone.smart_parcel.service.support;

import com.capstone.smart_parcel.config.StorageProperties;
import com.capstone.smart_parcel.dto.history.ImageResourceBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

    private final StorageProperties storageProperties;

    public ImageResourceBundle bundle(String storedPath) {
        String resolved = resolve(storedPath);
        if (resolved == null) {
            return ImageResourceBundle.of(null, null, null);
        }
        return ImageResourceBundle.of(resolved, null, null);
    }

    public String resolve(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        String trimmed = storedPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String prefix = storageProperties.publicUrlPrefixNormalized();
        String candidate = trimmed.replace("\\", "/");
        String relative = toRelativePath(candidate, prefix);
        if (relative == null || relative.isBlank()) {
            return null;
        }

        if (!isWithinStorage(relative)) {
            return null;
        }

        String publicPath = prefix + "/" + relative;
        return buildAbsolute(publicPath);
    }

    private String toRelativePath(String candidate, String prefix) {
        String prefixWithoutSlash = prefix.length() > 1 ? prefix.substring(1) : "";
        String normalized = candidate;

        if (normalized.startsWith(prefix + "/")) {
            normalized = normalized.substring(prefix.length() + 1);
        } else if (!prefixWithoutSlash.isEmpty() && normalized.startsWith(prefixWithoutSlash + "/")) {
            normalized = normalized.substring(prefixWithoutSlash.length() + 1);
        } else if (!prefixWithoutSlash.isEmpty() && normalized.equals(prefixWithoutSlash)) {
            normalized = "";
        } else if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private boolean isWithinStorage(String relativePath) {
        if (relativePath.contains("..")) {
            return false;
        }
        try {
            var storageRoot = storageProperties.imageDirPath();
            var resolved = storageRoot.resolve(relativePath).normalize();
            if (!resolved.startsWith(storageRoot)) {
                return false;
            }
            return java.nio.file.Files.exists(resolved);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildAbsolute(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(normalized)
                .build()
                .toUriString();
    }
}
