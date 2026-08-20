package com.chc.dpgb.common.exception;

public class AladinApiException extends BadGatewayException {

	private static final String CODE = "ALADIN_API_ERROR";
	private static final String DEFAULT_MESSAGE = "외부 도서 정보 조회 중 오류가 발생했습니다.";

	public AladinApiException() {
		this(DEFAULT_MESSAGE);
	}

	public AladinApiException(String message) {
		super(message);
	}

	@Override
	public String code() {
		return CODE;
	}
}
