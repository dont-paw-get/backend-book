package com.chc.dpgb.common.exception;

public class LibrarianAlreadyOwnedException extends ConflictException {

    private static final String CODE = "LIBRARIAN_ALREADY_OWNED";
    private static final String DEFAULT_MESSAGE = "이미 해당 타입의 사서를 보유하고 있습니다.";

    public LibrarianAlreadyOwnedException() {
        this(DEFAULT_MESSAGE);
    }

    public LibrarianAlreadyOwnedException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
