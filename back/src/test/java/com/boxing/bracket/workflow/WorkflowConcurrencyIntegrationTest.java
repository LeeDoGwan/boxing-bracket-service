package com.boxing.bracket.workflow;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import com.boxing.bracket.ringmanager.service.RingManagerService;
import com.boxing.bracket.scoring.domain.DecisionType;
import com.boxing.bracket.scoring.dto.BoutResultConfirmRequest;
import com.boxing.bracket.scoring.dto.BoutResultResponse;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.dto.RoundScoreSubmitRequest;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import com.boxing.bracket.scoring.service.JudgeScoreService;
import com.boxing.bracket.scoring.service.SupervisorResultService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;

@SpringBootTest
@ActiveProfiles("test")
class WorkflowConcurrencyIntegrationTest {

    @Autowired
    private RingManagerService ringManagerService;

    @Autowired
    private JudgeScoreService judgeScoreService;

    @Autowired
    private SupervisorResultService supervisorResultService;

    @Autowired
    private RingRepository ringRepository;

    @Autowired
    private BoutRepository boutRepository;

    @Autowired
    private RoundScoreRepository roundScoreRepository;

    @Autowired
    private PenaltyRepository penaltyRepository;

    @Autowired
    private BoutResultRepository boutResultRepository;

    @SpyBean
    private BoutEventPublisher boutEventPublisher;

    @BeforeEach
    void setUp() {
        clearData();
        clearInvocations(boutEventPublisher);
    }

    @AfterEach
    void tearDown() {
        clearData();
    }

    @Test
    void concurrentStartRequestsStartOneBoutAndPublishOneEvent() throws Exception {
        Ring ring = createRing();
        Bout bout = createBout(ring.getId(), BoutStatus.READY);

        List<RingManagerBoutResponse> responses = executeConcurrently(() -> ringManagerService.startBout(bout.getId()));

        assertThat(responses).hasSize(2);
        assertThat(responses).allSatisfy(response -> assertThat(response.getStatus()).isEqualTo(BoutStatus.IN_PROGRESS));
        assertThat(boutRepository.findById(bout.getId()).orElseThrow().getStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
        assertThat(ringRepository.findById(ring.getId()).orElseThrow().getCurrentBoutId()).isEqualTo(bout.getId());
        then(boutEventPublisher).should(times(1)).publish(org.mockito.ArgumentMatchers.any(BoutEventResponse.class));
    }

    @Test
    void concurrentIdenticalScoreRequestsPersistOneScoreAndPublishOneEvent() throws Exception {
        Ring ring = createRing();
        Bout bout = createBout(ring.getId(), BoutStatus.IN_PROGRESS);
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(30L, 10, 9);

        List<RoundScoreResponse> responses = executeConcurrently(
                () -> judgeScoreService.submitRoundScore(bout.getId(), 1, request)
        );

        assertThat(responses).hasSize(2);
        assertThat(roundScoreRepository.findByBoutId(bout.getId())).hasSize(1);
        then(boutEventPublisher).should(times(1)).publish(org.mockito.ArgumentMatchers.any(BoutEventResponse.class));
    }

    @Test
    void concurrentIdenticalResultRequestsPersistOneResultAndPublishOneEvent() throws Exception {
        Ring ring = createRing();
        Bout bout = createBout(ring.getId(), BoutStatus.IN_PROGRESS);
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(BoutSide.RED, DecisionType.POINTS, 40L);

        List<BoutResultResponse> responses = executeConcurrently(
                () -> supervisorResultService.confirmResult(bout.getId(), request)
        );

        assertThat(responses).hasSize(2);
        assertThat(boutResultRepository.findAll()).hasSize(1);
        assertThat(boutRepository.findById(bout.getId()).orElseThrow().isResultConfirmed()).isTrue();
        then(boutEventPublisher).should(times(1)).publish(org.mockito.ArgumentMatchers.any(BoutEventResponse.class));
    }

    private Ring createRing() {
        return ringRepository.saveAndFlush(Ring.builder()
                .tournamentId(1L)
                .name("Ring A")
                .status(RingStatus.READY)
                .build());
    }

    private Bout createBout(Long ringId, BoutStatus status) {
        return boutRepository.saveAndFlush(Bout.builder()
                .tournamentId(1L)
                .ringId(ringId)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(status)
                .currentRound(status == BoutStatus.IN_PROGRESS ? 1 : 0)
                .scheduledOrder(1)
                .build());
    }

    private <T> List<T> executeConcurrently(RequestAction<T> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < 2; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("concurrent request start timed out");
                    }
                    return action.execute();
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private void clearData() {
        boutResultRepository.deleteAll();
        penaltyRepository.deleteAll();
        roundScoreRepository.deleteAll();
        boutRepository.deleteAll();
        ringRepository.deleteAll();
    }

    @FunctionalInterface
    private interface RequestAction<T> {
        T execute();
    }
}
