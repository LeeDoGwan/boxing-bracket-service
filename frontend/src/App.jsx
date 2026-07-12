import { useMemo } from 'react';
import { Route, Routes, useSearchParams } from 'react-router-dom';
import { AppHeader } from './components/AppHeader';
import { AudienceHome } from './pages/AudienceHome';
import { BracketPage } from './pages/BracketPage';
import { JudgePage } from './pages/JudgePage';
import { SupervisorPage } from './pages/SupervisorPage';
import { RingManagerPage } from './pages/RingManagerPage';
import { OperationsPage } from './pages/OperationsPage';
import { AuditLogPage } from './pages/AuditLogPage';
import { AdminTournamentPage } from './pages/AdminTournamentPage';
import { AdminRingPage } from './pages/AdminRingPage';
import { AdminAthletePage } from './pages/AdminAthletePage';
import { AdminNoticePage } from './pages/AdminNoticePage';
import { AdminBoutPage } from './pages/AdminBoutPage';
import { AdminAccountPage } from './pages/AdminAccountPage';

function readTournamentId(searchParams) {
  const value = Number.parseInt(searchParams.get('tournamentId'), 10);
  return Number.isInteger(value) && value > 0 ? value : 1;
}

export default function App() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tournamentId = useMemo(() => readTournamentId(searchParams), [searchParams]);

  const updateTournamentId = (value) => {
    const nextTournamentId = Number.parseInt(value, 10);
    if (!Number.isInteger(nextTournamentId) || nextTournamentId < 1) {
      return;
    }
    setSearchParams({ tournamentId: String(nextTournamentId) });
  };

  return (
    <div className="app-shell">
      <AppHeader onTournamentChange={updateTournamentId} tournamentId={tournamentId} />
      <Routes>
        <Route element={<AudienceHome tournamentId={tournamentId} />} path="/" />
        <Route element={<BracketPage tournamentId={tournamentId} />} path="/bracket" />
        <Route element={<JudgePage tournamentId={tournamentId} />} path="/judge" />
        <Route element={<SupervisorPage tournamentId={tournamentId} />} path="/supervisor" />
        <Route element={<RingManagerPage tournamentId={tournamentId} />} path="/ring-manager" />
        <Route element={<OperationsPage tournamentId={tournamentId} />} path="/operations" />
        <Route element={<AuditLogPage tournamentId={tournamentId} />} path="/audit-logs" />
        <Route element={<AdminTournamentPage />} path="/admin/tournaments" />
        <Route element={<AdminRingPage tournamentId={tournamentId} />} path="/admin/rings" />
        <Route element={<AdminAthletePage />} path="/admin/athletes" />
        <Route element={<AdminNoticePage tournamentId={tournamentId} />} path="/admin/notices" />
        <Route element={<AdminBoutPage tournamentId={tournamentId} />} path="/admin/bouts" />
        <Route element={<AdminAccountPage />} path="/admin/accounts" />
      </Routes>
    </div>
  );
}
