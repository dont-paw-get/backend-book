package com.chc.dpgb.common.exception;

public class InvalidLibrarianDataException extends BadRequestException {

    private static final String CODE = "INVALID_LIBRARIAN_DATA";
    private static final String DEFAULT_MESSAGE = "올바르지 않은 사서 정보입니다.";

    public InvalidLibrarianDataException() {
        this(DEFAULT_MESSAGE);
    }

    public InvalidLibrarianDataException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
