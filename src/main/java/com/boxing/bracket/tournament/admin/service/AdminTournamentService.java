package com.boxing.bracket.tournament.admin.service;

import com.boxing.bracket.tournament.admin.dto.AdminTournamentRequest;
import com.boxing.bracket.tournament.admin.dto.AdminTournamentResponse;
import com.boxing.bracket.tournament.domain.Tournament;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class AdminTournamentService {

    private final TournamentRepository tournamentRepository;

    public AdminTournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTournamentResponse> getTournaments() {
        return tournamentRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(AdminTournamentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminTournamentResponse getTournament(Long tournamentId) {
        validateTournamentId(tournamentId);
        return tournamentRepository.findById(tournamentId)
                .map(AdminTournamentResponse::from)
                .orElseThrow(TournamentNotFoundException::new);
    }

    public AdminTournamentResponse createTournament(AdminTournamentRequest request) {
        validateRequest(request);
        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .location(request.getLocation())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .build();

        return AdminTournamentResponse.from(tournamentRepository.save(tournament));
    }

    public AdminTournamentResponse updateTournament(Long tournamentId, AdminTournamentRequest request) {
        validateTournamentId(tournamentId);
        validateRequest(request);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(TournamentNotFoundException::new);
        tournament.updateInfo(
                request.getName(),
                request.getLocation(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus()
        );

        return AdminTournamentResponse.from(tournamentRepository.save(tournament));
    }

    public void deleteTournament(Long tournamentId) {
        validateTournamentId(tournamentId);
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }

        tournamentRepository.deleteById(tournamentId);
    }

    private void validateRequest(AdminTournamentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("tournament request is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }
}
