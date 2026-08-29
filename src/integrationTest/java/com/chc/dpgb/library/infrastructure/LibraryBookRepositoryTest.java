package com.chc.dpgb.library.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.chc.dpgb.RepositoryIntegrationTestSupport;
import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.ReadingStatus;
import com.chc.dpgb.library.domain.Shelf;

class LibraryBookRepositoryTest extends RepositoryIntegrationTestSupport {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final UUID MEMBER_2 = UUID.randomUUID();

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LibraryBookJpaRepository libraryBookRepository;

    @Autowired
    private ShelfJpaRepository shelfRepository;

    @Test
    void 등록한_책을_저장하고_조회할_수_있다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        LibraryBook book = LibraryBook.register(
                MEMBER_1, shelfId, "m", "어린 왕자", "생텍쥐페리", "9788932917245", null, "열린책들", null,
                "https://example.com/cover.jpg", null, 160
        );
        LibraryBook saved = libraryBookRepository.saveAndFlush(book);

        Optional<LibraryBook> found = libraryBookRepository.findById(saved.getBookId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("어린 왕자");
    }

    @Test
    void 지정한_genre와_readingStatus가_DB에_저장되고_다시_조회된다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        LibraryBook saved = libraryBookRepository.saveAndFlush(LibraryBook.register(
                MEMBER_1, shelfId, "m", "어린 왕자", "생텍쥐페리", "9788932917245", Genre.LITERARY_FICTION,
                "열린책들", null, null, ReadingStatus.READING, 160
        ));
        // 1차 캐시가 아니라 PostgreSQL enum 컬럼에 실제로 저장된 값을 읽는다
        entityManager.clear();

        LibraryBook found = libraryBookRepository.findById(saved.getBookId()).orElseThrow();

        assertThat(found.getGenre()).isEqualTo(Genre.LITERARY_FICTION);
        assertThat(found.getReadingStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    void genre와_readingStatus를_생략하면_기본값이_DB에_저장된다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        LibraryBook saved = libraryBookRepository.saveAndFlush(LibraryBook.register(
                MEMBER_1, shelfId, "m", "어린 왕자", "생텍쥐페리", null, null, null, null, null, null, 160
        ));
        entityManager.clear();

        LibraryBook found = libraryBookRepository.findById(saved.getBookId()).orElseThrow();

        assertThat(found.getGenre()).isEqualTo(Genre.NONE);
        assertThat(found.getReadingStatus()).isEqualTo(ReadingStatus.PLANNED);
    }

    @Test
    void 같은_책장에_같은_shelfRank로_등록하면_유일성_제약을_위반한다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelfId, "m", "책1", "저자1", null, null, null, null, null, null, 100)
        );
        LibraryBook duplicate = LibraryBook.register(
                MEMBER_1, shelfId, "m", "책2", "저자2", null, null, null, null, null, null, 100
        );

        assertThatThrownBy(() -> libraryBookRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_사용자가_같은_isbn으로_등록하면_유일성_제약을_위반한다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        libraryBookRepository.saveAndFlush(LibraryBook.register(
                MEMBER_1, shelfId, "m", "책1", "저자1", "9788932917245", null, null, null, null, null, 100
        ));
        LibraryBook duplicate = LibraryBook.register(
                MEMBER_1, shelfId, "n", "책2", "저자2", "9788932917245", null, null, null, null, null, 100
        );

        assertThatThrownBy(() -> libraryBookRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_책장은_같은_shelfRank를_각각_가질_수_있다() {
        Long shelf1 = shelf(MEMBER_1).getShelfId();
        Long shelf2 = shelf(MEMBER_2).getShelfId();
        libraryBookRepository.saveAndFlush(LibraryBook.register(
                MEMBER_1, shelf1, "m", "책1", "저자1", "9788932917245", null, null, null, null, null, 100
        ));
        LibraryBook other = LibraryBook.register(
                MEMBER_2, shelf2, "m", "책1", "저자1", "9788932917245", null, null, null, null, null, 100
        );

        LibraryBook saved = libraryBookRepository.saveAndFlush(other);

        assertThat(saved.getBookId()).isNotNull();
    }

    @Test
    void shelfRank_오름차순으로_책장_목록을_조회한다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelfId, "n", "두번째", "저자", null, null, null, null, null, null, 100)
        );
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelfId, "m", "첫번째", "저자", null, null, null, null, null, null, 100)
        );

        List<LibraryBook> books = libraryBookRepository.findByShelfIdOrderByShelfRankAsc(shelfId);

        assertThat(books).extracting(LibraryBook::getTitle).containsExactly("첫번째", "두번째");
    }

    @Test
    void 책장의_마지막_shelfRank를_조회한다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelfId, "m", "책1", "저자", null, null, null, null, null, null, 100)
        );
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelfId, "z", "책2", "저자", null, null, null, null, null, null, 100)
        );

        Optional<LibraryBook> last = libraryBookRepository.findTopByShelfIdOrderByShelfRankDesc(shelfId);

        assertThat(last).isPresent();
        assertThat(last.get().getShelfRank()).isEqualTo("z");
    }

    @Test
    void isbn으로_등록된_책을_조회한다() {
        Long shelfId = shelf(MEMBER_1).getShelfId();
        libraryBookRepository.saveAndFlush(LibraryBook.register(
                MEMBER_1, shelfId, "m", "책1", "저자", "9788932917245", null, null, null, null, null, 100
        ));

        assertThat(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, "9788932917245")).isPresent();
        assertThat(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, "9788932917246")).isEmpty();
    }

    @Test
    void 책장에_속한_책_수를_센다() {
        Long shelf1 = shelf(MEMBER_1).getShelfId();
        Long shelf2 = shelf(MEMBER_1).getShelfId();
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelf1, "m", "책1", "저자", null, null, null, null, null, null, 100)
        );
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelf1, "n", "책2", "저자", null, null, null, null, null, null, 100)
        );
        libraryBookRepository.saveAndFlush(
                LibraryBook.register(MEMBER_1, shelf2, "m", "책3", "저자", null, null, null, null, null, null, 100)
        );

        assertThat(libraryBookRepository.countByShelfId(shelf1)).isEqualTo(2);
        assertThat(libraryBookRepository.countByShelfId(shelf2)).isEqualTo(1);
    }

    private Shelf shelf(UUID memberId) {
        return shelfRepository.saveAndFlush(Shelf.create(memberId, "책장", false));
    }
}
