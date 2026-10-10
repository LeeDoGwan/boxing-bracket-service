import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getBoutDetail } from '../api/audience';
import { confirmResult, correctResult, createPenalty, getPenalties, getSupervisorScores } from '../api/supervisor';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { SupervisorAssignedPage } from './SupervisorAssignedPage';
import { setLanguagePreference, translate as t } from '../i18n';

vi.mock('../api/audience', () => ({ getBoutDetail: vi.fn() }));
vi.mock('../api/supervisor', () => ({ confirmResult: vi.fn(), correctResult: vi.fn(), createPenalty: vi.fn(), getPenalties: vi.fn(), getSupervisorScores: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ getAssignedBouts: vi.fn(), getAssignedRings: vi.fn() }));
vi.mock('../hooks/useBoutEventStream', () => ({ useBoutEventStream: vi.fn(() => 'connected') }));

const session = { accessToken: 'supervisor-token', account: { accountId: 8, name: 'Supervisor', role: 'SUPERVISOR' } };
const bout = { boutId: 12, boutNumber: 12, redAthlete: { name: 'Red' }, blueAthlete: { name: 'Blue' }, status: 'IN_PROGRESS' };

beforeEach(() => {
  vi.clearAllMocks();
  useBoutEventStream.mockReturnValue('connected');
  getAssignedRings.mockResolvedValue([{ name: 'Ring A', ringId: 4 }]);
  getAssignedBouts.mockResolvedValue([{ boutId: 12, boutNumber: 12, ringId: 4, status: 'IN_PROGRESS' }]);
  getBoutDetail.mockResolvedValue(bout);
  getSupervisorScores.mockResolvedValue([]);
  getPenalties.mockResolvedValue([]);
  createPenalty.mockResolvedValue({ penaltyId: 1, penaltyPoint: 1, reason: 'hold', targetSide: 'RED' });
  confirmResult.mockResolvedValue(undefined);
  correctResult.mockResolvedValue(undefined);
});

describe('SupervisorAssignedPage', () => {
  it('preserves correction reasons, confirmation and backend enums across language changes', async () => {
    getBoutDetail.mockResolvedValue({ ...bout, result: { decisionType: 'POINTS', winnerSide: 'RED' }, resultConfirmed: true, status: 'FINISHED' });
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByLabelText('수정 사유');
    const reason = '증거 재검토 / evidence / 証拠';
    fireEvent.change(screen.getByLabelText('수정 사유'), { target: { value: reason } });
    fireEvent.change(screen.getByLabelText('결과 방식'), { target: { value: 'RSC' } });
    fireEvent.click(screen.getByRole('button', { name: '수정 내용 검토' }));
    for (const language of ['en', 'ja']) {
      await act(() => setLanguagePreference(language, { persist: false }));
      expect(screen.getByLabelText(t('operations:result.correctionReason'))).toHaveValue(reason);
      expect(screen.getByLabelText(t('operations:result.decision'))).toHaveValue('RSC');
      expect(screen.getByRole('dialog', { name: t('operations:result.confirmCorrection') })).toHaveTextContent(reason);
    }
    expect(getBoutDetail).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('button', { name: t('operations:result.submitCorrection') }));
    await waitFor(() => expect(correctResult).toHaveBeenCalledWith(12, {
      decisionType: 'RSC', reason, winnerSide: 'RED',
    }, 'supervisor-token'));
  });

  it('loads the assigned ring and bout APIs before reviewing a bout', async () => {
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);

    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    expect(getAssignedRings).toHaveBeenCalledWith(1, 'supervisor-token');
    expect(getAssignedBouts).toHaveBeenCalledWith(4, 'supervisor-token');
  });

  it('refreshes scores and penalties without clearing an in-progress penalty input', async () => {
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('패널티 사유'), { target: { value: 'hold' } });
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'SCORE_SUBMITTED' }));

    await waitFor(() => expect(getSupervisorScores).toHaveBeenCalledTimes(2));
    expect(getPenalties).toHaveBeenCalledTimes(2);
    expect(screen.getByLabelText('패널티 사유')).toHaveValue('hold');
  });

  it('shows draft scores and prevents result confirmation until scores are submitted', async () => {
    getSupervisorScores.mockResolvedValue([
      { blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' },
      { blueScore: null, judgeId: 11, redScore: null, roundNo: 1, status: 'DRAFT' },
    ]);

    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);

    expect(await screen.findByText('제출 완료 1건 · 작성 중 1건')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '결과 검토' })).toBeDisabled();
  });

  it('adds a penalty to the opponent effective total', async () => {
    getSupervisorScores.mockResolvedValue([
      { blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' },
    ]);
    getPenalties.mockResolvedValue([
      { penaltyId: 1, penaltyPoint: 1, targetSide: 'RED' },
      { penaltyId: 2, penaltyPoint: 2, targetSide: 'BLUE' },
    ]);

    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);

    expect(await screen.findByText('파랑 패널티 +2 | 유효 점수 12')).toBeInTheDocument();
    expect(screen.getByText('빨강 패널티 +1 | 유효 점수 10')).toBeInTheDocument();
    expect(screen.getByText(/빨강 패널티 → 파랑 \+1점/)).toBeInTheDocument();
    expect(screen.getByText(/파랑 패널티 → 빨강 \+2점/)).toBeInTheDocument();
  });

  it('leaves a tied effective total for the Supervisor to decide', async () => {
    getSupervisorScores.mockResolvedValue([
      { blueScore: 10, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' },
    ]);

    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.change(screen.getByLabelText('승자'), { target: { value: 'BLUE' } });
    fireEvent.click(screen.getByRole('button', { name: '결과 검토' }));

    expect(screen.getByText('유효 점수가 동점입니다. 감독관이 승자 또는 무승부를 결정합니다.')).toBeInTheDocument();
    expect(screen.getByText('파랑 승 · 판정 | 유효 점수 10-10')).toBeInTheDocument();
  });

  it('blocks zero penalty points without calling the API', async () => {
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.change(screen.getByLabelText('패널티 점수'), { target: { value: '0' } });
    fireEvent.click(screen.getByRole('button', { name: '패널티 추가' }));

    expect(await screen.findByText('패널티 점수는 1 이상의 정수로 입력해 주세요.')).toBeInTheDocument();
    expect(createPenalty).not.toHaveBeenCalled();
  });

  it('keeps result selections when the confirmation is cancelled', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.change(screen.getByLabelText('승자'), { target: { value: 'BLUE' } });
    fireEvent.change(screen.getByLabelText('결과 방식'), { target: { value: 'KO' } });
    fireEvent.click(screen.getByRole('button', { name: '결과 검토' }));
    expect(screen.getByRole('dialog', { name: '결과 확정' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '취소' }));

    expect(screen.getByLabelText('승자')).toHaveValue('BLUE');
    expect(screen.getByLabelText('결과 방식')).toHaveValue('KO');
    expect(confirmResult).not.toHaveBeenCalled();
  });

  it('sends the authenticated action without client actor IDs', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.change(screen.getByLabelText('패널티 사유'), { target: { value: 'hold' } });
    fireEvent.change(screen.getByLabelText('패널티 점수'), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: '패널티 추가' }));
    await waitFor(() => expect(createPenalty).toHaveBeenCalledWith(
      12,
      { penaltyPoint: 1, reason: 'hold', roundNo: 1, targetSide: 'RED' },
      'supervisor-token',
    ));

    fireEvent.click(screen.getByRole('button', { name: '결과 검토' }));
    fireEvent.click(screen.getByRole('button', { name: '결과 확정' }));
    await waitFor(() => expect(confirmResult).toHaveBeenCalledWith(
      12,
      { decisionType: 'POINTS', winnerSide: 'RED' },
      'supervisor-token',
    ));
  });

  it('calls result confirmation once when the confirm action is double clicked', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    let resolveRequest;
    confirmResult.mockImplementation(() => new Promise((resolve) => { resolveRequest = resolve; }));
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.click(screen.getByRole('button', { name: '결과 검토' }));
    const confirmButton = screen.getByRole('button', { name: '결과 확정' });
    fireEvent.click(confirmButton);
    fireEvent.click(confirmButton);

    expect(confirmResult).toHaveBeenCalledTimes(1);
    resolveRequest({ decisionType: 'POINTS', winnerSide: 'RED' });
    await waitFor(() => expect(screen.getByText('결과가 확정되었습니다. 감독관은 사유를 입력하여 결과를 수정할 수 있습니다.')).toBeInTheDocument());
  });

  it('locks penalty actions when a result confirmation event refreshes the bout', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');
    getBoutDetail.mockResolvedValue({ ...bout, result: { decisionType: 'POINTS', winnerSide: 'RED' }, resultConfirmed: true, status: 'FINISHED' });
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'RESULT_CONFIRMED' }));

    await waitFor(() => expect(screen.getByText('결과가 확정되었습니다. 감독관은 사유를 입력하여 결과를 수정할 수 있습니다.')).toBeInTheDocument());
    expect(screen.getByRole('button', { name: '패널티 추가' })).toBeDisabled();
  });

  it('allows a supervisor to correct a confirmed result with a reason', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    getBoutDetail.mockResolvedValue({ ...bout, result: { decisionType: 'POINTS', winnerSide: 'RED' }, resultConfirmed: true, status: 'FINISHED' });
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('결과가 확정되었습니다. 감독관은 사유를 입력하여 결과를 수정할 수 있습니다.');

    fireEvent.change(screen.getByLabelText('수정 사유'), { target: { value: '감독관 작업실ed tie evidence' } });
    fireEvent.click(screen.getByRole('button', { name: '수정 내용 검토' }));
    fireEvent.click(screen.getByRole('button', { name: '수정 확정' }));

    await waitFor(() => expect(correctResult).toHaveBeenCalledWith(
      12,
      { decisionType: 'POINTS', reason: '감독관 작업실ed tie evidence', winnerSide: 'RED' },
      'supervisor-token',
    ));
  });
});
