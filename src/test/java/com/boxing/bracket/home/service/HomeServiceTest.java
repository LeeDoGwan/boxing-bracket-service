package com.boxing.bracket.home.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.service.BoutService;
import com.boxing.bracket.home.dto.HomeResponse;
import com.boxing.bracket.notice.domain.Notice;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.service.NoticeService;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import com.boxing.bracket.ring.service.RingService;
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
class HomeServiceTest {

    @Mock
    private RingService ringService;

    @Mock
    private BoutService boutService;

    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private HomeService homeService;

    @Test
    void getHomeReturnsRingStatusesAndConfirmedResults() {
        RingStatusResponse ringStatus = RingStatusResponse.of(createRing(1L), null, null);
        BoutListResponse confirmedBout = BoutListResponse.of(
                createConfirmedBout(10L),
                createAthlete(10L, "Hong Gil Dong"),
                createAthlete(11L, "Kim Chul Soo")
        );
        BoutListResponse scheduledBout = BoutListResponse.of(
                createScheduledBout(11L),
                createAthlete(12L, "Park Min Soo"),
                createAthlete(13L, "Lee Jun Ho")
        );
        NoticeResponse notice = NoticeResponse.from(createNotice(1L));
        given(ringService.getRingStatuses(1L)).willReturn(List.of(ringStatus));
        given(boutService.getOfficialBouts(1L)).willReturn(List.of(confirmedBout, scheduledBout));
        given(noticeService.getActiveNotices(1L)).willReturn(List.of(notice));

        HomeResponse response = homeService.getHome(1L);

        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getNotices()).hasSize(1);
        assertThat(response.getNotices().get(0).getNoticeId()).isEqualTo(1L);
        assertThat(response.getRingStatuses()).hasSize(1);
        assertThat(response.getConfirmedResults()).hasSize(1);
        assertThat(response.getConfirmedResults().get(0).getBoutId()).isEqualTo(10L);
    }

    @Test
    void getHomeRejectsNullTournamentId() {
        assertThatThrownBy(() -> homeService.getHome(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    private Ring createRing(Long id) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring " + id)
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }

    private Bout createConfirmedBout(Long id) {
        Bout bout = createBout(id, BoutStatus.FINISHED);
        bout.confirmResult(BoutSide.RED);
        return bout;
    }

    private Bout createScheduledBout(Long id) {
        return createBout(id, BoutStatus.SCHEDULED);
    }

    private Bout createBout(Long id, BoutStatus status) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(id.intValue())
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(status)
                .scheduledOrder(id.intValue())
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private Athlete createAthlete(Long id, String name) {
        Athlete athlete = Athlete.builder()
                .name(name)
                .affiliation("Club " + id)
                .build();
        ReflectionTestUtils.setField(athlete, "id", id);
        return athlete;
    }

    private Notice createNotice(Long id) {
        Notice notice = Notice.builder()
                .tournamentId(1L)
                .title("Notice " + id)
                .content("Content " + id)
                .active(true)
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }
}
