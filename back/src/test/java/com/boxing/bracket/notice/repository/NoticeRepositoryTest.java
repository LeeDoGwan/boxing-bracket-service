package com.boxing.bracket.notice.repository;

import com.boxing.bracket.notice.domain.Notice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    void findsActiveNoticesByTournamentOrderedByDisplayOrder() {
        Notice second = noticeRepository.save(Notice.builder()
                .tournamentId(1L)
                .title("Second")
                .content("Second content")
                .active(true)
                .displayOrder(2)
                .build());
        Notice first = noticeRepository.save(Notice.builder()
                .tournamentId(1L)
                .title("First")
                .content("First content")
                .active(true)
                .displayOrder(1)
                .build());
        noticeRepository.save(Notice.builder()
                .tournamentId(1L)
                .title("Hidden")
                .content("Hidden content")
                .active(false)
                .displayOrder(0)
                .build());
        noticeRepository.flush();

        List<Notice> notices = noticeRepository.findByTournamentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L);

        assertThat(notices).extracting(Notice::getId)
                .containsExactly(first.getId(), second.getId());
    }
}
