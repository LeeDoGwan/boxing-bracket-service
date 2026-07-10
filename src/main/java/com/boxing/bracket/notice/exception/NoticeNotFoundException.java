package com.boxing.bracket.notice.exception;

public class NoticeNotFoundException extends RuntimeException {

    public NoticeNotFoundException() {
        super("Notice not found");
    }
}
