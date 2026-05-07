package com.hls.streaming.media.utils;

import com.hls.streaming.common.exception.BadRequestException;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@UtilityClass
public class MediaUtils {

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

    public static String generateKey(final String rawVideoPrefix,  final String userId, final String originalFilename) {
        if (StringUtils.isBlank(rawVideoPrefix) || StringUtils.isBlank(userId) || StringUtils.isBlank(originalFilename)) {
            throw new IllegalArgumentException();
        }
        var safeFileName = generateSafeFileName(originalFilename);
        return rawVideoPrefix + userId + "/" + safeFileName;
    }

    public static String extractUserIdFromKey(final String key, final String rawVideoPrefix) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(rawVideoPrefix)) {
            throw new IllegalArgumentException();
        }

        var prefix = rawVideoPrefix;
        if (!rawVideoPrefix.endsWith("/")) {
            prefix += "/";
        }

        if (!key.startsWith(prefix)) {
            throw new IllegalArgumentException("Key does not start with the expected prefix");
        }

        var remainingPath = key.substring(prefix.length());
        var parts = remainingPath.split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Key does not contain userId and file name");
        }

        return parts[0];
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

    public static String encodeCursor(final long createdAt, final String id) {
        if (StringUtils.isBlank(id)) {
            throw new BadRequestException("Cursor id is blank");
        }

        final var raw = createdAt + "_" + id;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeCursor(final String cursor) {
        if (StringUtils.isBlank(cursor)) {
            throw new BadRequestException("Cursor is blank");
        }

        try {
            final var decoded = Base64.getUrlDecoder().decode(cursor);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid cursor encoding", ex);
        }
    }
}
