package com.boxing.bracket.event.service;

import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Lazy
@Transactional(readOnly = true)
public class BoutEventStreamService {

    private final BoutEventPublisher boutEventPublisher;
    private final TournamentRepository tournamentRepository;

    public BoutEventStreamService(BoutEventPublisher boutEventPublisher, TournamentRepository tournamentRepository) {
        this.boutEventPublisher = boutEventPublisher;
        this.tournamentRepository = tournamentRepository;
    }

    public SseEmitter subscribe(Long tournamentId, Long ringId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }

        return boutEventPublisher.subscribe(tournamentId, ringId);
    }
}
