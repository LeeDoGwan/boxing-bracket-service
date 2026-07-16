import { lazy, Suspense, useMemo } from 'react';
import { Route, Routes, useSearchParams } from 'react-router-dom';
import { AppHeader } from './components/AppHeader';

const AudienceHome = lazy(() => import('./pages/AudienceHome').then(({ AudienceHome: page }) => ({ default: page })));
const BracketPage = lazy(() => import('./pages/BracketPage').then(({ BracketPage: page }) => ({ default: page })));
const OperationsPage = lazy(() => import('./pages/OperationsPage').then(({ OperationsPage: page }) => ({ default: page })));
const AuditLogPage = lazy(() => import('./pages/AuditLogPage').then(({ AuditLogPage: page }) => ({ default: page })));
const AdminTournamentPage = lazy(() => import('./pages/AdminTournamentPage').then(({ AdminTournamentPage: page }) => ({ default: page })));
const AdminRingPage = lazy(() => import('./pages/AdminRingPage').then(({ AdminRingPage: page }) => ({ default: page })));
const AdminAthletePage = lazy(() => import('./pages/AdminAthletePage').then(({ AdminAthletePage: page }) => ({ default: page })));
const AdminNoticePage = lazy(() => import('./pages/AdminNoticePage').then(({ AdminNoticePage: page }) => ({ default: page })));
const AdminSchedulePage = lazy(() => import('./pages/AdminSchedulePage').then(({ AdminSchedulePage: page }) => ({ default: page })));
const AdminBoutPage = lazy(() => import('./pages/AdminBoutPage').then(({ AdminBoutPage: page }) => ({ default: page })));
const AdminAccountPage = lazy(() => import('./pages/AdminAccountPage').then(({ AdminAccountPage: page }) => ({ default: page })));
const AdminAssignmentPage = lazy(() => import('./pages/AdminAssignmentPage').then(({ AdminAssignmentPage: page }) => ({ default: page })));
const AssignedJudgeRoute = lazy(() => import('./pages/JudgeAssignedPage').then(({ AssignedJudgeRoute: page }) => ({ default: page })));
const AssignedSupervisorRoute = lazy(() => import('./pages/SupervisorAssignedPage').then(({ AssignedSupervisorRoute: page }) => ({ default: page })));
const AssignedRingManagerRoute = lazy(() => import('./pages/RingManagerAssignedPage').then(({ AssignedRingManagerRoute: page }) => ({ default: page })));

const configuredDefaultTournamentId = Number.parseInt(import.meta.env.VITE_DEFAULT_TOURNAMENT_ID, 10);
const DEFAULT_TOURNAMENT_ID = Number.isInteger(configuredDefaultTournamentId) && configuredDefaultTournamentId > 0
  ? configuredDefaultTournamentId
  : 1;

function readTournamentId(searchParams) {
  const value = Number.parseInt(searchParams.get('tournamentId'), 10);
  return Number.isInteger(value) && value > 0 ? value : DEFAULT_TOURNAMENT_ID;
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
      <Suspense fallback={<main className="page-shell"><p className="dialog-state">화면을 불러오는 중입니다.</p></main>}>
        <Routes>
        <Route element={<AudienceHome tournamentId={tournamentId} />} path="/" />
        <Route element={<BracketPage tournamentId={tournamentId} />} path="/bracket" />
        <Route element={<AssignedJudgeRoute tournamentId={tournamentId} />} path="/judge" />
        <Route element={<AssignedSupervisorRoute tournamentId={tournamentId} />} path="/supervisor" />
        <Route element={<AssignedRingManagerRoute tournamentId={tournamentId} />} path="/ring-manager" />
        <Route element={<OperationsPage tournamentId={tournamentId} />} path="/operations" />
        <Route element={<AuditLogPage tournamentId={tournamentId} />} path="/audit-logs" />
        <Route element={<AdminTournamentPage />} path="/admin/tournaments" />
        <Route element={<AdminRingPage tournamentId={tournamentId} />} path="/admin/rings" />
        <Route element={<AdminAthletePage />} path="/admin/athletes" />
        <Route element={<AdminNoticePage tournamentId={tournamentId} />} path="/admin/notices" />
        <Route element={<AdminSchedulePage tournamentId={tournamentId} />} path="/admin/schedules" />
        <Route element={<AdminBoutPage tournamentId={tournamentId} />} path="/admin/bouts" />
        <Route element={<AdminAccountPage />} path="/admin/accounts" />
        <Route element={<AdminAssignmentPage />} path="/admin/assignments" />
        </Routes>
      </Suspense>
    </div>
  );
}
