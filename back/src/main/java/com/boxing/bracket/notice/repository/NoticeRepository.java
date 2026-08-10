package com.boxing.bracket.notice.repository;

import com.boxing.bracket.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    boolean existsByTournamentId(Long tournamentId);

    List<Notice> findByTournamentIdOrderByDisplayOrderAscIdAsc(Long tournamentId);

    List<Notice> findByTournamentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long tournamentId);
}
