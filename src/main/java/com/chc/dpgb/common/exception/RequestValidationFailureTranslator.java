package com.chc.dpgb.common.exception;

import java.util.Map;
import java.util.function.Supplier;

import com.chc.dpgb.librarian.web.dto.AcquireLibrarianRequest;
import com.chc.dpgb.librarian.web.dto.RenameLibrarianRequest;
import com.chc.dpgb.library.web.dto.CreateLibraryBookRequest;
import com.chc.dpgb.library.web.dto.CreateScrapRequest;
import com.chc.dpgb.library.web.dto.CreateShelfRequest;
import com.chc.dpgb.library.web.dto.MoveLibraryBookToShelfRequest;
import com.chc.dpgb.library.web.dto.ReorderLibraryBookRequest;
import com.chc.dpgb.library.web.dto.UpdateLibraryBookRequest;
import com.chc.dpgb.library.web.dto.UpdateReadingProgressRequest;
import com.chc.dpgb.library.web.dto.UpdateScrapRequest;
import com.chc.dpgb.library.web.dto.UpdateShelfRequest;

/**
 * Bean Validation 실패를 요청 DTO별 도메인 예외로 옮긴다.
 *
 * <p>{@code openapi.yaml}은 400을 endpoint별로 다른 코드({@code INVALID_BOOK_DATA}/{@code INVALID_SCRAP_DATA}/
 * {@code INVALID_SHELF_DATA}/{@code INVALID_LIBRARIAN_DATA}/{@code INVALID_PAGE_VALUE}/
 * {@code INVALID_REORDER_TARGET}/{@code INVALID_SHELF_TARGET})로 규정하는데, Bean Validation이 던지는
 * {@code MethodArgumentNotValidException}은 하나뿐이라 그것만으로는 코드를 고를 수 없다. 그래서 요청 DTO 타입을
 * 열쇠로 삼는다 (ADR-0013).
 *
 * <p>이 매핑은 {@code common.exception}이 web DTO 타입을 알게 되는 대신, 새 요청 DTO가 생겼을 때 컴파일러가 아니라
 * {@code RequestValidationFailureTranslatorTest}가 누락을 잡아준다. 계약이 한 곳에 모여 있는 편이 endpoint마다
 * 흩어 두는 것보다 낫다고 판단했다.
 */
public final class RequestValidationFailureTranslator {

    private static final Map<Class<?>, Supplier<BadRequestException>> BY_REQUEST_TYPE = Map.ofEntries(
            Map.entry(CreateLibraryBookRequest.class, InvalidBookDataException::new),
            Map.entry(UpdateLibraryBookRequest.class, InvalidBookDataException::new),
            Map.entry(ReorderLibraryBookRequest.class, InvalidReorderTargetException::new),
            Map.entry(MoveLibraryBookToShelfRequest.class, InvalidShelfTargetException::new),
            Map.entry(UpdateReadingProgressRequest.class, InvalidPageValueException::new),
            Map.entry(CreateShelfRequest.class, InvalidShelfDataException::new),
            Map.entry(UpdateShelfRequest.class, InvalidShelfDataException::new),
            Map.entry(CreateScrapRequest.class, InvalidScrapDataException::new),
            Map.entry(UpdateScrapRequest.class, InvalidScrapDataException::new),
            Map.entry(AcquireLibrarianRequest.class, InvalidLibrarianDataException::new),
            Map.entry(RenameLibrarianRequest.class, InvalidLibrarianDataException::new)
    );

    private RequestValidationFailureTranslator() {
    }

    /**
     * 매핑에 없는 타입이면 {@code null}을 돌려준다 — 호출자가 "계약에 없는 400"과 구분해 처리할 수 있게 한다.
     */
    public static BadRequestException translate(Class<?> requestType) {
        Supplier<BadRequestException> supplier = BY_REQUEST_TYPE.get(requestType);
        return supplier == null ? null : supplier.get();
    }

    public static boolean supports(Class<?> requestType) {
        return BY_REQUEST_TYPE.containsKey(requestType);
    }
}
