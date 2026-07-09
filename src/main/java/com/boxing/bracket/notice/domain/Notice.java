package com.boxing.bracket.notice.domain;

import com.boxing.bracket.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tournamentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int displayOrder = 0;

    @Builder
    private Notice(Long tournamentId, String title, String content, Boolean active, Integer displayOrder) {
        validate(tournamentId, title, content);
        this.tournamentId = tournamentId;
        this.title = title.trim();
        this.content = content.trim();
        this.active = active == null || active;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
    }

    public void updateInfo(Long tournamentId, String title, String content, Boolean active, Integer displayOrder) {
        validate(tournamentId, title, content);
        this.tournamentId = tournamentId;
        this.title = title.trim();
        this.content = content.trim();
        if (active != null) {
            this.active = active;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    private void validate(Long tournamentId, String title, String content) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }
    }
}
