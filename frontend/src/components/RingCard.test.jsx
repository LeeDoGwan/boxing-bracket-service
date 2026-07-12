import { fireEvent, render, screen } from '@testing-library/react';
import { RingCard } from './RingCard';

describe('RingCard', () => {
  it('shows current and next bouts and selects the current bout', () => {
    const onSelectBout = vi.fn();
    render(<RingCard
      onSelectBout={onSelectBout}
      ring={{
        currentBout: {
          blueAthleteAffiliation: 'Blue Gym',
          blueAthleteName: 'Blue Boxer',
          boutId: 12,
          boutNumber: 12,
          currentRound: 1,
          matchType: 'Final',
          redAthleteAffiliation: 'Red Gym',
          redAthleteName: 'Red Boxer',
          boutStatus: 'IN_PROGRESS',
        },
        nextBout: { boutId: 13, boutNumber: 13, boutStatus: 'READY' },
        ringId: 4,
        ringName: 'Main Ring',
        ringStatus: 'IN_PROGRESS',
      }}
    />);

    expect(screen.getByRole('heading', { name: 'Main Ring' })).toBeInTheDocument();
    expect(screen.getByText('Red Boxer')).toBeInTheDocument();
    expect(screen.getByText('경기 13')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button'));
    expect(onSelectBout).toHaveBeenCalledWith(12);
  });
});
