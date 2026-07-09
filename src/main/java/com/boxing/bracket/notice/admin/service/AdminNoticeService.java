package com.boxing.bracket.notice.admin.service;

import com.boxing.bracket.notice.domain.Notice;
import com.boxing.bracket.notice.dto.NoticeRequest;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.exception.NoticeNotFoundException;
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
@Transactional
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;
    private final TournamentRepository tournamentRepository;

    public AdminNoticeService(NoticeRepository noticeRepository, TournamentRepository tournamentRepository) {
        this.noticeRepository = noticeRepository;
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getNotices(Long tournamentId) {
        validateTournamentId(tournamentId);
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }

        return noticeRepository.findByTournamentIdOrderByDisplayOrderAscIdAsc(tournamentId).stream()
                .map(NoticeResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoticeResponse getNotice(Long noticeId) {
        validateNoticeId(noticeId);
        return noticeRepository.findById(noticeId)
                .map(NoticeResponse::from)
                .orElseThrow(NoticeNotFoundException::new);
    }

    public NoticeResponse createNotice(NoticeRequest request) {
        validateRequest(request);

        Notice notice = Notice.builder()
                .tournamentId(request.getTournamentId())
                .title(request.getTitle())
                .content(request.getContent())
                .active(request.getActive())
                .displayOrder(request.getDisplayOrder())
                .build();

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    public NoticeResponse updateNotice(Long noticeId, NoticeRequest request) {
        validateNoticeId(noticeId);
        validateRequest(request);

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
        notice.updateInfo(
                request.getTournamentId(),
                request.getTitle(),
                request.getContent(),
                request.getActive(),
                request.getDisplayOrder()
        );

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    public void deleteNotice(Long noticeId) {
        validateNoticeId(noticeId);
        if (!noticeRepository.existsById(noticeId)) {
            throw new NoticeNotFoundException();
        }

        noticeRepository.deleteById(noticeId);
    }

    private void validateRequest(NoticeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("notice request is required");
        }
        validateTournamentId(request.getTournamentId());
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("title is required");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("content is required");
        }
        if (!tournamentRepository.existsById(request.getTournamentId())) {
            throw new TournamentNotFoundException();
        }
    }

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }

    private void validateNoticeId(Long noticeId) {
        if (noticeId == null) {
            throw new IllegalArgumentException("noticeId is required");
        }
    }
}
