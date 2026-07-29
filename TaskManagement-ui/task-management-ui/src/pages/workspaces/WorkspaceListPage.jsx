import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Skeleton,
  Alert,
} from '@mui/material';
import {
  AddRounded as AddIcon,
  SpaceDashboardRounded as WorkspaceIcon,
  PeopleRounded as PeopleIcon,
  FolderRounded as FolderIcon,
} from '@mui/icons-material';
import { getWorkspacesApi, createWorkspaceApi } from '../../api/workspaceApi';
import EmptyState from '../../components/common/EmptyState';

export default function WorkspaceListPage() {
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [form, setForm] = useState({ name: '', description: '' });
  const [error, setError] = useState('');

  const fetchWorkspaces = useCallback(async () => {
    setLoading(true);
    try {
      const { data: res } = await getWorkspacesApi();
      setWorkspaces(res?.data ?? []);
    } catch {}
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchWorkspaces(); }, [fetchWorkspaces]);

  const handleCreate = async () => {
    if (!form.name.trim()) { setError('Workspace name is required'); return; }
    setCreateLoading(true);
    setError('');
    try {
      const { data: res } = await createWorkspaceApi({ name: form.name, description: form.description });
      setWorkspaces((prev) => [...prev, res?.data ?? res]);
      setOpen(false);
      setForm({ name: '', description: '' });
    } catch (err) { setError(err.response?.data?.message || 'Failed to create workspace'); }
    finally { setCreateLoading(false); }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 4 }}>
        <Box>
          <Typography variant="h4" sx={{ color: '#0F172A' }}>Workspaces</Typography>
          <Typography variant="body2" sx={{ color: '#64748B', mt: 0.5 }}>
            Manage your workspaces and collaborate on projects
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpen(true)}
          sx={{ background: 'linear-gradient(135deg, #3B82F6, #60A5FA)', '&:hover': { background: 'linear-gradient(135deg, #2563EB, #3B82F6)' } }}
        >
          Create Workspace
        </Button>
      </Box>

      {loading ? (
        <Grid container spacing={3}>
          {Array.from({ length: 4 }).map((_, i) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={i}>
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
              sx={{ background: 'linear-gradient(135deg, #3B82F6, #60A5FA)', '&:hover': { background: 'linear-gradient(135deg, #2563EB, #3B82F6)' } }}>
              Create Workspace
            </Button>
          }
        />
      ) : (
        <Grid container spacing={3}>
          {workspaces.map((ws) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={ws.id}>
              <Card
                sx={{
                  cursor: 'pointer',
                  borderRadius: 4,
                  border: '1px solid #E2E8F0',
                  '&:hover': { transform: 'translateY(-4px)', boxShadow: '0 12px 32px rgba(59,130,246,0.12)' },
                  transition: 'all 200ms ease',
                }}
                onClick={() => navigate(`/workspaces/${ws.id}`)}
              >
                <CardContent sx={{ p: 3 }}>
                  <Box sx={{
                    width: 44, height: 44, borderRadius: 2.5,
                    background: 'linear-gradient(135deg, #3B82F6, #60A5FA)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2,
                    boxShadow: '0 4px 12px rgba(59,130,246,0.25)',
                  }}>
                    <WorkspaceIcon sx={{ color: '#fff', fontSize: 22 }} />
                  </Box>
                  <Typography variant="h6" fontWeight={700} noWrap sx={{ mb: 0.5, color: '#0F172A' }}>
                    {ws.name}
                  </Typography>
                  <Typography variant="body2" noWrap sx={{ mb: 2.5, color: '#94A3B8', minHeight: 20 }}>
                    {ws.description || 'No description'}
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 2.5, pt: 1.5, borderTop: '1px solid #F1F5F9' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                      <FolderIcon sx={{ fontSize: 16, color: '#94A3B8' }} />
                      <Typography variant="caption" fontWeight={600} sx={{ color: '#64748B' }}>
                        {ws.projectCount ?? 0} projects
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                      <PeopleIcon sx={{ fontSize: 16, color: '#94A3B8' }} />
                      <Typography variant="caption" fontWeight={600} sx={{ color: '#64748B' }}>
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

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#0F172A' }}>Create Workspace</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>{error}</Alert>}
          <TextField autoFocus fullWidth label="Workspace Name" value={form.name}
            onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
            sx={{ mt: 1, mb: 2 }} required />
          <TextField fullWidth label="Description" value={form.description}
            onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
            multiline rows={3} />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setOpen(false)} disabled={createLoading} sx={{ color: '#64748B' }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={createLoading}
            sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #3B82F6, #60A5FA)' }}>
            {createLoading ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
