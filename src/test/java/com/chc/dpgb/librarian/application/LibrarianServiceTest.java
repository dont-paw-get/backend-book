package com.chc.dpgb.librarian.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import com.chc.dpgb.common.exception.InvalidLibrarianDataException;
import com.chc.dpgb.common.exception.LibrarianAccessDeniedException;
import com.chc.dpgb.common.exception.LibrarianAlreadyOwnedException;
import com.chc.dpgb.common.exception.LibrarianNotFoundException;
import com.chc.dpgb.common.exception.RepresentativeLibrarianNotSelectedException;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;
import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

@ExtendWith(MockitoExtension.class)
class LibrarianServiceTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final UUID MEMBER_2 = UUID.randomUUID();

    @Mock
    private LibrarianRepository librarianRepository;

    @Mock
    private LibrarianTypeInfoRepository librarianTypeInfoRepository;

    private LibrarianService librarianService;

    @BeforeEach
    void setUp() {
        librarianService = new LibrarianService(librarianRepository, librarianTypeInfoRepository);
    }

    private static Librarian librarian(Long librarianId, UUID memberId, LibrarianType type, String name) {
        Librarian librarian = Librarian.acquire(memberId, type, name);
        try {
            var field = Librarian.class.getDeclaredField("librarianId");
            field.setAccessible(true);
            field.set(librarian, librarianId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return librarian;
    }

    @Test
    void 사서_타입_카탈로그를_조회한다() {
        LibrarianTypeInfo typeInfo = new LibrarianTypeInfo(
                LibrarianType.RUSSIAN_BLUE, "https://example.com/cat.png", "https://example.com/cat-clicked.png"
        );
        when(librarianTypeInfoRepository.findAll()).thenReturn(List.of(typeInfo));

        assertThat(librarianService.getLibrarianTypes()).containsExactly(typeInfo);
    }

    @Test
    void 사서를_획득한다() {
        when(librarianRepository.existsByMemberIdAndType(MEMBER_1, LibrarianType.RUSSIAN_BLUE)).thenReturn(false);
        when(librarianRepository.save(any(Librarian.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Librarian result = librarianService.acquireLibrarian(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");

        assertThat(result.getName()).isEqualTo("나비");
        assertThat(result.getType()).isEqualTo(LibrarianType.RUSSIAN_BLUE);
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getExperience()).isEqualTo(0L);
        assertThat(result.isRepresentative()).isFalse();
    }

    @Test
    void 사서_획득_로그에는_memberId를_남기지_않는다() {
        ListAppender<ILoggingEvent> logs = captureLogs();
        try {
            Librarian saved = librarian(7L, MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
            when(librarianRepository.existsByMemberIdAndType(MEMBER_1, LibrarianType.RUSSIAN_BLUE)).thenReturn(false);
            when(librarianRepository.save(any(Librarian.class))).thenReturn(saved);

            librarianService.acquireLibrarian(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");

            assertThat(logMessages(logs))
                    .containsExactly("사서 획득 librarianId=7 type=RUSSIAN_BLUE")
                    .allSatisfy(message -> assertThat(message)
                            .doesNotContain("memberId")
                            .doesNotContain(MEMBER_1.toString()));
        } finally {
            detach(logs);
        }
    }

    @Test
    void 사서_획득_경합_로그에는_memberId를_남기지_않는다() {
        ListAppender<ILoggingEvent> logs = captureLogs();
        try {
            when(librarianRepository.existsByMemberIdAndType(MEMBER_1, LibrarianType.RUSSIAN_BLUE)).thenReturn(false);
            when(librarianRepository.save(any(Librarian.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> librarianService.acquireLibrarian(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비"))
                    .isInstanceOf(LibrarianAlreadyOwnedException.class);

            assertThat(logMessages(logs))
                    .containsExactly("사서 획득 경합으로 중복 판정 type=RUSSIAN_BLUE")
                    .allSatisfy(message -> assertThat(message)
                            .doesNotContain("memberId")
                            .doesNotContain(MEMBER_1.toString()));
        } finally {
            detach(logs);
        }
    }

    @Test
    void 같은_타입을_이미_보유하면_409() {
        when(librarianRepository.existsByMemberIdAndType(MEMBER_1, LibrarianType.RUSSIAN_BLUE)).thenReturn(true);

        assertThatThrownBy(() -> librarianService.acquireLibrarian(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비"))
                .isInstanceOf(LibrarianAlreadyOwnedException.class);
    }

    @Test
    void 이름이_비어있으면_획득을_거부한다() {
        when(librarianRepository.existsByMemberIdAndType(MEMBER_1, LibrarianType.RUSSIAN_BLUE)).thenReturn(false);

        assertThatThrownBy(() -> librarianService.acquireLibrarian(MEMBER_1, LibrarianType.RUSSIAN_BLUE, " "))
                .isInstanceOf(InvalidLibrarianDataException.class);
    }

    @Test
    void 보유한_사서_목록을_조회한다() {
        Librarian owned = librarian(1L, MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        when(librarianRepository.findAllOwned(MEMBER_1)).thenReturn(List.of(owned));

        assertThat(librarianService.getLibrarians(MEMBER_1)).containsExactly(owned);
    }

    @Test
    void 사서_이름을_바꾼다() {
        Librarian owned = librarian(1L, MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        when(librarianRepository.findById(1L)).thenReturn(Optional.of(owned));
        when(librarianRepository.save(any(Librarian.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Librarian result = librarianService.renameLibrarian(MEMBER_1, 1L, "루루");

        assertThat(result.getName()).isEqualTo("루루");
    }

    @Test
    void 존재하지_않는_사서를_수정하면_404() {
        when(librarianRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> librarianService.renameLibrarian(MEMBER_1, 1L, "루루"))
                .isInstanceOf(LibrarianNotFoundException.class);
    }

    @Test
    void 다른_사용자의_사서를_수정하면_403() {
        Librarian owned = librarian(1L, MEMBER_2, LibrarianType.RUSSIAN_BLUE, "나비");
        when(librarianRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> librarianService.renameLibrarian(MEMBER_1, 1L, "루루"))
                .isInstanceOf(LibrarianAccessDeniedException.class);
    }

    @Test
    void 처음_대표_사서를_지정한다() {
        Librarian target = librarian(1L, MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        when(librarianRepository.findById(1L)).thenReturn(Optional.of(target));
        when(librarianRepository.findRepresentative(MEMBER_1)).thenReturn(Optional.empty());
        when(librarianRepository.save(any(Librarian.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Librarian result = librarianService.selectRepresentative(MEMBER_1, 1L);

        assertThat(result.isRepresentative()).isTrue();
    }

    @Test
    void 대표_사서를_다른_사서로_바꾸면_기존_사서는_해제된다() {
        Librarian previous = librarian(1L, MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        previous.markAsRepresentative();
        Librarian target = librarian(2L, MEMBER_1, LibrarianType.SHOEBILL, "부엉");
        when(librarianRepository.findById(2L)).thenReturn(Optional.of(target));
        when(librarianRepository.findRepresentative(MEMBER_1)).thenReturn(Optional.of(previous));
        when(librarianRepository.save(any(Librarian.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Librarian result = librarianService.selectRepresentative(MEMBER_1, 2L);

        assertThat(result.isRepresentative()).isTrue();
        assertThat(previous.isRepresentative()).isFalse();
    }

    @Test
    void 대표_사서를_조회한다() {
        Librarian representative = librarian(1L, MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        representative.markAsRepresentative();
        when(librarianRepository.findRepresentative(MEMBER_1)).thenReturn(Optional.of(representative));

        assertThat(librarianService.getRepresentative(MEMBER_1)).isEqualTo(representative);
    }

    @Test
    void 대표_사서를_아직_선택하지_않았으면_404() {
        when(librarianRepository.findRepresentative(MEMBER_1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> librarianService.getRepresentative(MEMBER_1))
                .isInstanceOf(RepresentativeLibrarianNotSelectedException.class);
    }

    @Test
    void 사서를_방출한다() {
        Librarian owned = librarian(1L, MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        when(librarianRepository.findById(1L)).thenReturn(Optional.of(owned));
        when(librarianRepository.save(any(Librarian.class))).thenAnswer(invocation -> invocation.getArgument(0));

        librarianService.deleteLibrarian(MEMBER_1, 1L);

        assertThat(owned.isDeleted()).isTrue();
    }

    private static ListAppender<ILoggingEvent> captureLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(LibrarianService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detach(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(LibrarianService.class);
        logger.detachAppender(appender);
    }

    private static List<String> logMessages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
