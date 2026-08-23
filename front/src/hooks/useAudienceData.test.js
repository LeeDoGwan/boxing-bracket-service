import { renderHook, waitFor } from '@testing-library/react';
import { getHome } from '../api/audience';
import { useAudienceData } from './useAudienceData';

vi.mock('../api/audience', () => ({
  getHome: vi.fn(),
}));

describe('useAudienceData', () => {
  it('uses the official bouts embedded in the aggregate home response', async () => {
    const officialBouts = [{ boutId: 12, status: 'READY' }];
    getHome.mockResolvedValue({ officialBouts, ringStatuses: [] });

    const { result } = renderHook(() => useAudienceData(1));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.dataTournamentId).toBe(1);
    expect(result.current.bouts).toEqual(officialBouts);
    expect(result.current.boutsError).toBeNull();
    expect(result.current.error).toBeNull();
  });
});
