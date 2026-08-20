package com.chc.dpgb.common.exception;

public class InvalidBookDataException extends BadRequestException {

	private static final String CODE = "INVALID_BOOK_DATA";
	private static final String DEFAULT_MESSAGE = "올바르지 않은 도서 정보입니다.";

	public InvalidBookDataException() {
		this(DEFAULT_MESSAGE);
	}

	public InvalidBookDataException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
