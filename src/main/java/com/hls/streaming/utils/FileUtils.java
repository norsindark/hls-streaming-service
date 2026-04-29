package com.hls.streaming.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
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
        var baseNameIndex = originalFilename.lastIndexOf(".");

        var baseName = originalFilename;
        if (baseNameIndex != -1) {
            baseName = originalFilename.substring(0, baseNameIndex);
        }

        var normalizedString = Normalizer.normalize(baseName, Normalizer.Form.NFD);
        var asciiString = normalizedString.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        var cleanAsciiString = asciiString.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        var cleanBaseName = StringUtils.substring(cleanAsciiString, 0, 50);

        if (StringUtils.isBlank(cleanBaseName)) {
            cleanBaseName = "video";
        }

        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        var randomString = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 4);

        return cleanBaseName + timestamp + randomString + "." + extension;
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
