package com.chc.dpgb.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.chc.dpgb.common.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        log.info("권한 거부로 403 응답 code={}", ex.code());
        return toResponse(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return toResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        log.info("비즈니스 충돌로 409 응답 code={}", ex.code());
        return toResponse(HttpStatus.CONFLICT, ex);
    }

    /**
     * 일반적인 400/404는 계약상 클라이언트 오류라 개별 로그를 남기지 않는다. 운영상 의미가 있는 403/409와 외부 연동 실패인
     * 502만 code 중심으로 남긴다. 요청 본문, Authorization 헤더, token, 사용자 식별자는 남기지 않는다.
     */
    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<ErrorResponse> handleBadGateway(BadGatewayException ex) {
        log.warn("외부 연동 실패로 502 응답 code={}", ex.code());
        return toResponse(HttpStatus.BAD_GATEWAY, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("처리되지 않은 예외로 500 응답", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }

    private ResponseEntity<ErrorResponse> toResponse(HttpStatus status, DomainException ex) {
        return ResponseEntity.status(status).body(new ErrorResponse(ex.code(), ex.getMessage()));
    }
}
