package com.boxing.bracket.audit.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.notice.domain.Notice;
import com.boxing.bracket.notice.repository.NoticeRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuditStateSnapshotService {

    private static final Pattern BOUT_PATH = Pattern.compile("/bouts/(\\d+)(?:/|$)");
    private static final Pattern RING_PATH = Pattern.compile("/rings/(\\d+)(?:/|$)");
    private static final Pattern ACCOUNT_PATH = Pattern.compile("/accounts/(\\d+)(?:/|$)");
    private static final Pattern NOTICE_PATH = Pattern.compile("/notices/(\\d+)(?:/|$)");

    private final BoutRepository boutRepository;
    private final RingRepository ringRepository;
    private final AccountRepository accountRepository;
    private final NoticeRepository noticeRepository;
    private final AuditDataSerializer serializer;

    public AuditStateSnapshotService(
            BoutRepository boutRepository,
            RingRepository ringRepository,
            AccountRepository accountRepository,
            NoticeRepository noticeRepository,
            AuditDataSerializer serializer
    ) {
        this.boutRepository = boutRepository;
        this.ringRepository = ringRepository;
        this.accountRepository = accountRepository;
        this.noticeRepository = noticeRepository;
        this.serializer = serializer;
    }

    public AuditSnapshot capture(String requestUri) {
        Long boutId = findId(BOUT_PATH, requestUri);
        if (boutId != null) {
            return boutRepository.findById(boutId)
                    .map(this::fromBout)
                    .orElse(new AuditSnapshot(null, null, boutId, boutId, null));
        }

        Long ringId = findId(RING_PATH, requestUri);
        if (ringId != null) {
            return ringRepository.findById(ringId)
                    .map(this::fromRing)
                    .orElse(new AuditSnapshot(null, ringId, null, ringId, null));
        }

        Long accountId = findId(ACCOUNT_PATH, requestUri);
        if (accountId != null) {
            return accountRepository.findById(accountId)
                    .map(this::fromAccount)
                    .orElse(new AuditSnapshot(null, null, null, accountId, null));
        }

        Long noticeId = findId(NOTICE_PATH, requestUri);
        if (noticeId != null) {
            return noticeRepository.findById(noticeId)
                    .map(this::fromNotice)
                    .orElse(new AuditSnapshot(null, null, null, noticeId, null));
        }

        return AuditSnapshot.empty();
    }

    private AuditSnapshot fromBout(Bout bout) {
        return new AuditSnapshot(
                bout.getTournamentId(),
                bout.getRingId(),
                bout.getId(),
                bout.getId(),
                serializer.serialize(bout)
        );
    }

    private AuditSnapshot fromRing(Ring ring) {
        return new AuditSnapshot(
                ring.getTournamentId(),
                ring.getId(),
                ring.getCurrentBoutId(),
                ring.getId(),
                serializer.serialize(ring)
        );
    }

    private AuditSnapshot fromAccount(Account account) {
        return new AuditSnapshot(null, null, null, account.getId(), serializer.serialize(account));
    }

    private AuditSnapshot fromNotice(Notice notice) {
        return new AuditSnapshot(
                notice.getTournamentId(),
                null,
                null,
                notice.getId(),
                serializer.serialize(notice)
        );
    }

    private Long findId(Pattern pattern, String requestUri) {
        if (requestUri == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(requestUri);
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }
}
