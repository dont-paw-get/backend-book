package com.chc.dpgb.common.exception;

public class InvalidShelfDataException extends BadRequestException {

	private static final String CODE = "INVALID_SHELF_DATA";
	private static final String DEFAULT_MESSAGE = "올바르지 않은 책장 정보입니다.";

	public InvalidShelfDataException() {
		this(DEFAULT_MESSAGE);
	}

	public InvalidShelfDataException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
