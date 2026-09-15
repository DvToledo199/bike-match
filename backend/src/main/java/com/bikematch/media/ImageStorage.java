package com.bikematch.media;

import java.net.URI;

public interface ImageStorage {

    URI upload(byte[] content, String publicId);

    void delete(String publicId);
}
