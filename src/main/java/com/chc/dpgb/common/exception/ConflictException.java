package com.chc.dpgb.common.exception;

public abstract class ConflictException extends DomainException {

    protected ConflictException(String message) {
        super(message);
    }
}
