package com.boxing.bracket.athlete.domain;

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
@Table(name = "athletes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Athlete extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String affiliation;

    @Builder
    private Athlete(String name, String affiliation) {
        this.name = name;
        this.affiliation = affiliation;
    }
}
