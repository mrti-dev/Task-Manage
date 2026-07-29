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
} from '@mui/material';
import {
  DashboardRounded as DashboardIcon,
  ChevronLeftRounded as CollapseIcon,
  AddRounded as AddIcon,
  SpaceDashboardRounded as WorkspaceIcon,
} from '@mui/icons-material';
import { getWorkspacesApi } from '../../api/workspaceApi';

const DRAWER_WIDTH = 270;

export default function Sidebar({ open, onToggle }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const location = useLocation();
  const [workspaces, setWorkspaces] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!open) return;
    const fetch = async () => {
      setLoading(true);
      try {
        const { data: res } = await getWorkspacesApi();
        setWorkspaces(res?.data ?? []);
      } catch {
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [open]);

  const currentWsId = location.pathname.match(/\/workspaces\/(\d+)/)?.[1];

  const content = (
    <Box
      sx={{
        width: DRAWER_WIDTH,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: '#F8FAFC',
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
              background: 'linear-gradient(135deg, #3B82F6, #60A5FA)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 8px rgba(59,130,246,0.3)',
            }}
          >
            <DashboardIcon sx={{ color: '#fff', fontSize: 20 }} />
          </Box>
          <Box>
            <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#0F172A', lineHeight: 1.2 }}>
              Task Manager
            </Typography>
            <Typography variant="caption" sx={{ color: '#94A3B8' }}>
              Workspace dashboard
            </Typography>
          </Box>
        </Box>
        {isMobile && (
          <IconButton onClick={onToggle} size="small" sx={{ color: '#94A3B8' }}>
            <CollapseIcon />
          </IconButton>
        )}
      </Box>

      <Divider sx={{ mx: 2 }} />

      <Box sx={{ px: 2.5, py: 1.5 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography
            variant="caption"
            sx={{
              fontWeight: 700,
              color: '#94A3B8',
              textTransform: 'uppercase',
              letterSpacing: '0.08em',
              fontSize: '0.6875rem',
            }}
          >
            Workspaces
          </Typography>
          <IconButton
            size="small"
            onClick={() => navigate('/')}
            sx={{
              color: '#3B82F6',
              bgcolor: '#F1F5F9',
              width: 26,
              height: 26,
              '&:hover': { bgcolor: '#DBEAFE' },
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
            sx={{ px: 2, py: 3, textAlign: 'center', color: '#94A3B8' }}
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
                    bgcolor: '#3B82F6',
                    color: '#fff',
                    '&:hover': { bgcolor: '#2563EB' },
                    '& .MuiListItemIcon-root': { color: '#fff' },
                    '& .ws-badge': { color: '#fff', bgcolor: 'rgba(255,255,255,0.2)' },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 32, color: selected ? '#fff' : '#94A3B8' }}>
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      bgcolor: selected ? '#fff' : '#3B82F6',
                    }}
                  />
                </ListItemIcon>
                <ListItemText
                  primary={ws.name}
                  primaryTypographyProps={{
                    variant: 'body2',
                    fontWeight: selected ? 600 : 500,
                    noWrap: true,
                    sx: { color: selected ? '#fff' : '#475569' },
                  }}
                />
                <Box
                  className="ws-badge"
                  sx={{
                    px: 1,
                    py: 0.25,
                    borderRadius: 1,
                    fontSize: '0.6875rem',
                    fontWeight: 600,
                    color: selected ? '#3B82F6' : '#94A3B8',
                    bgcolor: selected ? 'rgba(255,255,255,0.2)' : '#F1F5F9',
                  }}
                >
                  {ws.projectCount ?? 0}
                </Box>
              </ListItemButton>
            );
          })
        )}
      </List>

      <Box sx={{ p: 2, borderTop: '1px solid', borderColor: '#E2E8F0' }}>
        <Typography variant="caption" sx={{ color: '#CBD5E1', textAlign: 'center', display: 'block' }}>
          Task Manager v1.0
        </Typography>
      </Box>
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