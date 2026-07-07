package com.boxing.bracket.scoring.repository;

import com.boxing.bracket.scoring.domain.RoundScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundScoreRepository extends JpaRepository<RoundScore, Long> {

    List<RoundScore> findByBoutId(Long boutId);

    List<RoundScore> findByBoutIdAndJudgeId(Long boutId, Long judgeId);
}
