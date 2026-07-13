import { athleteLabel, statusLabel, winnerLabel } from './utils';

describe('display helpers', () => {
  it('formats athlete affiliation and fallback values', () => {
    expect(athleteLabel({ name: 'Kim', affiliation: 'Red Gym' })).toBe('Kim · Red Gym');
    expect(athleteLabel({ name: 'Kim' })).toBe('Kim');
    expect(athleteLabel(null)).toBe('선수 정보 없음');
  });

  it('maps known and unknown bout statuses', () => {
    expect(statusLabel('IN_PROGRESS')).toBe('진행 중');
    expect(statusLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(statusLabel(null)).toBe('정보 없음');
  });

  it('returns the confirmed winner only', () => {
    const bout = {
      blueAthlete: { name: 'Blue Boxer' },
      redAthlete: { name: 'Red Boxer' },
      resultConfirmed: true,
      winnerSide: 'RED',
    };
    expect(winnerLabel(bout)).toBe('Red Boxer');
    expect(winnerLabel({ ...bout, resultConfirmed: false })).toBeNull();
  });
});
