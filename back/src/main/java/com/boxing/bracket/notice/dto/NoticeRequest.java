package com.boxing.bracket.notice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class NoticeRequest {

    @NotNull(message = "tournamentId is required")
    private Long tournamentId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;

    private Boolean active;

    private Integer displayOrder;

    protected NoticeRequest() {
    }

    public NoticeRequest(Long tournamentId, String title, String content, Boolean active, Integer displayOrder) {
        this.tournamentId = tournamentId;
        this.title = title;
        this.content = content;
        this.active = active;
        this.displayOrder = displayOrder;
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

    public Boolean getActive() {
        return active;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}
