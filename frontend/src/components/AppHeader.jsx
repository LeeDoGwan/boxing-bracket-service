import { NavLink } from 'react-router-dom';

export function AppHeader({ tournamentId, onTournamentChange }) {
  return (
    <header className="app-header">
      <div className="brand-block">
        <p className="eyebrow">LIVE TOURNAMENT</p>
        <h1>복싱 대회 현황</h1>
      </div>
      <nav aria-label="전체 메뉴" className="main-nav">
        <NavLink end to={`/?tournamentId=${tournamentId}`}>현황</NavLink>
        <NavLink to={`/bracket?tournamentId=${tournamentId}`}>대진표</NavLink>
        <NavLink to={`/judge?tournamentId=${tournamentId}`}>심판</NavLink>
      </nav>
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
