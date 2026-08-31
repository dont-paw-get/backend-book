package com.chc.dpgb.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    /**
     * 요청 DTO의 Bean Validation 실패를 endpoint별 계약 코드로 옮긴다 (ADR-0013). 매핑에 없는 요청 타입이면
     * 계약에 없는 400을 지어내지 않고 기존 500 경로로 흘려보낸다 — 새 요청 DTO에 매핑을 빠뜨린 사실이 드러나야 한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleRequestValidationFailure(MethodArgumentNotValidException ex) {
        Class<?> requestType = ex.getParameter().getParameterType();
        BadRequestException translated = RequestValidationFailureTranslator.translate(requestType);
        if (translated == null) {
            return handleUnexpected(ex);
        }
        return toResponse(HttpStatus.BAD_REQUEST, translated);
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
