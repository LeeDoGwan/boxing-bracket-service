package com.boxing.bracket.event.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.event.domain.BoutEventType;
import com.boxing.bracket.event.dto.BoutEventResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class BoutEventPublisherTest {

    @Test
    void publishSendsOnlyMatchingTournamentAndRingSubscribers() {
        BoutEventPublisher publisher = new BoutEventPublisher();
        RecordingEmitter matchingEmitter = new RecordingEmitter();
        RecordingEmitter otherRingEmitter = new RecordingEmitter();
        RecordingEmitter tournamentEmitter = new RecordingEmitter();

        publisher.subscribe(1L, 2L, matchingEmitter);
        publisher.subscribe(1L, 3L, otherRingEmitter);
        publisher.subscribe(1L, null, tournamentEmitter);

        publisher.publish(BoutEventResponse.of(BoutEventType.BOUT_STARTED, createBout(10L)));

        assertThat(matchingEmitter.getSendCount()).isEqualTo(1);
        assertThat(otherRingEmitter.getSendCount()).isZero();
        assertThat(tournamentEmitter.getSendCount()).isEqualTo(1);
        assertThat(publisher.subscriberCount()).isEqualTo(3);
    }

    @Test
    void publishRemovesBrokenSubscriber() {
        BoutEventPublisher publisher = new BoutEventPublisher();
        BrokenEmitter brokenEmitter = new BrokenEmitter();
        publisher.subscribe(1L, 2L, brokenEmitter);

        publisher.publish(BoutEventResponse.of(BoutEventType.BOUT_STARTED, createBout(10L)));

        assertThat(publisher.subscriberCount()).isZero();
    }

    private Bout createBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(2L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.READY)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private static class RecordingEmitter extends SseEmitter {

        private int sendCount;

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            sendCount++;
        }

        private int getSendCount() {
            return sendCount;
        }
    }

    private static class BrokenEmitter extends SseEmitter {

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            throw new IOException("broken");
        }
    }
}
