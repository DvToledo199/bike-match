package com.bikematch.moderation;

public class RemovalNoticeNotFoundException extends RuntimeException {

    public RemovalNoticeNotFoundException() {
        super("Notice not found");
    }
}
