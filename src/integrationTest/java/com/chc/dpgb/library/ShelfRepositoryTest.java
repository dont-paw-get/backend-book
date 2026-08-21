package com.chc.dpgb.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.chc.dpgb.RepositoryIntegrationTestSupport;

class ShelfRepositoryTest extends RepositoryIntegrationTestSupport {

	@Autowired
	private ShelfJpaRepository shelfRepository;

	@Test
	void 등록한_책장을_저장하고_조회할_수_있다() {
		Shelf saved = shelfRepository.saveAndFlush(Shelf.create("member-1", "기본 책장", true));

		Optional<Shelf> found = shelfRepository.findById(saved.getShelfId());

		assertThat(found).isPresent();
		assertThat(found.get().getName()).isEqualTo("기본 책장");
	}

	@Test
	void 같은_사용자가_기본_책장을_두_개_가질_수_없다() {
		shelfRepository.saveAndFlush(Shelf.create("member-1", "기본 책장", true));
		Shelf duplicateDefault = Shelf.create("member-1", "다른 기본 책장", true);

		assertThatThrownBy(() -> shelfRepository.saveAndFlush(duplicateDefault))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 기본_책장이_아니면_여러_개_만들_수_있다() {
		shelfRepository.saveAndFlush(Shelf.create("member-1", "기본 책장", true));
		shelfRepository.saveAndFlush(Shelf.create("member-1", "완독한 책", false));
		Shelf third = Shelf.create("member-1", "읽고 싶은 책", false);

		Shelf saved = shelfRepository.saveAndFlush(third);

		assertThat(saved.getShelfId()).isNotNull();
	}

	@Test
	void 다른_사용자는_각자_기본_책장을_가질_수_있다() {
		shelfRepository.saveAndFlush(Shelf.create("member-1", "기본 책장", true));
		Shelf other = Shelf.create("member-2", "기본 책장", true);

		Shelf saved = shelfRepository.saveAndFlush(other);

		assertThat(saved.getShelfId()).isNotNull();
	}

	@Test
	void 사용자의_기본_책장을_조회한다() {
		shelfRepository.saveAndFlush(Shelf.create("member-1", "완독한 책", false));
		shelfRepository.saveAndFlush(Shelf.create("member-1", "기본 책장", true));

		Optional<Shelf> found = shelfRepository.findByMemberIdAndIsDefaultTrue("member-1");

		assertThat(found).isPresent();
		assertThat(found.get().getName()).isEqualTo("기본 책장");
	}

	@Test
	void 사용자의_책장_목록을_기본_책장_우선으로_조회한다() {
		shelfRepository.saveAndFlush(Shelf.create("member-1", "완독한 책", false));
		shelfRepository.saveAndFlush(Shelf.create("member-1", "기본 책장", true));

		List<Shelf> shelves = shelfRepository.findByMemberIdOrderByIsDefaultDescCreatedAtAsc("member-1");

		assertThat(shelves).extracting(Shelf::getName).containsExactly("기본 책장", "완독한 책");
	}
}
