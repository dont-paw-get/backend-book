package com.chc.dpgb.common.exception;

public class ShelfAccessDeniedException extends ForbiddenException {

	private static final String CODE = "SHELF_ACCESS_DENIED";
	private static final String DEFAULT_MESSAGE = "해당 책장에 접근할 권한이 없습니다.";

	public ShelfAccessDeniedException() {
		this(DEFAULT_MESSAGE);
	}

	public ShelfAccessDeniedException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
