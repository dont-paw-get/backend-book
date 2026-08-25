package com.chc.dpgb.common.exception;

public class RepresentativeLibrarianNotSelectedException extends NotFoundException {

    private static final String CODE = "REPRESENTATIVE_LIBRARIAN_NOT_SELECTED";
    private static final String DEFAULT_MESSAGE = "대표 사서를 아직 선택하지 않았습니다.";

    public RepresentativeLibrarianNotSelectedException() {
        this(DEFAULT_MESSAGE);
    }

    public RepresentativeLibrarianNotSelectedException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return CODE;
    }
}
