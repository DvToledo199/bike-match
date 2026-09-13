package com.bikematch.media.cloudinary;

import com.bikematch.media.ImageStorage;
import com.bikematch.media.ImageStorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryImageStorage implements ImageStorage {

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
            throw new ImageStorageException(exception);
        }

        if (result == null) {
            throw new ImageStorageException();
        }
        Object secureUrl = result.get("secure_url");
        if (!(secureUrl instanceof String value) || value.isBlank()) {
            throw new ImageStorageException();
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new ImageStorageException(exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.toString().length() > 2048) {
            throw new ImageStorageException();
        }
        return uri;
    }

    private static Cloudinary createClient(String cloudinaryUrl) {
        Cloudinary client = cloudinaryUrl == null || cloudinaryUrl.isBlank()
                ? new Cloudinary()
                : new Cloudinary(cloudinaryUrl.strip());
        client.config.secure = true;
        return client;
    }
}
