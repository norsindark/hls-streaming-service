package com.hls.streaming.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@UtilityClass
public class FileUtils {

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "mov", "mkv", "avi", "webm");

    public static void validateVideoFile(final MultipartFile file) {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (!Objects.requireNonNull(file.getContentType()).startsWith("video/")) {
            throw new IllegalArgumentException("Invalid content type");
        }

        var originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new IllegalArgumentException();
        }

        var extension = getFileExtension(originalFilename);
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException();
        }
    }

    public static String generateSafeFileName(final String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            throw new IllegalArgumentException();
        }

        var extension = getFileExtension(originalFilename);

        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        var random = UUID.randomUUID().toString().substring(0, 4);

        return timestamp + random + "." + extension;
    }

    public static String getFileExtension(final String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return StringUtils.EMPTY;
        }

        var lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return StringUtils.EMPTY;
    }
}
