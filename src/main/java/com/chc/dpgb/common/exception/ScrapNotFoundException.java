package com.chc.dpgb.common.exception;

public class ScrapNotFoundException extends NotFoundException {

    private static final String CODE = "SCRAP_NOT_FOUND";
    private static final String DEFAULT_MESSAGE = "스크랩을 찾을 수 없습니다.";

    public ScrapNotFoundException() {
        this(DEFAULT_MESSAGE);
    }

    public ScrapNotFoundException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
