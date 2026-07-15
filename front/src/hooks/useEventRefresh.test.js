import { act, renderHook, waitFor } from '@testing-library/react';
import { useEventRefresh } from './useEventRefresh';

describe('useEventRefresh', () => {
  it('debounces repeated events into one refresh', async () => {
    const refresh = vi.fn().mockResolvedValue(undefined);
    const { result } = renderHook(() => useEventRefresh(refresh, 10));

    act(() => {
      result.current();
      result.current();
      result.current();
    });
    await waitFor(() => expect(refresh).toHaveBeenCalledOnce());
  });

  it('queues one refresh while a previous refresh is in flight', async () => {
    let resolveRefresh;
    const refresh = vi.fn().mockImplementation(() => new Promise((resolve) => { resolveRefresh = resolve; }));
    const { result } = renderHook(() => useEventRefresh(refresh, 10));

    act(() => result.current());
    await waitFor(() => expect(refresh).toHaveBeenCalledOnce());
    act(() => result.current());
    expect(refresh).toHaveBeenCalledOnce();
    await act(async () => { resolveRefresh(); });
    await waitFor(() => expect(refresh).toHaveBeenCalledTimes(2));
  });
});
