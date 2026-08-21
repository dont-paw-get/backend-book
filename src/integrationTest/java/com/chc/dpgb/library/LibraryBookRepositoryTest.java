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

	@Autowired
	private ShelfJpaRepository shelfRepository;

	@Test
	void 등록한_책을_저장하고_조회할_수_있다() {
		Long shelfId = shelf("member-1").getShelfId();
		LibraryBook book = LibraryBook.register("member-1", shelfId, "m", "어린 왕자", "생텍쥐페리", "9788932917245",
				"열린책들", null, "https://example.com/cover.jpg", 160);
		LibraryBook saved = libraryBookRepository.saveAndFlush(book);

		Optional<LibraryBook> found = libraryBookRepository.findById(saved.getBookId());

		assertThat(found).isPresent();
		assertThat(found.get().getTitle()).isEqualTo("어린 왕자");
	}

	@Test
	void 같은_책장에_같은_shelfRank로_등록하면_유일성_제약을_위반한다() {
		Long shelfId = shelf("member-1").getShelfId();
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelfId, "m", "책1", "저자1", null, null, null, null, 100));
		LibraryBook duplicate = LibraryBook.register("member-1", shelfId, "m", "책2", "저자2", null, null, null, null, 100);

		assertThatThrownBy(() -> libraryBookRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 같은_사용자가_같은_isbn으로_등록하면_유일성_제약을_위반한다() {
		Long shelfId = shelf("member-1").getShelfId();
		libraryBookRepository.saveAndFlush(LibraryBook.register(
				"member-1", shelfId, "m", "책1", "저자1", "9788932917245", null, null, null, 100));
		LibraryBook duplicate = LibraryBook.register(
				"member-1", shelfId, "n", "책2", "저자2", "9788932917245", null, null, null, 100);

		assertThatThrownBy(() -> libraryBookRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 다른_책장은_같은_shelfRank를_각각_가질_수_있다() {
		Long shelf1 = shelf("member-1").getShelfId();
		Long shelf2 = shelf("member-2").getShelfId();
		libraryBookRepository.saveAndFlush(LibraryBook.register(
				"member-1", shelf1, "m", "책1", "저자1", "9788932917245", null, null, null, 100));
		LibraryBook other = LibraryBook.register(
				"member-2", shelf2, "m", "책1", "저자1", "9788932917245", null, null, null, 100);

		LibraryBook saved = libraryBookRepository.saveAndFlush(other);

		assertThat(saved.getBookId()).isNotNull();
	}

	@Test
	void shelfRank_오름차순으로_책장_목록을_조회한다() {
		Long shelfId = shelf("member-1").getShelfId();
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelfId, "n", "두번째", "저자", null, null, null, null, 100));
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelfId, "m", "첫번째", "저자", null, null, null, null, 100));

		List<LibraryBook> books = libraryBookRepository.findByShelfIdOrderByShelfRankAsc(shelfId);

		assertThat(books).extracting(LibraryBook::getTitle).containsExactly("첫번째", "두번째");
	}

	@Test
	void 책장의_마지막_shelfRank를_조회한다() {
		Long shelfId = shelf("member-1").getShelfId();
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelfId, "m", "책1", "저자", null, null, null, null, 100));
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelfId, "z", "책2", "저자", null, null, null, null, 100));

		Optional<LibraryBook> last = libraryBookRepository.findTopByShelfIdOrderByShelfRankDesc(shelfId);

		assertThat(last).isPresent();
		assertThat(last.get().getShelfRank()).isEqualTo("z");
	}

	@Test
	void isbn으로_중복_여부를_확인한다() {
		Long shelfId = shelf("member-1").getShelfId();
		libraryBookRepository.saveAndFlush(LibraryBook.register(
				"member-1", shelfId, "m", "책1", "저자", "9788932917245", null, null, null, 100));

		assertThat(libraryBookRepository.existsByMemberIdAndIsbn("member-1", "9788932917245")).isTrue();
		assertThat(libraryBookRepository.existsByMemberIdAndIsbn("member-1", "9788932917246")).isFalse();
	}

	@Test
	void 책장에_속한_책_수를_센다() {
		Long shelf1 = shelf("member-1").getShelfId();
		Long shelf2 = shelf("member-1").getShelfId();
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelf1, "m", "책1", "저자", null, null, null, null, 100));
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelf1, "n", "책2", "저자", null, null, null, null, 100));
		libraryBookRepository.saveAndFlush(
				LibraryBook.register("member-1", shelf2, "m", "책3", "저자", null, null, null, null, 100));

		assertThat(libraryBookRepository.countByShelfId(shelf1)).isEqualTo(2);
		assertThat(libraryBookRepository.countByShelfId(shelf2)).isEqualTo(1);
	}

	private Shelf shelf(String memberId) {
		return shelfRepository.saveAndFlush(Shelf.create(memberId, "책장", false));
	}
}
