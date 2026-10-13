import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import AuthLayout from './components/layouts/AuthLayout';
import MainLayout from './components/layouts/MainLayout';

const LoginPage = lazy(() => import('./pages/auth/LoginPage'));
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage'));
const VerifyEmailPage = lazy(() => import('./pages/auth/VerifyEmailPage'));
const WorkspaceListPage = lazy(() => import('./pages/workspaces/WorkspaceListPage'));
const WorkspaceDetailPage = lazy(() => import('./pages/workspaces/WorkspaceDetailPage'));
const ProjectBoardPage = lazy(() => import('./pages/projects/ProjectBoardPage'));
const NotificationPage = lazy(() => import('./pages/notifications/NotificationPage'));
const CalendarPage = lazy(() => import('./pages/calendar/CalendarPage'));
const MyTasksPage = lazy(() => import('./pages/tasks/MyTasksPage'));
const WorkspaceDashboardPage = lazy(() => import('./pages/dashboard/WorkspaceDashboardPage'));

function GuestRoute({ children }) {
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <Suspense fallback={null}>
        <Routes>
        <Route element={<GuestRoute><AuthLayout /></GuestRoute>}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
        </Route>


        <Route
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<WorkspaceListPage />} />
          <Route path="/my-tasks" element={<MyTasksPage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/dashboard" element={<WorkspaceDashboardPage />} />
          <Route path="/workspaces/:id" element={<WorkspaceDetailPage />} />
          <Route path="/workspaces/:id/calendar" element={<CalendarPage />} />
          <Route path="/workspaces/:id/dashboard" element={<WorkspaceDashboardPage />} />
          <Route path="/workspaces/:id/projects/:projectId/tasks/:taskId" element={<ProjectBoardPage />} />
          <Route path="/workspaces/:id/projects/:projectId" element={<ProjectBoardPage />} />
          <Route path="/notifications" element={<NotificationPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </AuthProvider>
  );
}
