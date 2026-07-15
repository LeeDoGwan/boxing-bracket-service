import { act, renderHook } from '@testing-library/react';
import { useBoutEventStream } from './useBoutEventStream';

class MockEventSource {
  static instances = [];

  constructor(url) {
    this.listeners = {};
    this.url = url;
    this.close = vi.fn();
    MockEventSource.instances.push(this);
  }

  addEventListener(type, listener) {
    this.listeners[type] = listener;
  }

  emit(type, payload) {
    this.listeners[type]?.({ data: JSON.stringify(payload), lastEventId: '' });
  }

  emitRaw(type, data) {
    this.listeners[type]?.({ data, lastEventId: '' });
  }
}

describe('useBoutEventStream', () => {
  beforeEach(() => {
    MockEventSource.instances = [];
    vi.stubGlobal('EventSource', MockEventSource);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('deduplicates events and closes the source on unmount', () => {
    const onEvent = vi.fn();
    const { unmount } = renderHook(() => useBoutEventStream(1, onEvent));
    const source = MockEventSource.instances[0];
    const payload = { boutId: 12, eventType: 'BOUT_STARTED', occurredAt: '2026-07-12T00:00:00' };

    act(() => {
      source.emit('BOUT_STARTED', payload);
      source.emit('BOUT_STARTED', payload);
    });

    expect(onEvent).toHaveBeenCalledOnce();
    unmount();
    expect(source.close).toHaveBeenCalledOnce();
    source.emit('BOUT_STARTED', { ...payload, occurredAt: '2026-07-12T00:01:00' });
    expect(onEvent).toHaveBeenCalledOnce();
  });

  it('scopes the stream to a ring and closes the previous source when the ring changes', () => {
    const onEvent = vi.fn();
    const { rerender } = renderHook(({ ringId }) => useBoutEventStream(1, { onEvent, ringId }), { initialProps: { ringId: 4 } });
    const firstSource = MockEventSource.instances[0];
    expect(firstSource.url).toContain('tournamentId=1');
    expect(firstSource.url).toContain('ringId=4');

    rerender({ ringId: 5 });

    expect(firstSource.close).toHaveBeenCalledOnce();
    expect(MockEventSource.instances[1].url).toContain('ringId=5');
  });

  it('filters event types, ignores malformed JSON, and supports the named backend event', () => {
    const onEvent = vi.fn();
    renderHook(() => useBoutEventStream(1, { eventTypes: ['RESULT_CONFIRMED'], onEvent, ringId: 4 }));
    const source = MockEventSource.instances[0];

    act(() => {
      source.emit('bout-update', { boutId: 12, eventType: 'BOUT_STARTED' });
      source.emitRaw('bout-update', '{invalid');
      source.emit('bout-update', { boutId: 12, eventType: 'RESULT_CONFIRMED' });
    });

    expect(onEvent).toHaveBeenCalledOnce();
    expect(onEvent).toHaveBeenCalledWith({ boutId: 12, eventType: 'RESULT_CONFIRMED' });
  });

  it('does not create a source when disabled or when no ring is selected', () => {
    const { result } = renderHook(() => useBoutEventStream(1, { enabled: false, onEvent: vi.fn(), ringId: null }));

    expect(MockEventSource.instances).toHaveLength(0);
    expect(result.current).toBe('offline');
  });
});
