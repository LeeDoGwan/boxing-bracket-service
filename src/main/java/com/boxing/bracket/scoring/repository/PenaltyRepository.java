package com.boxing.bracket.scoring.repository;

import com.boxing.bracket.scoring.domain.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    List<Penalty> findByBoutId(Long boutId);
}
