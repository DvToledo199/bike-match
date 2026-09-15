package com.bikematch.media.cloudinary;

import com.bikematch.media.ImageStorage;
import com.bikematch.media.ImageStorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageStorage.class);

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryImageStorage(@Value("${app.cloudinary.url:}") String cloudinaryUrl) {
        this(createClient(cloudinaryUrl));
    }

    CloudinaryImageStorage(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public URI upload(byte[] content, String publicId) {
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                    "resource_type", "image",
                    "public_id", publicId,
                    "overwrite", true,
                    "invalidate", true,
                    "unique_filename", false));
        } catch (IOException | RuntimeException exception) {
            log.warn("Cloudinary rejected the upload of {}: {}", publicId, exception.getMessage());
            throw new ImageStorageException(exception);
        }

        if (result == null) {
            throw invalidResponse(publicId);
        }
        Object secureUrl = result.get("secure_url");
        if (!(secureUrl instanceof String value) || value.isBlank()) {
            throw invalidResponse(publicId);
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            log.warn("Cloudinary returned a malformed photo URL for {}", publicId);
            throw new ImageStorageException(exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.toString().length() > 2048) {
            throw invalidResponse(publicId);
        }
        return uri;
    }

    private static ImageStorageException invalidResponse(String publicId) {
        log.warn("Cloudinary returned an unusable upload response for {}", publicId);
        return new ImageStorageException();
    }

    private static Cloudinary createClient(String cloudinaryUrl) {
        Cloudinary client = cloudinaryUrl == null || cloudinaryUrl.isBlank()
                ? new Cloudinary()
                : new Cloudinary(cloudinaryUrl.strip());
        client.config.secure = true;
        return client;
    }
}
