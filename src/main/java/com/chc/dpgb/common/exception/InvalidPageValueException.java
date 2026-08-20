package com.chc.dpgb.common.exception;

public class InvalidPageValueException extends BadRequestException {

	private static final String CODE = "INVALID_PAGE_VALUE";
	private static final String DEFAULT_MESSAGE = "현재 페이지와 전체 페이지 값을 확인해야 합니다.";

	public InvalidPageValueException() {
		this(DEFAULT_MESSAGE);
	}

	public InvalidPageValueException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
