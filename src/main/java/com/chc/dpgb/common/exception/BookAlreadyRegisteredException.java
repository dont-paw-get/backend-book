package com.chc.dpgb.common.exception;

public class BookAlreadyRegisteredException extends ConflictException {

	private static final String CODE = "BOOK_ALREADY_REGISTERED";
	private static final String DEFAULT_MESSAGE = "이미 서재에 등록된 도서입니다.";

	public BookAlreadyRegisteredException() {
		this(DEFAULT_MESSAGE);
	}

	public BookAlreadyRegisteredException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
