package com.chc.dpgb.common.exception;

public class LibraryBookNotFoundException extends NotFoundException {

	private static final String CODE = "LIBRARY_BOOK_NOT_FOUND";
	private static final String DEFAULT_MESSAGE = "서재에서 해당 도서를 찾을 수 없습니다.";

	public LibraryBookNotFoundException() {
		this(DEFAULT_MESSAGE);
	}

	public LibraryBookNotFoundException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
