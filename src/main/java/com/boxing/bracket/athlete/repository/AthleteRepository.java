package com.boxing.bracket.athlete.repository;

import com.boxing.bracket.athlete.domain.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
}
