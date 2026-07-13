package com.boxing.bracket.notice.service;

import com.boxing.bracket.notice.domain.Notice;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.repository.NoticeRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    void getActiveNoticesReturnsPublicNotices() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(noticeRepository.findByTournamentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(createNotice(10L)));

        List<NoticeResponse> responses = noticeService.getActiveNotices(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getNoticeId()).isEqualTo(10L);
        assertThat(responses.get(0).getTitle()).isEqualTo("Notice 10");
    }

    @Test
    void getActiveNoticesRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> noticeService.getActiveNotices(99L))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void getActiveNoticesRejectsNullTournamentId() {
        assertThatThrownBy(() -> noticeService.getActiveNotices(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    private Notice createNotice(Long id) {
        Notice notice = Notice.builder()
                .tournamentId(1L)
                .title("Notice " + id)
                .content("Content " + id)
                .active(true)
                .displayOrder(id.intValue())
                .build();
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }
}
