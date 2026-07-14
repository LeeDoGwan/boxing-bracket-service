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
        validateName(name);
        this.name = name.trim();
        this.affiliation = normalizeAffiliation(affiliation);
    }

    public void update(String name, String affiliation) {
        validateName(name);
        this.name = name.trim();
        this.affiliation = normalizeAffiliation(affiliation);
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static String normalizeAffiliation(String affiliation) {
        if (affiliation == null) {
            return null;
        }
        return affiliation.trim();
    }
}
