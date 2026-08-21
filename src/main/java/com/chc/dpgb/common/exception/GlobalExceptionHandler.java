package com.chc.dpgb.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.chc.dpgb.common.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return toResponse(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return toResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return toResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<ErrorResponse> handleBadGateway(BadGatewayException ex) {
        return toResponse(HttpStatus.BAD_GATEWAY, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }

    private ResponseEntity<ErrorResponse> toResponse(HttpStatus status, DomainException ex) {
        return ResponseEntity.status(status).body(new ErrorResponse(ex.code(), ex.getMessage()));
    }
}
