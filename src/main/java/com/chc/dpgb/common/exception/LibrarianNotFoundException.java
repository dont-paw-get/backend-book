package com.chc.dpgb.common.exception;

public class LibrarianNotFoundException extends NotFoundException {

	private static final String CODE = "LIBRARIAN_NOT_FOUND";
	private static final String DEFAULT_MESSAGE = "선택할 수 없는 사서입니다.";

	public LibrarianNotFoundException() {
		this(DEFAULT_MESSAGE);
	}

	public LibrarianNotFoundException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
