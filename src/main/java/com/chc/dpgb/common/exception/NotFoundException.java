package com.chc.dpgb.common.exception;

public abstract class NotFoundException extends DomainException {

	protected NotFoundException(String message) {
		super(message);
	}
}
