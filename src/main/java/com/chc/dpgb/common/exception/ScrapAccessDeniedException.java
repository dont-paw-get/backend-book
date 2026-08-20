package com.chc.dpgb.common.exception;

public class ScrapAccessDeniedException extends ForbiddenException {

	private static final String CODE = "SCRAP_ACCESS_DENIED";
	private static final String DEFAULT_MESSAGE = "해당 스크랩에 접근할 권한이 없습니다.";

	public ScrapAccessDeniedException() {
		this(DEFAULT_MESSAGE);
	}

	public ScrapAccessDeniedException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
