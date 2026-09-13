package com.bikematch.bike;

import java.util.Arrays;
import java.util.Locale;

public record BikePhotoFile(byte[] content, String contentType) {

    static final int MAX_SIZE_BYTES = 10 * 1024 * 1024;

    public BikePhotoFile {
        if (content == null || content.length == 0) {
            throw new InvalidBikePhotoException("Photo is required");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new InvalidBikePhotoException("Photo must not exceed 10 MB");
        }

        String detectedContentType = detectContentType(content);
        if (detectedContentType == null) {
            throw new InvalidBikePhotoException("Photo must be a JPG, PNG or WebP image");
        }

        String declaredContentType = normalizeContentType(contentType);
        if (declaredContentType != null
                && !"application/octet-stream".equals(declaredContentType)
                && !detectedContentType.equals(declaredContentType)) {
            throw new InvalidBikePhotoException("Photo content does not match its file type");
        }

        content = Arrays.copyOf(content, content.length);
        contentType = detectedContentType;
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String normalized = contentType.strip().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
    }

    private static String detectContentType(byte[] bytes) {
        if (isJpeg(bytes)) {
            return "image/jpeg";
        }
        if (isPng(bytes)) {
            return "image/png";
        }
        if (isWebP(bytes)) {
            return "image/webp";
        }
        return null;
    }

    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 4
                && unsigned(bytes[0]) == 0xFF
                && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF
                && unsigned(bytes[bytes.length - 2]) == 0xFF
                && unsigned(bytes[bytes.length - 1]) == 0xD9;
    }

    private static boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (unsigned(bytes[index]) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWebP(byte[] bytes) {
        return bytes.length >= 16
                && matchesAscii(bytes, 0, "RIFF")
                && matchesAscii(bytes, 8, "WEBP")
                && (matchesAscii(bytes, 12, "VP8 ")
                    || matchesAscii(bytes, 12, "VP8L")
                    || matchesAscii(bytes, 12, "VP8X"));
    }

    private static boolean matchesAscii(byte[] bytes, int offset, String expected) {
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
