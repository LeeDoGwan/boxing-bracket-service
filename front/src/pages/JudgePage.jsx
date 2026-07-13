import { useCallback, useEffect, useMemo, useState } from 'react';
import { getBoutDetail, getBouts } from '../api/audience';
import { login, logout } from '../api/auth';
import { getJudgeScores, submitRoundScore } from '../api/judge';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.judge.session';

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && session?.account?.role === 'JUDGE' ? session : null;
  } catch {
    return null;
  }
}

function displayAthlete(athlete) {
  return athlete?.name || '선수 미정';
}

function displayBoutStatus(status) {
  const labels = {
    FINISHED: '종료',
    IN_PROGRESS: '진행 중',
    READY: '준비',
    SCHEDULED: '예정',
  };
  return labels[status] || status || '상태 미정';
}

function sortBouts(bouts) {
  const priority = { IN_PROGRESS: 0, READY: 1, SCHEDULED: 2, FINISHED: 3 };
  return [...bouts].sort((left, right) => {
    const statusDifference = (priority[left.status] ?? 4) - (priority[right.status] ?? 4);
    return statusDifference || ((left.scheduledOrder ?? left.boutNumber ?? 0) - (right.scheduledOrder ?? right.boutNumber ?? 0));
  });
}

function LoginForm({ onLogin }) {
  const [form, setForm] = useState({ loginId: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const session = await login(form.loginId, form.password);
      if (session.account?.role !== 'JUDGE') {
        throw new Error('JUDGE_ONLY');
      }
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message === 'JUDGE_ONLY' ? '심판 계정으로 로그인해 주세요.' : '로그인 정보를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell">
      <section className="auth-panel">
        <p className="eyebrow">JUDGE DESK</p>
        <h2>심판 로그인</h2>
        <p>경기 점수를 입력하려면 심판 계정으로 로그인하세요.</p>
        <form onSubmit={handleSubmit}>
          <label>
            아이디
            <input
              autoComplete="username"
              onChange={(event) => setForm((current) => ({ ...current, loginId: event.target.value }))}
              required
              value={form.loginId}
            />
          </label>
          <label>
            비밀번호
            <input
              autoComplete="current-password"
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              required
              type="password"
              value={form.password}
            />
          </label>
          {error && <p aria-live="polite" className="form-error" role="alert">{error}</p>}
          <button className="command-button" disabled={submitting} type="submit">
            {submitting ? '로그인 중...' : '로그인'}
          </button>
        </form>
      </section>
    </main>
  );
}

function BoutPicker({ bouts, onSelect, selectedBoutId }) {
  if (!bouts.length) {
    return <StatePanel title="선택할 경기가 없습니다.">게임 매니저가 등록한 공식 경기가 나타납니다.</StatePanel>;
  }

  return (
    <div className="judge-bout-list" aria-label="경기 목록">
      {bouts.map((bout) => (
        <button
          className={`judge-bout-option${selectedBoutId === bout.boutId ? ' selected' : ''}`}
          key={bout.boutId}
          onClick={() => onSelect(bout.boutId)}
          type="button"
        >
          <span>경기 {bout.boutNumber}</span>
          <strong>{displayAthlete(bout.redAthlete)} <b>vs</b> {displayAthlete(bout.blueAthlete)}</strong>
          <small>{bout.matchType || '일반 경기'} · {displayBoutStatus(bout.status)}</small>
        </button>
      ))}
    </div>
  );
}

function ScoreCard({ accountId, bout, onSubmit, roundNo, score, submitting }) {
  const [redScore, setRedScore] = useState(score?.redScore ?? '');
  const [blueScore, setBlueScore] = useState(score?.blueScore ?? '');
  const [error, setError] = useState('');
  const submitted = score?.status === 'SUBMITTED';
  const completed = bout.resultConfirmed || bout.status === 'FINISHED' || bout.status === 'CONFIRMED';

  useEffect(() => {
    setRedScore(score?.redScore ?? '');
    setBlueScore(score?.blueScore ?? '');
  }, [score?.blueScore, score?.redScore]);

  async function handleSubmit(event) {
    event.preventDefault();
    const red = Number(redScore);
    const blue = Number(blueScore);
    if (!Number.isInteger(red) || red < 0 || !Number.isInteger(blue) || blue < 0) {
      setError('레드와 블루 점수를 0 이상의 정수로 입력하세요.');
      return;
    }
    setError('');
    await onSubmit(roundNo, { judgeId: accountId, redScore: red, blueScore: blue });
  }

  return (
    <form className={`judge-score-card${submitted ? ' submitted' : ''}`} onSubmit={handleSubmit}>
      <div className="judge-score-card-heading">
        <div>
          <p className="eyebrow">ROUND {roundNo}</p>
          <h3>{roundNo === bout.currentRound ? '현재 라운드' : `라운드 ${roundNo}`}</h3>
        </div>
        <span className={submitted ? 'score-status submitted' : 'score-status'}>
          {submitted ? '제출됨' : completed ? '제출 불가' : '입력 대기'}
        </span>
      </div>
      <div className="judge-score-fields">
        <label className="score-input red-score">
          <span>레드 점수</span>
          <input
            aria-label={`레드 점수 ${roundNo}라운드`}
            disabled={submitted || completed || submitting}
            inputMode="numeric"
            min="0"
            onChange={(event) => setRedScore(event.target.value)}
            required
            type="number"
            value={redScore}
          />
        </label>
        <label className="score-input blue-score">
          <span>블루 점수</span>
          <input
            aria-label={`블루 점수 ${roundNo}라운드`}
            disabled={submitted || completed || submitting}
            inputMode="numeric"
            min="0"
            onChange={(event) => setBlueScore(event.target.value)}
            required
            type="number"
            value={blueScore}
          />
        </label>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      {!submitted && !completed && (
        <button className="command-button" disabled={submitting} type="submit">
          {submitting ? '제출 중...' : `${roundNo}라운드 제출`}
        </button>
      )}
    </form>
  );
}

function JudgeWorkspace({ onLogout, session, tournamentId }) {
  const [bouts, setBouts] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const [bout, setBout] = useState(null);
  const [scores, setScores] = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [submittingRound, setSubmittingRound] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  const sortedBouts = useMemo(() => sortBouts(bouts), [bouts]);
  const loadBouts = useCallback(async () => {
    setListLoading(true);
    setListError('');
    try {
      const nextBouts = (await getBouts(tournamentId)) || [];
      setBouts(nextBouts);
      setSelectedBoutId((current) => current && nextBouts.some((item) => item.boutId === current)
        ? current
        : sortBouts(nextBouts)[0]?.boutId || null);
    } catch {
      setListError('경기 목록을 불러오지 못했습니다.');
    } finally {
      setListLoading(false);
    }
  }, [tournamentId]);

  useEffect(() => {
    loadBouts();
  }, [loadBouts]);

  useEffect(() => {
    if (!selectedBoutId) {
      setBout(null);
      setScores([]);
      return undefined;
    }
    let cancelled = false;
    setDetailLoading(true);
    setDetailError('');
    Promise.all([
      getBoutDetail(selectedBoutId),
      getJudgeScores(selectedBoutId, session.account.accountId, session.accessToken),
    ])
      .then(([nextBout, nextScores]) => {
        if (!cancelled) {
          setBout(nextBout);
          setScores(nextScores || []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDetailError('경기와 점수 정보를 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDetailLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey, selectedBoutId, session.accessToken, session.account.accountId]);

  async function handleSubmit(roundNo, score) {
    setSubmittingRound(roundNo);
    try {
      const submittedScore = await submitRoundScore(selectedBoutId, roundNo, score, session.accessToken);
      setScores((current) => [
        ...current.filter((item) => !(item.roundNo === roundNo && item.judgeId === score.judgeId)),
        submittedScore,
      ]);
    } catch (error) {
      setDetailError(error.message === 'INVALID_BOUT_STATE' ? '종료된 경기는 점수를 제출할 수 없습니다.' : '점수 제출에 실패했습니다.');
    } finally {
      setSubmittingRound(null);
    }
  }

  const rounds = bout
    ? Array.from({ length: Math.max(1, bout.totalRounds || bout.currentRound || 1) }, (_, index) => index + 1)
    : [];

  return (
    <main className="page-shell judge-shell">
      <div className="judge-heading">
        <div>
          <p className="eyebrow">JUDGE DESK</p>
          <h2>심판 점수 입력</h2>
          <p>로그인 계정: {session.account.name} · 심판 ID {session.account.accountId}</p>
        </div>
        <button className="secondary-button" onClick={onLogout} type="button">로그아웃</button>
      </div>

      {listLoading ? <StatePanel title="경기 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError ? (
        <StatePanel action={<button className="command-button" onClick={loadBouts} type="button">다시 시도</button>} title="경기 목록을 불러오지 못했습니다." tone="error">
          대회 ID를 확인하거나 잠시 후 다시 시도해 주세요.
        </StatePanel>
      ) : null}
      {!listLoading && !listError ? (
        <div className="judge-layout">
          <aside>
            <div className="judge-section-heading">
              <div>
                <p className="eyebrow">TOURNAMENT {tournamentId}</p>
                <h3>경기 선택</h3>
              </div>
              <span>{sortedBouts.length}경기</span>
            </div>
            <BoutPicker bouts={sortedBouts} onSelect={setSelectedBoutId} selectedBoutId={selectedBoutId} />
          </aside>

          <section className="judge-score-panel" aria-label="점수 입력">
            {detailLoading ? <StatePanel title="경기 정보를 불러오는 중입니다.">점수 입력 화면을 준비하고 있습니다.</StatePanel> : null}
            {detailError && !detailLoading ? (
              <StatePanel action={<button className="command-button" onClick={() => setReloadKey((current) => current + 1)} type="button">다시 시도</button>} title="점수 정보를 불러오지 못했습니다." tone="error">
                선택한 경기의 상태를 확인해 주세요.
              </StatePanel>
            ) : null}
            {!detailLoading && !detailError && bout ? (
              <>
                <div className="judge-bout-heading">
                  <div>
                    <p className="eyebrow">BOUT {bout.boutNumber}</p>
                    <h3>{bout.matchType || '일반 경기'}</h3>
                    <p>{displayBoutStatus(bout.status)} · {bout.currentRound || 0}/{bout.totalRounds || '?'} 라운드</p>
                  </div>
                  <span className="status-pill">{bout.resultConfirmed ? '결과 확정' : displayBoutStatus(bout.status)}</span>
                </div>
                <div className="judge-athletes">
                  <div className="judge-athlete red-side"><span>R</span><strong>{displayAthlete(bout.redAthlete)}</strong><small>{bout.redAthlete?.affiliation || '소속 미정'}</small></div>
                  <div className="versus">VS</div>
                  <div className="judge-athlete blue-side"><span>B</span><strong>{displayAthlete(bout.blueAthlete)}</strong><small>{bout.blueAthlete?.affiliation || '소속 미정'}</small></div>
                </div>
                <div className="judge-score-list">
                  {rounds.map((roundNo) => (
                    <ScoreCard
                      accountId={session.account.accountId}
                      bout={bout}
                      key={roundNo}
                      onSubmit={handleSubmit}
                      roundNo={roundNo}
                      score={scores.find((score) => score.roundNo === roundNo)}
                      submitting={submittingRound === roundNo}
                    />
                  ))}
                </div>
              </>
            ) : null}
            {!detailLoading && !detailError && !bout && !sortedBouts.length ? (
              <StatePanel title="점수 입력 경기가 없습니다.">대회에 등록된 공식 경기를 선택하면 점수를 입력할 수 있습니다.</StatePanel>
            ) : null}
          </section>
        </div>
      ) : null}
    </main>
  );
}

export function JudgePage({ tournamentId }) {
  const [session, setSession] = useState(readSession);

  function handleLogin(nextSession) {
    window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
  }

  async function handleLogout() {
    if (session?.accessToken) {
      await logout(session.accessToken).catch(() => undefined);
    }
    window.sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
  }

  return session
    ? <JudgeWorkspace onLogout={handleLogout} session={session} tournamentId={tournamentId} />
    : <LoginForm onLogin={handleLogin} />;
}
