package com.boxing.bracket.notice.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoticeTest {

    @Test
    void createsNoticeWithDefaults() {
        Notice notice = Notice.builder()
                .tournamentId(1L)
                .title(" Notice ")
                .content(" Content ")
                .build();

        assertThat(notice.getTournamentId()).isEqualTo(1L);
        assertThat(notice.getTitle()).isEqualTo("Notice");
        assertThat(notice.getContent()).isEqualTo("Content");
        assertThat(notice.isActive()).isTrue();
        assertThat(notice.getDisplayOrder()).isZero();
    }

    @Test
    void updateInfoChangesNotice() {
        Notice notice = Notice.builder()
                .tournamentId(1L)
                .title("Notice")
                .content("Content")
                .build();

        notice.updateInfo(2L, " Updated ", " Updated Content ", false, 5);

        assertThat(notice.getTournamentId()).isEqualTo(2L);
        assertThat(notice.getTitle()).isEqualTo("Updated");
        assertThat(notice.getContent()).isEqualTo("Updated Content");
        assertThat(notice.isActive()).isFalse();
        assertThat(notice.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Notice.builder()
                .tournamentId(1L)
                .title(" ")
                .content("Content")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is required");
    }
}
