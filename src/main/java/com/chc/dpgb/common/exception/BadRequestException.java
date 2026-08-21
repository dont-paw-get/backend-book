package com.chc.dpgb.common.exception;

public abstract class BadRequestException extends DomainException {

    protected BadRequestException(String message) {
        super(message);
    }
}
