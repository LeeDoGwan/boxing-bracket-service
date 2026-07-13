import { fireEvent, render, screen } from '@testing-library/react';
import { getBoutDetail } from '../api/audience';
import { BoutDetailDialog } from './BoutDetailDialog';

vi.mock('../api/audience', () => ({
  getBoutDetail: vi.fn(),
}));

describe('BoutDetailDialog', () => {
  it('loads bout details and closes on command', async () => {
    const onClose = vi.fn();
    getBoutDetail.mockResolvedValue({
      blueAthlete: { affiliation: 'Blue Gym', name: 'Blue Boxer' },
      boutNumber: 12,
      currentRound: 2,
      matchType: 'Semi Final',
      redAthlete: { affiliation: 'Red Gym', name: 'Red Boxer' },
      status: 'IN_PROGRESS',
      totalRounds: 3,
    });

    render(<BoutDetailDialog boutId={12} onClose={onClose} />);

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Semi Final' })).toBeInTheDocument();
    expect(screen.getByText('Red Boxer · Red Gym')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /상세 닫기/ }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
