package com.chc.dpgb.common.exception;

public class InvalidScrapDataException extends BadRequestException {

    private static final String CODE = "INVALID_SCRAP_DATA";
    private static final String DEFAULT_MESSAGE = "올바르지 않은 스크랩 정보입니다.";

    public InvalidScrapDataException() {
        this(DEFAULT_MESSAGE);
    }

    public InvalidScrapDataException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
