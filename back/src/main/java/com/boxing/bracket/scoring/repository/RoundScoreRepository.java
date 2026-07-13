package com.boxing.bracket.scoring.repository;

import com.boxing.bracket.scoring.domain.RoundScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoundScoreRepository extends JpaRepository<RoundScore, Long> {

    List<RoundScore> findByBoutId(Long boutId);

    List<RoundScore> findByBoutIdIn(Collection<Long> boutIds);

    List<RoundScore> findByBoutIdOrderByRoundNoAscJudgeIdAsc(Long boutId);

    List<RoundScore> findByBoutIdAndJudgeId(Long boutId, Long judgeId);

    Optional<RoundScore> findByBoutIdAndRoundNoAndJudgeId(Long boutId, Integer roundNo, Long judgeId);
}
