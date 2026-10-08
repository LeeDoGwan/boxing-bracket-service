import { athleteLabel, decisionLabel, effectiveResultScores, statusLabel, streamStateLabel, winnerLabel, winnerText } from './utils';

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

  it('displays RSC as TKO and adds each penalty to the opponent score', () => {
    expect(decisionLabel('RSC')).toBe('TKO');
    expect(effectiveResultScores({ bluePenaltyTotal: 2, blueTotalScore: 19, redPenaltyTotal: 1, redTotalScore: 19 }))
      .toEqual({ blue: 20, red: 21 });
  });

  it('displays the supported result decisions in Korean', () => {
    expect(decisionLabel('POINTS')).toBe('판정');
    expect(decisionLabel('KO')).toBe('KO');
    expect(decisionLabel('ABD')).toBe('기권');
    expect(decisionLabel('DSQ')).toBe('실격');
    expect(decisionLabel('WALKOVER')).toBe('부전승');
    expect(decisionLabel(null)).toBe('판정');
  });

  it('displays each stream state in Korean', () => {
    expect(streamStateLabel('connected')).toBe('연결됨');
    expect(streamStateLabel('connecting')).toBe('연결 중');
    expect(streamStateLabel('reconnecting')).toBe('재연결 중');
    expect(streamStateLabel('offline')).toBe('연결 안 됨');
    expect(streamStateLabel(null)).toBe('연결 안 됨');
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

  it('renders a draw without assigning a winner', () => {
    const bout = {
      blueAthlete: { name: 'Blue Boxer' },
      redAthlete: { name: 'Red Boxer' },
      resultConfirmed: true,
      winnerSide: 'DRAW',
    };
    expect(winnerLabel(bout)).toBe('무승부');
    expect(winnerText(bout)).toBe('무승부');
  });
});
