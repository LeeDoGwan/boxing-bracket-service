package com.boxing.bracket.scoring.repository;

import com.boxing.bracket.scoring.domain.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
}
