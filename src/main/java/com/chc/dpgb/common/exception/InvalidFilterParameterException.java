package com.chc.dpgb.common.exception;

public class InvalidFilterParameterException extends BadRequestException {

	private static final String CODE = "INVALID_FILTER_PARAMETER";
	private static final String DEFAULT_MESSAGE = "올바르지 않은 필터 또는 정렬 조건입니다.";

	public InvalidFilterParameterException() {
		this(DEFAULT_MESSAGE);
	}

	public InvalidFilterParameterException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
