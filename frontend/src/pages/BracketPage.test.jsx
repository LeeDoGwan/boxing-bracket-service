import { act, fireEvent, render, screen } from '@testing-library/react';
import { getBouts, searchBouts } from '../api/audience';
import { BracketPage } from './BracketPage';

vi.mock('../api/audience', () => ({
  getBouts: vi.fn(),
  searchBouts: vi.fn(),
}));

describe('BracketPage', () => {
  it('loads official bouts and submits a search', async () => {
    const bouts = [{
      blueAthlete: { affiliation: 'Blue Gym', name: 'Blue Boxer' },
      boutId: 12,
      boutNumber: 12,
      redAthlete: { affiliation: 'Red Gym', name: 'Red Boxer' },
      status: 'SCHEDULED',
    }];
    getBouts.mockResolvedValue(bouts);
    searchBouts.mockResolvedValue(bouts);

    render(<BracketPage tournamentId={1} />);

    expect(await screen.findByText('1개 경기')).toBeInTheDocument();
    expect(getBouts).toHaveBeenCalledWith(1);
    fireEvent.change(screen.getByLabelText(/선수/), { target: { value: 'Blue' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: '검색' }));
      await Promise.resolve();
    });
    expect(searchBouts).toHaveBeenCalledWith(1, 'Blue');
  });
});
