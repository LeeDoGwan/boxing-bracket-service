package com.boxing.bracket.event.service;

import com.boxing.bracket.event.dto.BoutEventResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Lazy
public class BoutEventPublisher {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(Long tournamentId, Long ringId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        subscribe(tournamentId, ringId, emitter);
        return emitter;
    }

    void subscribe(Long tournamentId, Long ringId, SseEmitter emitter) {
        Subscription subscription = new Subscription(tournamentId, ringId, emitter);
        subscriptions.add(subscription);
        emitter.onCompletion(() -> subscriptions.remove(subscription));
        emitter.onTimeout(() -> subscriptions.remove(subscription));
        emitter.onError(error -> subscriptions.remove(subscription));
    }

    public void publish(BoutEventResponse event) {
        if (event == null) {
            return;
        }

        subscriptions.stream()
                .filter(subscription -> subscription.matches(event))
                .forEach(subscription -> send(subscription, event));
    }

    int subscriberCount() {
        return subscriptions.size();
    }

    private void send(Subscription subscription, BoutEventResponse event) {
        try {
            subscription.getEmitter().send(SseEmitter.event()
                    .name("bout-update")
                    .data(event));
        } catch (IOException | IllegalStateException exception) {
            subscriptions.remove(subscription);
        }
    }

    private static class Subscription {

        private final Long tournamentId;
        private final Long ringId;
        private final SseEmitter emitter;

        private Subscription(Long tournamentId, Long ringId, SseEmitter emitter) {
            this.tournamentId = tournamentId;
            this.ringId = ringId;
            this.emitter = emitter;
        }

        private boolean matches(BoutEventResponse event) {
            if (!tournamentId.equals(event.getTournamentId())) {
                return false;
            }
            return ringId == null || ringId.equals(event.getRingId());
        }

        private SseEmitter getEmitter() {
            return emitter;
        }
    }
}
