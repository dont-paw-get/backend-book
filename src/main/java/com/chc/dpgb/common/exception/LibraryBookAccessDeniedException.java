package com.chc.dpgb.common.exception;

public class LibraryBookAccessDeniedException extends ForbiddenException {

	private static final String CODE = "LIBRARY_BOOK_ACCESS_DENIED";
	private static final String DEFAULT_MESSAGE = "해당 도서에 접근할 권한이 없습니다.";

	public LibraryBookAccessDeniedException() {
		this(DEFAULT_MESSAGE);
	}

	public LibraryBookAccessDeniedException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
