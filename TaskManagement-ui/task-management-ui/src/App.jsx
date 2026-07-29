import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import AuthLayout from './components/layouts/AuthLayout';
import MainLayout from './components/layouts/MainLayout';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import WorkspaceListPage from './pages/workspaces/WorkspaceListPage';
import WorkspaceDetailPage from './pages/workspaces/WorkspaceDetailPage';
import ProjectBoardPage from './pages/projects/ProjectBoardPage';
import NotificationPage from './pages/notifications/NotificationPage';

function GuestRoute({ children }) {
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<GuestRoute><AuthLayout /></GuestRoute>}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        <Route
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<WorkspaceListPage />} />
          <Route path="/workspaces/:id" element={<WorkspaceDetailPage />} />
          <Route path="/workspaces/:id/projects/:projectId/tasks/:taskId" element={<ProjectBoardPage />} />
          <Route path="/workspaces/:id/projects/:projectId" element={<ProjectBoardPage />} />
          <Route path="/notifications" element={<NotificationPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
