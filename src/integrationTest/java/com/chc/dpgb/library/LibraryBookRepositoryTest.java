package com.chc.dpgb.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.chc.dpgb.RepositoryIntegrationTestSupport;

class LibraryBookRepositoryTest extends RepositoryIntegrationTestSupport {

	@Autowired
	private LibraryBookJpaRepository libraryBookRepository;

	@Test
	void 등록한_책을_저장하고_소유자_기준으로_조회할_수_있다() {
		LibraryBook book = LibraryBook.register("member-1", "m", "어린 왕자", "생텍쥐페리", "9788932917245",
				"열린책들", null, "https://example.com/cover.jpg", 160);
		LibraryBook saved = libraryBookRepository.saveAndFlush(book);

		Optional<LibraryBook> found = libraryBookRepository.findByBookIdAndMemberId(saved.getBookId(), "member-1");

		assertThat(found).isPresent();
		assertThat(found.get().getTitle()).isEqualTo("어린 왕자");
	}

	@Test
	void 다른_사용자의_책은_조회되지_않는다() {
		LibraryBook saved = libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", "m", "어린 왕자", "생텍쥐페리", null, null, null, null, 160));

		Optional<LibraryBook> found = libraryBookRepository.findByBookIdAndMemberId(saved.getBookId(), "member-2");

		assertThat(found).isEmpty();
	}

	@Test
	void 같은_사용자가_같은_shelfRank로_등록하면_유일성_제약을_위반한다() {
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", "m", "책1", "저자1", null, null, null, null, 100));
		LibraryBook duplicate = LibraryBook.register("member-1", "m", "책2", "저자2", null, null, null, null, 100);

		assertThatThrownBy(() -> libraryBookRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 같은_사용자가_같은_isbn으로_등록하면_유일성_제약을_위반한다() {
		libraryBookRepository.saveAndFlush(LibraryBook.register(
				"member-1", "m", "책1", "저자1", "9788932917245", null, null, null, 100));
		LibraryBook duplicate = LibraryBook.register(
				"member-1", "n", "책2", "저자2", "9788932917245", null, null, null, 100);

		assertThatThrownBy(() -> libraryBookRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 다른_사용자는_같은_shelfRank와_isbn을_각각_등록할_수_있다() {
		libraryBookRepository.saveAndFlush(LibraryBook.register(
				"member-1", "m", "책1", "저자1", "9788932917245", null, null, null, 100));
		LibraryBook other = LibraryBook.register(
				"member-2", "m", "책1", "저자1", "9788932917245", null, null, null, 100);

		LibraryBook saved = libraryBookRepository.saveAndFlush(other);

		assertThat(saved.getBookId()).isNotNull();
	}

	@Test
	void shelfRank_오름차순으로_서재_목록을_조회한다() {
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", "n", "두번째", "저자", null, null, null, null, 100));
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", "m", "첫번째", "저자", null, null, null, null, 100));

		List<LibraryBook> books = libraryBookRepository.findByMemberIdOrderByShelfRankAsc("member-1");

		assertThat(books).extracting(LibraryBook::getTitle).containsExactly("첫번째", "두번째");
	}

	@Test
	void 서재의_마지막_shelfRank를_조회한다() {
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", "m", "책1", "저자", null, null, null, null, 100));
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", "z", "책2", "저자", null, null, null, null, 100));

		Optional<LibraryBook> last = libraryBookRepository.findTopByMemberIdOrderByShelfRankDesc("member-1");

		assertThat(last).isPresent();
		assertThat(last.get().getShelfRank()).isEqualTo("z");
	}

	@Test
	void isbn으로_중복_여부를_확인한다() {
		libraryBookRepository.saveAndFlush(LibraryBook.register(
				"member-1", "m", "책1", "저자", "9788932917245", null, null, null, 100));

		assertThat(libraryBookRepository.existsByMemberIdAndIsbn("member-1", "9788932917245")).isTrue();
		assertThat(libraryBookRepository.existsByMemberIdAndIsbn("member-1", "9788932917246")).isFalse();
	}
}
