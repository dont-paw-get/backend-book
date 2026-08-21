package com.chc.dpgb.common.exception;

public class DefaultShelfCannotBeDeletedException extends BadRequestException {

	private static final String CODE = "DEFAULT_SHELF_CANNOT_BE_DELETED";
	private static final String DEFAULT_MESSAGE = "기본 책장은 삭제할 수 없습니다.";

	public DefaultShelfCannotBeDeletedException() {
		this(DEFAULT_MESSAGE);
	}

	public DefaultShelfCannotBeDeletedException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
