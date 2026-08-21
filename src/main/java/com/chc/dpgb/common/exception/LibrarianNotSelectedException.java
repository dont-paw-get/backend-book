package com.chc.dpgb.common.exception;

public class LibrarianNotSelectedException extends NotFoundException {

    private static final String CODE = "LIBRARIAN_NOT_SELECTED";
    private static final String DEFAULT_MESSAGE = "아직 대표 사서를 선택하지 않았습니다.";

    public LibrarianNotSelectedException() {
        this(DEFAULT_MESSAGE);
    }

    public LibrarianNotSelectedException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
