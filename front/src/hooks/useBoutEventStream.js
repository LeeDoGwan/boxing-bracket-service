import { useEffect, useRef, useState } from 'react';
import { eventStreamUrl } from '../api/audience';

const EVENT_TYPES = [
  'BOUT_STARTED',
  'BOUT_STATUS_CHANGED',
  'ROUND_STARTED',
  'NEXT_BOUT_READY',
  'SCORE_SUBMITTED',
  'RESULT_CONFIRMED',
];

export function useBoutEventStream(tournamentId, onEvent) {
  const eventHandler = useRef(onEvent);
  const seenEvents = useRef(new Set());
  const [connectionState, setConnectionState] = useState('connecting');

  useEffect(() => {
    eventHandler.current = onEvent;
  }, [onEvent]);

  useEffect(() => {
    if (!tournamentId || typeof EventSource === 'undefined') {
      setConnectionState('offline');
      return undefined;
    }

    const source = new EventSource(eventStreamUrl(tournamentId));
    const handleEvent = (event) => {
      try {
        const payload = JSON.parse(event.data);
        const key = `${payload.eventType}:${payload.boutId}:${payload.roundNo || ''}:${payload.occurredAt || ''}`;
        if (seenEvents.current.has(key)) {
          return;
        }
        seenEvents.current.add(key);
        if (seenEvents.current.size > 100) {
          seenEvents.current.clear();
        }
        eventHandler.current?.(payload);
      } catch {
        // Ignore malformed stream messages and preserve the existing screen state.
      }
    };

    source.onopen = () => setConnectionState('connected');
    source.onerror = () => setConnectionState('reconnecting');
    EVENT_TYPES.forEach((eventType) => source.addEventListener(eventType, handleEvent));
    source.onmessage = handleEvent;

    return () => source.close();
  }, [tournamentId]);

  return connectionState;
}
