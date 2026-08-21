package com.chc.dpgb.library.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.common.exception.InvalidBookDataException;
import com.chc.dpgb.common.exception.InvalidFilterParameterException;
import com.chc.dpgb.common.exception.InvalidPageValueException;
import com.chc.dpgb.common.exception.InvalidShelfTargetException;
import com.chc.dpgb.library.LibraryBook;
import com.chc.dpgb.library.LibraryBookService;
import com.chc.dpgb.library.LibrarySortBy;
import com.chc.dpgb.library.web.dto.CreateLibraryBookRequest;
import com.chc.dpgb.library.web.dto.CreateLibraryBookResponse;
import com.chc.dpgb.library.web.dto.LibraryBookDetailResponse;
import com.chc.dpgb.library.web.dto.LibraryBookPageResponse;
import com.chc.dpgb.library.web.dto.MoveLibraryBookToShelfRequest;
import com.chc.dpgb.library.web.dto.MoveLibraryBookToShelfResponse;
import com.chc.dpgb.library.web.dto.ReorderLibraryBookRequest;
import com.chc.dpgb.library.web.dto.ReorderLibraryBookResponse;
import com.chc.dpgb.library.web.dto.UpdateLibraryBookRequest;
import com.chc.dpgb.library.web.dto.UpdateLibraryBookResponse;
import com.chc.dpgb.library.web.dto.UpdateReadingProgressRequest;
import com.chc.dpgb.library.web.dto.UpdateReadingProgressResponse;
import com.chc.dpgb.security.MemberIdResolver;

@RestController
@RequestMapping("/api/v1/library/books")
public class LibraryBookController {

	private final LibraryBookService libraryBookService;

	LibraryBookController(LibraryBookService libraryBookService) {
		this.libraryBookService = libraryBookService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreateLibraryBookResponse createLibraryBook(
			@AuthenticationPrincipal Jwt jwt, @RequestBody CreateLibraryBookRequest request) {
		String memberId = MemberIdResolver.resolve(jwt);
		LibraryBook book = libraryBookService.createLibraryBook(
				memberId, request.shelfId(), request.title(), request.author(), request.isbn(),
				request.publisher(), request.publishedDate(), request.coverUrl(), requireTotalPages(request.totalPages()));
		return CreateLibraryBookResponse.from(book);
	}

	@GetMapping
	public LibraryBookPageResponse getLibraryBooks(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Long shelfId,
			@RequestParam(required = false) String author,
			@RequestParam(required = false, defaultValue = "SHELF_ORDER") String sortBy,
			@RequestParam(required = false, defaultValue = "ASC") String sortOrder,
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "20") int size) {
		String memberId = MemberIdResolver.resolve(jwt);
		LibrarySortBy resolvedSortBy = parseSortBy(sortBy);
		Sort.Direction direction = parseSortOrder(sortOrder);
		validatePaging(page, size);
		Page<LibraryBook> result = libraryBookService.getLibraryBooks(
				memberId, shelfId, author, resolvedSortBy, direction, page, size);
		return LibraryBookPageResponse.from(result);
	}

	@GetMapping("/{bookId}")
	public LibraryBookDetailResponse getLibraryBook(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId) {
		String memberId = MemberIdResolver.resolve(jwt);
		return LibraryBookDetailResponse.from(libraryBookService.getLibraryBook(memberId, bookId));
	}

	@PatchMapping("/{bookId}")
	public UpdateLibraryBookResponse updateLibraryBook(
			@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId,
			@RequestBody UpdateLibraryBookRequest request) {
		String memberId = MemberIdResolver.resolve(jwt);
		LibraryBook book = libraryBookService.updateLibraryBook(
				memberId, bookId, request.title(), request.author(), request.isbn(), request.publisher(),
				request.publishedDate(), request.coverUrl(), requireTotalPages(request.totalPages()));
		return UpdateLibraryBookResponse.from(book);
	}

	@DeleteMapping("/{bookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteLibraryBook(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId) {
		String memberId = MemberIdResolver.resolve(jwt);
		libraryBookService.deleteLibraryBook(memberId, bookId);
	}

	@PatchMapping("/{bookId}/order")
	public ReorderLibraryBookResponse reorderLibraryBook(
			@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId,
			@RequestBody ReorderLibraryBookRequest request) {
		String memberId = MemberIdResolver.resolve(jwt);
		LibraryBook book = libraryBookService.reorderLibraryBook(
				memberId, bookId, request.beforeBookId(), request.afterBookId());
		return ReorderLibraryBookResponse.from(book);
	}

	@PatchMapping("/{bookId}/shelf")
	public MoveLibraryBookToShelfResponse moveLibraryBookToShelf(
			@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId,
			@RequestBody MoveLibraryBookToShelfRequest request) {
		String memberId = MemberIdResolver.resolve(jwt);
		if (request.shelfId() == null) {
			throw new InvalidShelfTargetException();
		}
		LibraryBook book = libraryBookService.moveLibraryBookToShelf(memberId, bookId, request.shelfId());
		return MoveLibraryBookToShelfResponse.from(book);
	}

	@PatchMapping("/{bookId}/progress")
	public UpdateReadingProgressResponse updateReadingProgress(
			@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId,
			@RequestBody UpdateReadingProgressRequest request) {
		String memberId = MemberIdResolver.resolve(jwt);
		if (request.currentPage() == null || request.totalPages() == null) {
			throw new InvalidPageValueException();
		}
		LibraryBook book = libraryBookService.updateReadingProgress(
				memberId, bookId, request.currentPage(), request.totalPages());
		return UpdateReadingProgressResponse.from(book);
	}

	private static int requireTotalPages(Integer totalPages) {
		if (totalPages == null) {
			throw new InvalidBookDataException();
		}
		return totalPages;
	}

	static LibrarySortBy parseSortBy(String value) {
		try {
			return LibrarySortBy.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidFilterParameterException();
		}
	}

	static Sort.Direction parseSortOrder(String value) {
		try {
			return Sort.Direction.fromString(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidFilterParameterException();
		}
	}

	static void validatePaging(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new InvalidFilterParameterException();
		}
	}
}
