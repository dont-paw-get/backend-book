package com.chc.dpgb.library.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.chc.dpgb.RepositoryIntegrationTestSupport;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.Scrap;
import com.chc.dpgb.library.domain.Shelf;

class ScrapRepositoryTest extends RepositoryIntegrationTestSupport {

    @Autowired
    private ScrapJpaRepository scrapRepository;

    @Autowired
    private LibraryBookJpaRepository libraryBookRepository;

    @Autowired
    private ShelfJpaRepository shelfRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 등록한_스크랩을_저장하고_조회할_수_있다() {
        Long bookId = book("member-1").getBookId();
        Scrap saved = scrapRepository.saveAndFlush(Scrap.create(bookId, "문장", 12, "메모"));

        Optional<Scrap> found = scrapRepository.findById(saved.getScrapId());

        assertThat(found).isPresent();
        assertThat(found.get().getSentence()).isEqualTo("문장");
    }

    @Test
    void 책별_스크랩_목록을_생성_순서로_조회한다() {
        Long bookId = book("member-1").getBookId();
        scrapRepository.saveAndFlush(Scrap.create(bookId, "두번째", null, null));
        scrapRepository.saveAndFlush(Scrap.create(bookId, "첫번째", null, null));

        Page<Scrap> page = scrapRepository.findByBookIdOrderByCreatedAtAsc(bookId, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Scrap::getSentence).containsExactly("두번째", "첫번째");
    }

    @Test
    void 다른_책의_스크랩은_섞이지_않는다() {
        Long book1 = book("member-1").getBookId();
        Long book2 = book("member-1").getBookId();
        scrapRepository.saveAndFlush(Scrap.create(book1, "책1 문장", null, null));
        scrapRepository.saveAndFlush(Scrap.create(book2, "책2 문장", null, null));

        Page<Scrap> page = scrapRepository.findByBookIdOrderByCreatedAtAsc(book1, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Scrap::getSentence).containsExactly("책1 문장");
    }

    @Test
    void 책을_삭제하면_스크랩도_함께_삭제된다() {
        LibraryBook book = book("member-1");
        Scrap scrap = scrapRepository.saveAndFlush(Scrap.create(book.getBookId(), "문장", null, null));

        libraryBookRepository.delete(book);
        libraryBookRepository.flush();
        entityManager.clear();

        assertThat(scrapRepository.findById(scrap.getScrapId())).isEmpty();
    }

    private LibraryBook book(String memberId) {
        Long shelfId = shelfRepository.saveAndFlush(Shelf.create(memberId, "책장", false)).getShelfId();
        return libraryBookRepository.saveAndFlush(
                LibraryBook.register(memberId, shelfId, "m", "제목", "저자", null, null, null, null, 100)
        );
    }
}
