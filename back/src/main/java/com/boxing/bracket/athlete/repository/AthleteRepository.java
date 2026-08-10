package com.boxing.bracket.athlete.repository;

import com.boxing.bracket.athlete.domain.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    List<Athlete> findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase(
            String name,
            String affiliation
    );

    List<Athlete> findByTournamentIdOrderByIdAsc(Long tournamentId);

    @Query("select athlete from Athlete athlete "
            + "where athlete.tournamentId = :tournamentId "
            + "and (lower(athlete.name) like lower(concat('%', :keyword, '%')) "
            + "or lower(athlete.affiliation) like lower(concat('%', :keyword, '%'))) "
            + "order by athlete.id asc")
    List<Athlete> searchByTournamentId(
            @Param("tournamentId") Long tournamentId,
            @Param("keyword") String keyword
    );

    Optional<Athlete> findByIdAndTournamentId(Long athleteId, Long tournamentId);

    boolean existsByIdAndTournamentId(Long athleteId, Long tournamentId);
}
