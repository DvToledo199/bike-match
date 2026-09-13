package com.bikematch.media.cloudinary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;

import com.bikematch.media.ImageStorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new CloudinaryImageStorage(cloudinary);
        given(cloudinary.uploader()).willReturn(uploader);
    }

    @Test
    void returnsTheSecureUrlAndUsesAReplaceablePublicId() throws IOException {
        given(uploader.upload(any(), anyMap())).willReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/bike.jpg"));

        URI result = storage.upload(new byte[]{1}, "bikematch/bikes/7");

        assertThat(result).hasToString(
                "https://res.cloudinary.com/demo/image/upload/bike.jpg");
        ArgumentCaptor<Map<?, ?>> options = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(uploader).upload(any(), options.capture());
        assertThat(options.getValue().get("resource_type")).isEqualTo("image");
        assertThat(options.getValue().get("public_id")).isEqualTo("bikematch/bikes/7");
        assertThat(options.getValue().get("overwrite")).isEqualTo(true);
        assertThat(options.getValue().get("invalidate")).isEqualTo(true);
    }

    @Test
    void translatesProviderFailuresIntoTheSharedStorageException() throws IOException {
        given(uploader.upload(any(), anyMap())).willThrow(new IOException("provider down"));

        assertThatThrownBy(() -> storage.upload(new byte[]{1}, "bikematch/bikes/7"))
                .isInstanceOf(ImageStorageException.class)
                .hasMessage("Photo storage is temporarily unavailable");
    }

    @Test
    void rejectsMissingOrInsecureProviderUrls() throws IOException {
        given(uploader.upload(any(), anyMap())).willReturn(null);
        assertThatThrownBy(() -> storage.upload(new byte[]{1}, "bikematch/bikes/7"))
                .isInstanceOf(ImageStorageException.class);

        given(uploader.upload(any(), anyMap())).willReturn(Map.of());
        assertThatThrownBy(() -> storage.upload(new byte[]{1}, "bikematch/bikes/7"))
                .isInstanceOf(ImageStorageException.class);

        given(uploader.upload(any(), anyMap())).willReturn(Map.of(
                "secure_url", "http://example.com/bike.jpg"));
        assertThatThrownBy(() -> storage.upload(new byte[]{1}, "bikematch/bikes/7"))
                .isInstanceOf(ImageStorageException.class);
    }
}
