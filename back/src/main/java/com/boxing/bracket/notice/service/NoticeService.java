package com.boxing.bracket.notice.service;

import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.repository.NoticeRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final TournamentRepository tournamentRepository;

    public NoticeService(NoticeRepository noticeRepository, TournamentRepository tournamentRepository) {
        this.noticeRepository = noticeRepository;
        this.tournamentRepository = tournamentRepository;
    }

    public List<NoticeResponse> getActiveNotices(Long tournamentId) {
        validateTournamentId(tournamentId);
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }

        return noticeRepository.findByTournamentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(tournamentId).stream()
                .map(NoticeResponse::from)
                .collect(Collectors.toList());
    }

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }
}
