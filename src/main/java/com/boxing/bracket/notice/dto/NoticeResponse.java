package com.boxing.bracket.notice.dto;

import com.boxing.bracket.notice.domain.Notice;

public class NoticeResponse {

    private final Long noticeId;
    private final Long tournamentId;
    private final String title;
    private final String content;
    private final boolean active;
    private final int displayOrder;

    private NoticeResponse(
            Long noticeId,
            Long tournamentId,
            String title,
            String content,
            boolean active,
            int displayOrder
    ) {
        this.noticeId = noticeId;
        this.tournamentId = tournamentId;
        this.title = title;
        this.content = content;
        this.active = active;
        this.displayOrder = displayOrder;
    }

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTournamentId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isActive(),
                notice.getDisplayOrder()
        );
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
