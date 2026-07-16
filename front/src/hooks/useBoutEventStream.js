import { useEffect, useRef, useState } from 'react';
import { eventStreamUrl } from '../api/audience';

export const DEFAULT_EVENT_TYPES = [
  'BOUT_STARTED',
  'BOUT_STATUS_CHANGED',
  'ROUND_STARTED',
  'NEXT_BOUT_READY',
  'SCORE_SUBMITTED',
  'RESULT_CONFIRMED',
];

export function useBoutEventStream(tournamentId, onEventOrOptions) {
  const options = typeof onEventOrOptions === 'function'
    ? { enabled: true, eventTypes: DEFAULT_EVENT_TYPES, onEvent: onEventOrOptions, ringId: null }
    : {
      enabled: onEventOrOptions?.enabled !== false,
      eventTypes: onEventOrOptions?.eventTypes || DEFAULT_EVENT_TYPES,
      onEvent: onEventOrOptions?.onEvent,
      ringId: onEventOrOptions?.ringId ?? null,
  };
  const eventHandler = useRef(options.onEvent);
  const eventTypesRef = useRef(options.eventTypes);
  const [connectionState, setConnectionState] = useState('offline');
  const eventTypeKey = options.eventTypes.join('|');

  useEffect(() => {
    eventHandler.current = options.onEvent;
  }, [options.onEvent]);

  useEffect(() => {
    eventTypesRef.current = options.eventTypes;
  }, [eventTypeKey, options.eventTypes]);

  useEffect(() => {
    if (!tournamentId || !options.enabled || typeof EventSource === 'undefined') {
      setConnectionState('offline');
      return undefined;
    }

    let active = true;
    const seenEvents = new Set();
    const eventTypes = eventTypesRef.current;
    const allowedEventTypes = new Set(eventTypes);
    setConnectionState('connecting');
    const source = new EventSource(eventStreamUrl(tournamentId, options.ringId));
    const handleEvent = (event) => {
      if (!active) {
        return;
      }
      try {
        const payload = JSON.parse(event.data);
        if (payload.eventType && !allowedEventTypes.has(payload.eventType)) {
          return;
        }
        const key = `${event.lastEventId || ''}:${payload.eventType || event.type}:${payload.boutId || ''}:${payload.roundNo || ''}:${payload.occurredAt || ''}`;
        if (seenEvents.has(key)) {
          return;
        }
        seenEvents.add(key);
        if (seenEvents.size > 100) {
          const oldest = seenEvents.values().next().value;
          seenEvents.delete(oldest);
        }
        eventHandler.current?.(payload);
      } catch {
        // Ignore malformed stream messages and preserve the existing screen state.
      }
    };

    source.onopen = () => active && setConnectionState('connected');
    source.onerror = () => active && setConnectionState('reconnecting');
    source.addEventListener('bout-update', handleEvent);
    eventTypes.forEach((eventType) => source.addEventListener(eventType, handleEvent));
    source.onmessage = handleEvent;

    return () => {
      active = false;
      source.close();
    };
  }, [eventTypeKey, options.enabled, options.ringId, tournamentId]);

  return connectionState;
}
