import { NavLink } from 'react-router-dom';

export function AppHeader({ tournamentId, onTournamentChange }) {
  const publicLinks = [
    ['/', '현황'],
    ['/bracket', '대진표'],
  ];
  const operationLinks = [
    ['/judge', '심판'],
    ['/supervisor', '감독'],
    ['/ring-manager', '링 운영'],
    ['/operations', '운영 현황'],
    ['/audit-logs', '감사 로그'],
    ['/admin/tournaments', '대회 관리'],
    ['/admin/rings', '링 관리'],
    ['/admin/athletes', '선수 관리'],
    ['/admin/notices', '공지 관리'],
    ['/admin/schedules', '일정 관리'],
    ['/admin/bouts', '대진 관리'],
    ['/admin/accounts', '계정 관리'],
    ['/admin/assignments', '배정 관리'],
  ];
  return (
    <header className="app-header">
      <div className="brand-block">
        <div className="brand-lockup">
          <span aria-hidden="true" className="brand-mark">BX</span>
          <div>
            <p className="eyebrow">LIVE TOURNAMENT</p>
            <h1>복싱 대회 현황</h1>
          </div>
        </div>
      </div>
      <nav aria-label="공개 메뉴" className="main-nav">
        {publicLinks.map(([path, label]) => (
          <NavLink end={path === '/'} key={path} to={`${path}?tournamentId=${tournamentId}`}>{label}</NavLink>
        ))}
      </nav>
      <details className="operations-menu">
        <summary>운영 메뉴</summary>
        <nav aria-label="운영 메뉴" className="operations-menu-panel">
          {operationLinks.map(([path, label]) => (
            <NavLink key={path} to={`${path}?tournamentId=${tournamentId}`}>{label}</NavLink>
          ))}
        </nav>
      </details>
      <label className="tournament-input">
        <span>대회 ID</span>
        <input
          min="1"
          type="number"
          value={tournamentId}
          onChange={(event) => onTournamentChange(event.target.value)}
        />
      </label>
    </header>
  );
}
