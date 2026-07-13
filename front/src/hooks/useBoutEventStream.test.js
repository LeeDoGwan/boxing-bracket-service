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
    this.listeners[type]?.({ data: JSON.stringify(payload) });
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
  });
});
