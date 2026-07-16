import { lazy, Suspense, useMemo } from 'react';
import { Route, Routes, useSearchParams } from 'react-router-dom';
import { StaffAuthProvider } from './auth/StaffAuthContext';
import { AppFooter } from './components/AppFooter';
import { AppHeader } from './components/AppHeader';
import { StaffRoute } from './components/StaffRoute';
import { StaffLoginPage } from './pages/StaffLoginPage';

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

function AppRoutes() {
  const [searchParams] = useSearchParams();
  const tournamentId = useMemo(() => readTournamentId(searchParams), [searchParams]);

  return (
    <div className="app-shell">
      <AppHeader tournamentId={tournamentId} />
      <Suspense fallback={<main className="page-shell"><p className="dialog-state">화면을 불러오는 중입니다.</p></main>}>
        <Routes>
        <Route element={<AudienceHome tournamentId={tournamentId} />} path="/" />
        <Route element={<BracketPage tournamentId={tournamentId} />} path="/bracket" />
        <Route element={<StaffLoginPage />} path="/staff/login" />
        <Route element={<StaffRoute allowedRoles={['JUDGE']}><AssignedJudgeRoute tournamentId={tournamentId} /></StaffRoute>} path="/judge" />
        <Route element={<StaffRoute allowedRoles={['SUPERVISOR']}><AssignedSupervisorRoute tournamentId={tournamentId} /></StaffRoute>} path="/supervisor" />
        <Route element={<StaffRoute allowedRoles={['RING_MANAGER']}><AssignedRingManagerRoute tournamentId={tournamentId} /></StaffRoute>} path="/ring-manager" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><OperationsPage tournamentId={tournamentId} /></StaffRoute>} path="/operations" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AuditLogPage tournamentId={tournamentId} /></StaffRoute>} path="/audit-logs" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AdminTournamentPage /></StaffRoute>} path="/admin/tournaments" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AdminRingPage tournamentId={tournamentId} /></StaffRoute>} path="/admin/rings" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AdminAthletePage /></StaffRoute>} path="/admin/athletes" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AdminNoticePage tournamentId={tournamentId} /></StaffRoute>} path="/admin/notices" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AdminSchedulePage tournamentId={tournamentId} /></StaffRoute>} path="/admin/schedules" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AdminBoutPage tournamentId={tournamentId} /></StaffRoute>} path="/admin/bouts" />
        <Route element={<StaffRoute allowedRoles={['SERVICE_MANAGER']}><AdminAccountPage /></StaffRoute>} path="/admin/accounts" />
        <Route element={<StaffRoute allowedRoles={['GAME_MANAGER', 'SERVICE_MANAGER']}><AdminAssignmentPage /></StaffRoute>} path="/admin/assignments" />
        </Routes>
      </Suspense>
      <AppFooter />
    </div>
  );
}

export default function App() {
  return <StaffAuthProvider><AppRoutes /></StaffAuthProvider>;
}
