package com.chc.dpgb.common.exception;

public class InvalidSearchParameterException extends BadRequestException {

    private static final String CODE = "INVALID_SEARCH_PARAMETER";
    private static final String DEFAULT_MESSAGE = "유효한 isbn이 필요합니다.";

    public InvalidSearchParameterException() {
        this(DEFAULT_MESSAGE);
    }

    public InvalidSearchParameterException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
