package com.boxing.bracket.athlete.repository;

import com.boxing.bracket.athlete.domain.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    List<Athlete> findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase(
            String name,
            String affiliation
    );
}
