import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  IconButton,
  useTheme,
  useMediaQuery,
  Divider,
  Skeleton,
  Tooltip,
} from '@mui/material';
import {
  DashboardRounded as DashboardIcon,
  ChevronLeftRounded as CollapseIcon,
  AddRounded as AddIcon,
  AssignmentTurnedInRounded as MyTasksIcon,
  CalendarMonthRounded as CalendarIcon,
  BarChartRounded as MetricsIcon,
  BusinessRounded as WorkspaceNavIcon,
  WorkspacePremiumRounded as OwnerIcon,
} from '@mui/icons-material';
import { getWorkspacesApi } from '../../api/workspaceApi';
import { useAuth } from '../../contexts/AuthContext';
import CreateWorkspaceDialog from '../workspace/CreateWorkspaceDialog';
import { on } from '../../utils/eventBus';
import { isWorkspaceOwner } from '../../utils/workspaceOwnership';

const DRAWER_WIDTH = 256;

export default function Sidebar({ open, onToggle }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [workspaces, setWorkspaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

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

  useEffect(() => on('workspace:joined', async () => {
    try {
      const { data: res } = await getWorkspacesApi();
      setWorkspaces(res?.data ?? []);
    } catch (err) {
      console.warn('Failed to refresh workspaces after joining:', err?.message);
    }
  }), []);

  useEffect(() => {
    if (!open) return;
    const fetch = async () => {
      setLoading(true);
      try {
        const { data: res } = await getWorkspacesApi();
        setWorkspaces(res?.data ?? []);
      } catch (err) {
        console.warn('Failed to load workspaces in sidebar:', err?.message);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [open]);

  const currentWsId = location.pathname.match(/\/workspaces\/(\d+)/)?.[1];

  useEffect(() => {
    if (currentWsId) {
      localStorage.setItem('lastWorkspaceId', currentWsId);
    }
  }, [currentWsId]);

  const content = (
    <Box
      sx={{
        width: DRAWER_WIDTH,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: '#F3F6F4',
        borderRight: '1px solid rgba(23,32,31,0.08)',
      }}
    >
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          px: 2.5,
          py: 2.5,
        }}
      >
        <Box
          sx={{ display: 'flex', alignItems: 'center', gap: 1.5, cursor: 'pointer' }}
          onClick={() => navigate('/')}
        >
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: 2,
              background: '#17201F',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 8px rgba(15,118,110,0.3)',
            }}
          >
            <DashboardIcon sx={{ color: '#fff', fontSize: 20 }} />
          </Box>
          <Box>
            <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#17201F', lineHeight: 1.2 }}>
              Task Manager
            </Typography>
            <Typography variant="caption" sx={{ color: '#84918D' }}>
              Workspace dashboard
            </Typography>
          </Box>
        </Box>
        {isMobile && (
          <IconButton onClick={onToggle} size="small" sx={{ color: '#84918D' }}>
            <CollapseIcon />
          </IconButton>
        )}
      </Box>

      <Divider sx={{ mx: 2 }} />

      {/* Navigation Links */}
      <List sx={{ px: 1.5, py: 1 }} dense>
        <ListItemButton
          selected={location.pathname === '/'}
          onClick={() => {
            navigate('/');
            if (isMobile) onToggle();
          }}
          sx={{
            borderRadius: 2,
            mb: 0.5,
            py: 0.8,
            px: 1.5,
            '&.Mui-selected': { bgcolor: '#D9F3ED', color: '#115E59' },
          }}
        >
          <ListItemIcon sx={{ minWidth: 32, color: location.pathname === '/' ? '#0F766E' : '#84918D' }}>
            <WorkspaceNavIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="All Workspaces" primaryTypographyProps={{ variant: 'body2', fontWeight: 600 }} />
        </ListItemButton>

        <ListItemButton
          selected={location.pathname === '/my-tasks'}
          onClick={() => {
            navigate('/my-tasks');
            if (isMobile) onToggle();
          }}
          sx={{
            borderRadius: 2,
            mb: 0.5,
            py: 0.8,
            px: 1.5,
            '&.Mui-selected': { bgcolor: '#D9F3ED', color: '#115E59' },
          }}
        >
          <ListItemIcon sx={{ minWidth: 32, color: location.pathname === '/my-tasks' ? '#0F766E' : '#84918D' }}>
            <MyTasksIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="My Tasks" primaryTypographyProps={{ variant: 'body2', fontWeight: 600 }} />
        </ListItemButton>


        <ListItemButton
          selected={location.pathname === '/calendar' || location.pathname.endsWith('/calendar')}
          onClick={() => {
            navigate('/calendar');
            if (isMobile) onToggle();
          }}
          sx={{
            borderRadius: 2,
            mb: 0.5,
            py: 0.8,
            px: 1.5,
            '&.Mui-selected': { bgcolor: '#D9F3ED', color: '#115E59' },
          }}
        >
          <ListItemIcon sx={{ minWidth: 32, color: (location.pathname === '/calendar' || location.pathname.endsWith('/calendar')) ? '#0F766E' : '#84918D' }}>
            <CalendarIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Calendar" primaryTypographyProps={{ variant: 'body2', fontWeight: 600 }} />
        </ListItemButton>

        <ListItemButton
          selected={location.pathname === '/dashboard' || location.pathname.endsWith('/dashboard')}
          onClick={() => {
            const targetWsId = currentWsId || localStorage.getItem('lastWorkspaceId') || workspaces[0]?.id;
            if (targetWsId) {
              navigate(`/workspaces/${targetWsId}/dashboard`);
            } else {
              navigate('/dashboard');
            }
            if (isMobile) onToggle();
          }}
          sx={{
            borderRadius: 2,
            mb: 0.5,
            py: 0.8,
            px: 1.5,
            '&.Mui-selected': { bgcolor: '#D9F3ED', color: '#115E59' },
          }}
        >
          <ListItemIcon sx={{ minWidth: 32, color: (location.pathname === '/dashboard' || location.pathname.endsWith('/dashboard')) ? '#0F766E' : '#84918D' }}>
            <MetricsIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="WS Dashboard" primaryTypographyProps={{ variant: 'body2', fontWeight: 600 }} />
        </ListItemButton>
      </List>

      <Divider sx={{ mx: 2 }} />

      <Box sx={{ px: 2.5, py: 1.5 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography
            variant="caption"
            sx={{
              fontWeight: 700,
              color: '#84918D',
              textTransform: 'uppercase',
              letterSpacing: '0.08em',
              fontSize: '1rem',
            }}
          >
            Workspaces
          </Typography>
          <IconButton
            size="small"
            aria-label="Create workspace"
            title="Create workspace"
            onClick={() => setCreateOpen(true)}
            sx={{
              color: '#0F766E',
              bgcolor: '#EEF3F0',
              width: 26,
              height: 26,
              '&:hover': { bgcolor: '#D9F3ED' },
            }}
          >
            <AddIcon sx={{ fontSize: 16 }} />
          </IconButton>
        </Box>
      </Box>

      <List sx={{ flex: 1, overflow: 'auto', px: 1.5 }} dense>
        {loading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} variant="rounded" height={42} sx={{ mb: 0.5, mx: 0.5 }} />
          ))
        ) : workspaces.length === 0 ? (
          <Typography
            variant="body2"
            sx={{ px: 2, py: 3, textAlign: 'center', color: '#84918D' }}
          >
            No workspaces yet
          </Typography>
        ) : (
          workspaces.map((ws) => {
            const selected = String(ws.id) === currentWsId;
            return (
              <ListItemButton
                key={ws.id}
                selected={selected}
                onClick={() => {
                  navigate(`/workspaces/${ws.id}`);
                  if (isMobile) onToggle();
                }}
                sx={{
                  borderRadius: 2,
                  mb: 0.25,
                  py: 1,
                  px: 1.5,
                  '&.Mui-selected': {
                    bgcolor: '#D9F3ED',
                    color: '#115E59',
                    '&:hover': { bgcolor: '#C7EBE3' },
                    '& .MuiListItemIcon-root': { color: '#0F766E' },
                    '& .ws-badge': { color: '#115E59', bgcolor: 'rgba(15,118,110,0.1)' },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 32, color: selected ? '#0F766E' : '#84918D' }}>
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      bgcolor: '#0F766E',
                    }}
                  />
                </ListItemIcon>
                <ListItemText
                  primary={ws.name}
                  sx={{ minWidth: 0 }}
                  primaryTypographyProps={{
                    variant: 'body2',
                    fontWeight: selected ? 600 : 500,
                    noWrap: true,
                    sx: { color: selected ? '#115E59' : '#42504C' },
                  }}
                />
                {isWorkspaceOwner(ws, user) && (
                  <Tooltip title="Workspace owner">
                    <OwnerIcon aria-label="Workspace owner" sx={{ color: 'warning.dark', fontSize: 16, ml: 0.5, mr: 0.5, flexShrink: 0 }} />
                  </Tooltip>
                )}
                <Box
                  className="ws-badge"
                  sx={{
                    px: 1,
                    py: 0.25,
                    borderRadius: 1,
                    fontSize: '1rem',
                    fontWeight: 600,
                    color: selected ? '#115E59' : '#84918D',
                    bgcolor: selected ? 'rgba(15,118,110,0.1)' : '#EEF3F0',
                  }}
                >
                  {ws.projectCount ?? 0}
                </Box>
              </ListItemButton>
            );
          })
        )}
      </List>

      <Box sx={{ p: 2, borderTop: '1px solid', borderColor: '#DCE4E0' }}>
        <Typography variant="caption" sx={{ color: '#CBD5D1', textAlign: 'center', display: 'block' }}>
          Task Manager v1.0
        </Typography>
      </Box>

      <CreateWorkspaceDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(workspace) => {
          navigate(`/workspaces/${workspace.id}`);
          if (isMobile) onToggle();
        }}
      />
    </Box>
  );

  if (isMobile) {
    return (
      <Drawer
        variant="temporary"
        open={open}
        onClose={onToggle}
        ModalProps={{ keepMounted: true }}
        sx={{ '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}
      >
        {content}
      </Drawer>
    );
  }

  return (
    <Drawer
      variant="persistent"
      open={open}
      sx={{
        width: open ? DRAWER_WIDTH : 0,
        flexShrink: 0,
        transition: 'width 200ms ease',
        '& .MuiDrawer-paper': {
          width: DRAWER_WIDTH,
          position: 'relative',
          border: 'none',
        },
      }}
    >
      {content}
    </Drawer>
  );
}
