package com.chc.dpgb.common.exception;

public class LibrarianNotFoundException extends NotFoundException {

    private static final String CODE = "LIBRARIAN_NOT_FOUND";
    private static final String DEFAULT_MESSAGE = "사서를 찾을 수 없습니다.";

    public LibrarianNotFoundException() {
        this(DEFAULT_MESSAGE);
    }

    public LibrarianNotFoundException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
