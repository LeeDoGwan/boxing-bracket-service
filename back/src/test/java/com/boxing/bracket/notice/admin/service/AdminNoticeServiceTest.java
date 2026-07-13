package com.boxing.bracket.notice.admin.service;

import com.boxing.bracket.notice.domain.Notice;
import com.boxing.bracket.notice.dto.NoticeRequest;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.exception.NoticeNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminNoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private AdminNoticeService adminNoticeService;

    @Test
    void getNoticesReturnsTournamentNotices() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(noticeRepository.findByTournamentIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(createNotice(10L), createNotice(11L)));

        List<NoticeResponse> responses = adminNoticeService.getNotices(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getNoticeId()).isEqualTo(10L);
    }

    @Test
    void getNoticeReturnsNotice() {
        given(noticeRepository.findById(10L)).willReturn(Optional.of(createNotice(10L)));

        NoticeResponse response = adminNoticeService.getNotice(10L);

        assertThat(response.getNoticeId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Notice 10");
    }

    @Test
    void getNoticeRejectsMissingNotice() {
        given(noticeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminNoticeService.getNotice(99L))
                .isInstanceOf(NoticeNotFoundException.class)
                .hasMessage("Notice not found");
    }

    @Test
    void createNoticeSavesNotice() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(noticeRepository.save(any(Notice.class))).willAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            ReflectionTestUtils.setField(notice, "id", 10L);
            return notice;
        });

        NoticeResponse response = adminNoticeService.createNotice(request());

        assertThat(response.getNoticeId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Notice");
        assertThat(response.isActive()).isTrue();
        assertThat(response.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void createNoticeRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminNoticeService.createNotice(
                new NoticeRequest(99L, "Notice", "Content", true, 1)
        ))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void createNoticeRejectsBlankContent() {
        assertThatThrownBy(() -> adminNoticeService.createNotice(
                new NoticeRequest(1L, "Notice", " ", true, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content is required");
    }

    @Test
    void updateNoticeChangesNotice() {
        Notice notice = createNotice(10L);
        NoticeRequest request = new NoticeRequest(1L, "Updated", "Updated Content", false, 3);
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(noticeRepository.findById(10L)).willReturn(Optional.of(notice));
        given(noticeRepository.save(any(Notice.class))).willAnswer(invocation -> invocation.getArgument(0));

        NoticeResponse response = adminNoticeService.updateNotice(10L, request);

        assertThat(response.getNoticeId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Updated");
        assertThat(response.isActive()).isFalse();
        assertThat(response.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void updateNoticeRejectsMissingNotice() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(noticeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminNoticeService.updateNotice(99L, request()))
                .isInstanceOf(NoticeNotFoundException.class)
                .hasMessage("Notice not found");
    }

    @Test
    void deleteNoticeDeletesExistingNotice() {
        given(noticeRepository.existsById(10L)).willReturn(true);

        adminNoticeService.deleteNotice(10L);

        then(noticeRepository).should().deleteById(10L);
    }

    @Test
    void deleteNoticeRejectsMissingNotice() {
        given(noticeRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminNoticeService.deleteNotice(99L))
                .isInstanceOf(NoticeNotFoundException.class)
                .hasMessage("Notice not found");
    }

    private NoticeRequest request() {
        return new NoticeRequest(1L, " Notice ", " Content ", true, 1);
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
