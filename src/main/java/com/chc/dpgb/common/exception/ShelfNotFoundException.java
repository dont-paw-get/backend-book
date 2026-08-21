package com.chc.dpgb.common.exception;

public class ShelfNotFoundException extends NotFoundException {

	private static final String CODE = "SHELF_NOT_FOUND";
	private static final String DEFAULT_MESSAGE = "책장을 찾을 수 없습니다.";

	public ShelfNotFoundException() {
		this(DEFAULT_MESSAGE);
	}

	public ShelfNotFoundException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
