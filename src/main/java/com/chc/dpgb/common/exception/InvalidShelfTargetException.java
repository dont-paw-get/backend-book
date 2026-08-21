package com.chc.dpgb.common.exception;

public class InvalidShelfTargetException extends BadRequestException {

    private static final String CODE = "INVALID_SHELF_TARGET";
    private static final String DEFAULT_MESSAGE = "대상 책장을 찾을 수 없거나 이 사용자의 책장이 아닙니다.";

    public InvalidShelfTargetException() {
        this(DEFAULT_MESSAGE);
    }

    public InvalidShelfTargetException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
