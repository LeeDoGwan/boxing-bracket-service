package com.boxing.bracket.scoring.repository;

import com.boxing.bracket.scoring.domain.BoutResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoutResultRepository extends JpaRepository<BoutResult, Long> {

    Optional<BoutResult> findByBoutId(Long boutId);
}
