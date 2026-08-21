package com.chc.dpgb.common.exception;

public class InvalidReorderTargetException extends BadRequestException {

	private static final String CODE = "INVALID_REORDER_TARGET";
	private static final String DEFAULT_MESSAGE = "기준으로 지정한 책을 찾을 수 없거나 같은 책장에 속하지 않습니다.";

	public InvalidReorderTargetException() {
		this(DEFAULT_MESSAGE);
	}

	public InvalidReorderTargetException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
