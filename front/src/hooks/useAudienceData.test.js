import { renderHook, waitFor } from '@testing-library/react';
import { getBouts, getHome } from '../api/audience';
import { useAudienceData } from './useAudienceData';

vi.mock('../api/audience', () => ({
  getBouts: vi.fn(),
  getHome: vi.fn(),
}));

describe('useAudienceData', () => {
  it('keeps home data available and exposes a separate error when bouts loading fails', async () => {
    const boutsError = new Error('bouts unavailable');
    getHome.mockResolvedValue({ ringStatuses: [] });
    getBouts.mockRejectedValue(boutsError);

    const { result } = renderHook(() => useAudienceData(1));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.dataTournamentId).toBe(1);
    expect(result.current.bouts).toEqual([]);
    expect(result.current.boutsError).toBe(boutsError);
    expect(result.current.error).toBeNull();
  });
});
