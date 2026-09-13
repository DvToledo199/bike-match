package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BikePhotoFileTest {

    @Test
    void recognizesSupportedImageContent() {
        assertThat(new BikePhotoFile(jpeg(), "image/jpeg").contentType())
                .isEqualTo("image/jpeg");
        assertThat(new BikePhotoFile(png(), "image/png").contentType())
                .isEqualTo("image/png");
        assertThat(new BikePhotoFile(webP(), "image/webp").contentType())
                .isEqualTo("image/webp");
    }

    @Test
    void rejectsEmptyUnsupportedAndMismatchedContent() {
        assertThatThrownBy(() -> new BikePhotoFile(new byte[0], "image/jpeg"))
                .isInstanceOf(InvalidBikePhotoException.class)
                .hasMessage("Photo is required");
        assertThatThrownBy(() -> new BikePhotoFile("text".getBytes(), "image/jpeg"))
                .isInstanceOf(InvalidBikePhotoException.class)
                .hasMessage("Photo must be a JPG, PNG or WebP image");
        assertThatThrownBy(() -> new BikePhotoFile(png(), "image/jpeg"))
                .isInstanceOf(InvalidBikePhotoException.class)
                .hasMessage("Photo content does not match its file type");
    }

    @Test
    void rejectsContentLargerThanTenMegabytes() {
        byte[] oversized = new byte[BikePhotoFile.MAX_SIZE_BYTES + 1];

        assertThatThrownBy(() -> new BikePhotoFile(oversized, "image/jpeg"))
                .isInstanceOf(InvalidBikePhotoException.class)
                .hasMessage("Photo must not exceed 10 MB");
    }

    @Test
    void protectsItsBytesFromExternalChanges() {
        byte[] original = jpeg();
        BikePhotoFile photo = new BikePhotoFile(original, "image/jpeg");

        original[0] = 0;
        byte[] returned = photo.content();
        returned[0] = 0;

        assertThat(photo.content()[0]).isEqualTo((byte) 0xFF);
    }

    static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00,
                (byte) 0xFF, (byte) 0xD9};
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private static byte[] webP() {
        byte[] bytes = new byte[16];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        System.arraycopy("VP8X".getBytes(StandardCharsets.US_ASCII), 0, bytes, 12, 4);
        return bytes;
    }
}
