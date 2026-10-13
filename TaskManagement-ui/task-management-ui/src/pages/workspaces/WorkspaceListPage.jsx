import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  Skeleton,
  Alert,
  Tooltip,
} from '@mui/material';
import {
  AddRounded as AddIcon,
  SpaceDashboardRounded as WorkspaceIcon,
  PeopleRounded as PeopleIcon,
  FolderRounded as FolderIcon,
  WorkspacePremiumRounded as OwnerIcon,
} from '@mui/icons-material';
import { getWorkspacesApi } from '../../api/workspaceApi';
import { useAuth } from '../../contexts/AuthContext';
import EmptyState from '../../components/common/EmptyState';
import CreateWorkspaceDialog from '../../components/workspace/CreateWorkspaceDialog';
import { on } from '../../utils/eventBus';
import { isWorkspaceOwner } from '../../utils/workspaceOwnership';

export default function WorkspaceListPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [workspaces, setWorkspaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(false);

  const fetchWorkspaces = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data: res } = await getWorkspacesApi();
      setWorkspaces(res?.data ?? []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load workspaces');
    } finally {
      setLoading(false);
    }
  }, []);

  // oxlint-disable-next-line react/set-state-in-effect -- tải dữ liệu khi trang được mở
  useEffect(() => { fetchWorkspaces(); }, [fetchWorkspaces]);
  useEffect(() => on('workspace:created', (workspace) => {
    setWorkspaces((current) => current.some(({ id }) => id === workspace.id)
      ? current
      : [...current, workspace]);
  }), []);
  useEffect(() => on('workspace:deleted', (workspaceId) => {
    setWorkspaces((current) => current.filter(({ id }) => id !== workspaceId));
  }), []);
  useEffect(() => on('workspace:project-count-changed', ({ workspaceId, delta }) => {
    setWorkspaces((current) => current.map((workspace) => workspace.id === workspaceId
      ? { ...workspace, projectCount: Math.max(0, (workspace.projectCount ?? 0) + delta) }
      : workspace));
  }), []);
  useEffect(() => on('workspace:owner-changed', ({ id, owner }) => {
    setWorkspaces((current) => current.map((workspace) => workspace.id === id
      ? { ...workspace, owner }
      : workspace));
  }), []);

  return (
    <Box>
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}
      <Box sx={{ display: 'flex', alignItems: { xs: 'stretch', sm: 'center' }, flexDirection: { xs: 'column', sm: 'row' }, gap: 3, justifyContent: 'space-between', mb: 6 }}>
        <Box>
          <Typography variant="h4" sx={{ color: '#17201F' }}>Workspaces</Typography>
          <Typography variant="body2" sx={{ color: '#66736F', mt: 0.5 }}>
            Manage your workspaces and collaborate on projects
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpen(true)}
          sx={{ background: '#0F766E', '&:hover': { background: '#115E59' } }}
        >
          Create Workspace
        </Button>
      </Box>

      {loading ? (
        <Grid container spacing={3}>
          {Array.from({ length: 4 }).map((_, i) => (
            <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={i}>
              <Skeleton variant="rounded" height={180} sx={{ borderRadius: 4 }} />
            </Grid>
          ))}
        </Grid>
      ) : workspaces.length === 0 ? (
        <EmptyState
          icon={<WorkspaceIcon sx={{ fontSize: 36 }} />}
          title="No workspaces yet"
          description="Create your first workspace to start managing projects"
          action={
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}
              sx={{ background: '#0F766E', '&:hover': { background: '#115E59' } }}>
              Create Workspace
            </Button>
          }
        />
      ) : (
        <Grid container spacing={3}>
          {workspaces.map((ws) => (
            <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={ws.id}>
              <Card
                sx={{
                  cursor: 'pointer',
                  height: '100%',
                  borderRadius: 4,
                  border: '1px solid #DCE4E0',
                  '&:hover': { transform: 'translateY(-4px)', boxShadow: '0 12px 32px rgba(15,118,110,0.12)' },
                  transition: 'all 200ms ease',
                }}
                onClick={() => navigate(`/workspaces/${ws.id}`)}
              >
                <CardContent sx={{ p: 4 }}>
                  <Box sx={{
                    width: 44, height: 44, borderRadius: 2.5,
                    background: '#0F766E',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2,
                    boxShadow: '0 4px 12px rgba(15,118,110,0.25)',
                  }}>
                    <WorkspaceIcon sx={{ color: '#fff', fontSize: 22 }} />
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 0.5 }}>
                    <Typography variant="h6" fontWeight={700} noWrap sx={{ color: '#17201F', minWidth: 0 }}>
                      {ws.name}
                    </Typography>
                    {isWorkspaceOwner(ws, user) && (
                      <Tooltip title="Workspace owner">
                        <OwnerIcon aria-label="Workspace owner" sx={{ color: 'warning.dark', fontSize: 20, flexShrink: 0 }} />
                      </Tooltip>
                    )}
                  </Box>
                  <Typography variant="body2" noWrap sx={{ mb: 2.5, color: '#84918D', minHeight: 20 }}>
                    {ws.description || 'No description'}
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 2.5, pt: 1.5, borderTop: '1px solid #EEF3F0' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                      <FolderIcon sx={{ fontSize: 16, color: '#84918D' }} />
                      <Typography variant="caption" fontWeight={600} sx={{ color: '#66736F' }}>
                        {ws.projectCount ?? 0} projects
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                      <PeopleIcon sx={{ fontSize: 16, color: '#84918D' }} />
                      <Typography variant="caption" fontWeight={600} sx={{ color: '#66736F' }}>
                        {ws.memberCount ?? 0} members
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <CreateWorkspaceDialog open={open} onClose={() => setOpen(false)} />
    </Box>
  );
}
