package com.chc.dpgb.common.exception;

public class LibrarianAccessDeniedException extends ForbiddenException {

    private static final String CODE = "LIBRARIAN_ACCESS_DENIED";
    private static final String DEFAULT_MESSAGE = "해당 사서에 접근할 권한이 없습니다.";

    public LibrarianAccessDeniedException() {
        this(DEFAULT_MESSAGE);
    }

    public LibrarianAccessDeniedException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
