package com.chc.dpgb.common.exception;

public abstract class ForbiddenException extends DomainException {

    protected ForbiddenException(String message) {
        super(message);
    }
}
