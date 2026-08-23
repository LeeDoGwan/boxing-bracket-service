import { fireEvent, render, screen, within } from '@testing-library/react';
import { getBoutDetail } from '../api/audience';
import { useAudienceData } from '../hooks/useAudienceData';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { AudienceHome } from './AudienceHome';

vi.mock('../api/audience', () => ({ getBoutDetail: vi.fn() }));
vi.mock('../hooks/useAudienceData', () => ({ useAudienceData: vi.fn() }));
vi.mock('../hooks/useBoutEventStream', () => ({ useBoutEventStream: vi.fn() }));

const detail = {
  blueAthlete: { affiliation: 'Blue Gym', name: 'Blue Boxer' },
  boutNumber: 11,
  currentRound: 2,
  matchType: 'Final',
  redAthlete: { affiliation: 'Red Gym', name: 'Red Boxer' },
  resultConfirmed: true,
  result: { decisionType: 'DECISION' },
  status: 'FINISHED',
  totalRounds: 3,
  winnerSide: 'RED',
};

const home = {
  confirmedResults: [{ ...detail, boutId: 11 }],
  notices: [{ content: '결승전은 18시에 시작합니다.', noticeId: 1, title: '오늘의 안내' }],
  ringStatuses: [
    {
      currentBout: { boutId: 11, boutNumber: 11, boutStatus: 'IN_PROGRESS', currentRound: 2, matchType: 'Final', redAthleteAffiliation: 'Red Gym', redAthleteName: 'Red Boxer', blueAthleteAffiliation: 'Blue Gym', blueAthleteName: 'Blue Boxer' },
      nextBout: { boutId: 12, boutNumber: 12, boutStatus: 'READY' },
      ringId: 2,
      ringName: 'Ring Live',
      ringStatus: 'IN_PROGRESS',
    },
    {
      currentBout: null,
      nextBout: { boutId: 13, boutNumber: 13, boutStatus: 'SCHEDULED' },
      ringId: 1,
      ringName: 'Ring Ready',
      ringStatus: 'READY',
    },
  ],
  schedules: [{ scheduleId: 4, startTime: '2026-08-23T18:00:00', status: 'SCHEDULED', title: '결승전', type: 'BOUT' }],
};

const bouts = [
  { boutId: 11, boutNumber: 11, ringId: 2, status: 'IN_PROGRESS' },
  { boutId: 12, boutNumber: 12, ringId: 2, scheduledOrder: 2, status: 'READY' },
  { boutId: 14, boutNumber: 14, ringId: 2, scheduledOrder: 4, status: 'SCHEDULED', redAthlete: { name: 'Later Red' }, blueAthlete: { name: 'Later Blue' } },
  { boutId: 15, boutNumber: 15, ringId: 2, scheduledOrder: 5, status: 'FINISHED' },
];

function setAudienceState(overrides = {}) {
  useAudienceData.mockReturnValue({
    bouts,
    boutsError: null,
    dataTournamentId: 1,
    error: null,
    home,
    loading: false,
    reload: vi.fn(),
    ...overrides,
  });
  useBoutEventStream.mockReturnValue('connected');
}

beforeEach(() => {
  vi.clearAllMocks();
  setAudienceState();
  getBoutDetail.mockResolvedValue(detail);
});

describe('AudienceHome', () => {
  it('renders public summary, ordered rings, later bouts, results, and schedule', () => {
    const { container } = render(<AudienceHome tournamentId={1} />);

    expect(screen.getByText('오늘의 안내')).toBeInTheDocument();
    expect(screen.getByText(/결승전은 18시/)).toBeInTheDocument();
    expect(within(screen.getByLabelText('대회 요약')).getByText('진행 중')).toBeInTheDocument();
    expect(screen.getByText('이후 경기')).toBeInTheDocument();
    expect(screen.getByText('경기 14')).toBeInTheDocument();
    expect(screen.queryByText('경기 15')).not.toBeInTheDocument();
    expect(screen.getByText('Red Boxer 승')).toBeInTheDocument();
    expect(screen.getByText('결승전')).toBeInTheDocument();

    const ringHeadings = within(container.querySelector('.ring-grid')).getAllByRole('heading', { level: 3 });
    expect(ringHeadings.map((heading) => heading.textContent)).toEqual(['Ring Live', 'Ring Ready']);
    expect(screen.getByText('실시간 반영 중')).toBeInTheDocument();
  });

  it('shows loading and fatal error states with a retry action', () => {
    const reload = vi.fn();
    setAudienceState({ dataTournamentId: null, error: null, home: null, loading: true, reload });
    const { rerender } = render(<AudienceHome tournamentId={1} />);
    expect(screen.getByText('대회 현황을 불러오는 중입니다.')).toBeInTheDocument();

    setAudienceState({ dataTournamentId: null, error: new Error('home unavailable'), home: null, loading: false, reload });
    rerender(<AudienceHome tournamentId={1} />);
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }));
    expect(reload).toHaveBeenCalledOnce();
  });

  it('keeps the last home data visible when refresh fails and retries from the stale banner', () => {
    const reload = vi.fn();
    setAudienceState({ error: new Error('home unavailable'), reload });

    render(<AudienceHome tournamentId={1} />);

    expect(screen.getByText(/최신 현황을 갱신하지 못했습니다/)).toBeInTheDocument();
    expect(screen.getByText('Ring Live')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }));
    expect(reload).toHaveBeenCalledOnce();
  });

  it('opens selected ring and result details and closes the dialog', async () => {
    render(<AudienceHome tournamentId={1} />);

    fireEvent.click(screen.getByRole('button', { name: '경기 11 상세 보기' }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Final' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '경기 상세 닫기' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /경기 11 Red Boxer 승 DECISION/ }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(getBoutDetail).toHaveBeenCalledWith(11, expect.objectContaining({ signal: expect.any(AbortSignal) }));
  });
});
