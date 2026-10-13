import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  IconButton,
  Avatar,
  Typography,
  Tooltip,
  Menu,
  MenuItem,
  ListItemIcon,
  Divider,
  Badge,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  InputBase,
  Paper,
  CircularProgress,
  Chip,
  Popper,
  ClickAwayListener,
} from '@mui/material';
import {
  MenuRounded as MenuIcon,
  LogoutRounded as LogoutIcon,
  NotificationsRounded as NotificationsIcon,
  PersonRounded as PersonIcon,
  SearchRounded as SearchIcon,
  ClearRounded as ClearIcon,
  BusinessRounded as WorkspaceIcon,
  FolderRounded as ProjectIcon,
  AssignmentRounded as TaskIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useNotification } from '../../contexts/NotificationContext';
import NotificationMenu from '../notification/NotificationMenu';
import { searchApi } from '../../api/searchApi';

export default function Header({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const { unreadCount } = useNotification();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const [notificationAnchor, setNotificationAnchor] = useState(null);
  const [profileOpen, setProfileOpen] = useState(false);

  // Global search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchPopperOpen, setSearchPopperOpen] = useState(false);
  const [searchAnchor, setSearchAnchor] = useState(null);

  useEffect(() => {
    if (!searchQuery.trim() || searchQuery.trim().length < 2) {
      // oxlint-disable-next-line react/set-state-in-effect -- xóa kết quả cũ khi truy vấn quá ngắn
      setSearchResults(null);
      setSearchLoading(false);
      return;
    }

    const timer = setTimeout(async () => {
      setSearchLoading(true);
      try {
        const { data: res } = await searchApi({ q: searchQuery.trim() });
        setSearchResults(res?.data ?? null);
        setSearchPopperOpen(true);
      } catch (err) {
        console.warn('Search failed:', err);
      } finally {
        setSearchLoading(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  const handleLogout = async () => {
    setAnchorEl(null);
    await logout();
    navigate('/login', { replace: true });
  };

  const hasResults = searchResults && (
    (searchResults.workspaces && searchResults.workspaces.length > 0) ||
    (searchResults.projects && searchResults.projects.length > 0) ||
    (searchResults.tasks && searchResults.tasks.length > 0)
  );

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        px: { xs: 2, md: 3 },
        py: 2,
        bgcolor: 'rgba(255,255,255,0.92)',
        backdropFilter: 'blur(12px)',
        position: 'sticky',
        top: 0,
        zIndex: 20,
        borderBottom: '1px solid rgba(23,32,31,0.08)',
      }}
    >
      <IconButton
        onClick={onToggleSidebar}
        sx={{
          ml: 0,
          mr: 1,
          color: '#66736F',
          bgcolor: '#F7FAF8',
          width: 36,
          height: 36,
          borderRadius: 2,
          '&:hover': { bgcolor: '#EEF3F0' },
        }}
      >
        <MenuIcon sx={{ fontSize: 20 }} />
      </IconButton>

      {/* Global Search Bar */}
      <Box sx={{ position: 'relative', width: { xs: 180, sm: 300, md: 400 }, ml: { xs: 1, sm: 2 } }}>
        <Paper
          ref={setSearchAnchor}
          elevation={0}
          sx={{
            display: 'flex',
            alignItems: 'center',
            px: 1.5,
            py: 0.5,
            borderRadius: 3,
            bgcolor: '#F3F6F4',
            border: '1px solid rgba(23,32,31,0.08)',
            '&:focus-within': {
              borderColor: '#0F766E',
              bgcolor: '#FFFFFF',
              boxShadow: '0 0 0 3px rgba(15,118,110,0.12)',
            },
          }}
        >
          <SearchIcon sx={{ color: '#84918D', fontSize: 20, mr: 1 }} />
          <InputBase
            placeholder="Search workspaces, projects, tasks..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onFocus={() => { if (searchResults) setSearchPopperOpen(true); }}
            sx={{ flex: 1, fontSize: '1rem' }}
          />
          {searchLoading && <CircularProgress size={16} sx={{ color: '#0F766E', mr: 0.5 }} />}
          {searchQuery && (
            <IconButton size="small" onClick={() => { setSearchQuery(''); setSearchResults(null); setSearchPopperOpen(false); }}>
              <ClearIcon sx={{ fontSize: 16 }} />
            </IconButton>
          )}
        </Paper>

        <Popper
          open={searchPopperOpen && Boolean(searchResults)}
          anchorEl={searchAnchor}
          placement="bottom-start"
          style={{ width: searchAnchor?.clientWidth ?? 380, zIndex: 1300 }}
        >
          <ClickAwayListener onClickAway={() => setSearchPopperOpen(false)}>
            <Paper
              elevation={4}
              sx={{
                mt: 1,
                maxHeight: 420,
                overflowY: 'auto',
                borderRadius: 2.5,
                p: 1.5,
                bgcolor: '#FFFFFF',
                border: '1px solid #DCE4E0',
              }}
            >
              {!hasResults && !searchLoading && (
                <Typography variant="body2" sx={{ color: '#84918D', textAlign: 'center', py: 2 }}>
                  No results found for &ldquo;{searchQuery}&rdquo;
                </Typography>
              )}

              {/* Workspaces */}
              {searchResults?.workspaces?.length > 0 && (
                <Box sx={{ mb: 1.5 }}>
                  <Typography variant="caption" sx={{ fontWeight: 700, color: '#84918D', textTransform: 'uppercase', px: 1 }}>
                    Workspaces
                  </Typography>
                  {searchResults.workspaces.map((ws) => (
                    <Box
                      key={ws.id}
                      onClick={() => {
                        setSearchPopperOpen(false);
                        navigate(`/workspaces/${ws.id}`);
                      }}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1.5,
                        p: 1,
                        borderRadius: 1.5,
                        cursor: 'pointer',
                        '&:hover': { bgcolor: '#F0FDF4' },
                      }}
                    >
                      <WorkspaceIcon sx={{ fontSize: 18, color: '#0F766E' }} />
                      <Typography variant="body2" fontWeight={600} noWrap>
                        {ws.name}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              )}

              {/* Projects */}
              {searchResults?.projects?.length > 0 && (
                <Box sx={{ mb: 1.5 }}>
                  <Typography variant="caption" sx={{ fontWeight: 700, color: '#84918D', textTransform: 'uppercase', px: 1 }}>
                    Projects
                  </Typography>
                  {searchResults.projects.map((p) => (
                    <Box
                      key={p.id}
                      onClick={() => {
                        setSearchPopperOpen(false);
                        navigate(`/projects/${p.id}`);
                      }}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1.5,
                        p: 1,
                        borderRadius: 1.5,
                        cursor: 'pointer',
                        '&:hover': { bgcolor: '#F0FDF4' },
                      }}
                    >
                      <ProjectIcon sx={{ fontSize: 18, color: '#0284C7' }} />
                      <Typography variant="body2" fontWeight={600} noWrap>
                        {p.name}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              )}

              {/* Tasks */}
              {searchResults?.tasks?.length > 0 && (
                <Box>
                  <Typography variant="caption" sx={{ fontWeight: 700, color: '#84918D', textTransform: 'uppercase', px: 1 }}>
                    Tasks
                  </Typography>
                  {searchResults.tasks.map((t) => (
                    <Box
                      key={t.id}
                      onClick={() => {
                        setSearchPopperOpen(false);
                        navigate(`/workspaces/${t.workspaceId}/projects/${t.projectId}/tasks/${t.id}`);
                      }}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        p: 1,
                        borderRadius: 1.5,
                        cursor: 'pointer',
                        '&:hover': { bgcolor: '#F0FDF4' },
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, overflow: 'hidden' }}>
                        <TaskIcon sx={{ fontSize: 18, color: '#D97706' }} />
                        <Typography variant="body2" fontWeight={600} noWrap>
                          {t.title}
                        </Typography>
                      </Box>
                      {t.status && (
                        <Chip
                          size="small"
                          label={t.status}
                          sx={{ fontSize: '0.775rem', height: 20 }}
                        />
                      )}
                    </Box>
                  ))}
                </Box>
              )}
            </Paper>
          </ClickAwayListener>
        </Popper>
      </Box>

      <Box sx={{ flex: 1 }} />

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Tooltip title="Notifications" arrow>
          <IconButton
            onClick={(e) => setNotificationAnchor(e.currentTarget)}
            sx={{
              color: '#66736F',
              bgcolor: '#F7FAF8',
              width: 36,
              height: 36,
              borderRadius: 2,
              '&:hover': { bgcolor: '#EEF3F0' },
            }}
          >
            <Badge badgeContent={unreadCount} color="error" max={99}>
              <NotificationsIcon sx={{ fontSize: 20 }} />
            </Badge>
          </IconButton>
        </Tooltip>

        <NotificationMenu
          anchorEl={notificationAnchor}
          onClose={() => setNotificationAnchor(null)}
        />

        <Tooltip title="Account" arrow>
          <IconButton
            onClick={(e) => setAnchorEl(e.currentTarget)}
            sx={{ ml: 0.5, p: 0.5 }}
          >
            <Avatar
              src={user?.avatar || ''}
              sx={{
                width: 34,
                height: 34,
                bgcolor: '#0F766E',
                fontSize: '1rem',
                fontWeight: 700,
              }}
            >
              {user?.username?.[0]?.toUpperCase() || 'U'}
            </Avatar>
          </IconButton>
        </Tooltip>
      </Box>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        PaperProps={{
          sx: {
            mt: 1,
            minWidth: 220,
            borderRadius: 3,
            boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
            border: '1px solid #DCE4E0',
          },
        }}
      >
        <Box sx={{ px: 2.5, py: 1.5 }}>
          <Typography variant="subtitle2" fontWeight={600} sx={{ color: '#17201F' }}>
            {user?.username || 'User'}
          </Typography>
          <Typography variant="caption" sx={{ color: '#84918D' }}>
            {user?.email || ''}
          </Typography>
        </Box>
        <MenuItem onClick={() => { setAnchorEl(null); setProfileOpen(true); }}
          sx={{ mx: 1, borderRadius: 2, my: 0.25 }}>
          <ListItemIcon><PersonIcon fontSize="small" sx={{ color: '#66736F' }} /></ListItemIcon>
          <Typography variant="body2">Profile</Typography>
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => { setAnchorEl(null); navigate('/notifications'); }}
          sx={{ mx: 1, borderRadius: 2, my: 0.25 }}>
          <ListItemIcon><NotificationsIcon fontSize="small" sx={{ color: '#66736F' }} /></ListItemIcon>
          <Typography variant="body2">Notifications</Typography>
        </MenuItem>
        <Divider />
        <MenuItem
          onClick={handleLogout}
          sx={{ mx: 1, borderRadius: 2, my: 0.25, color: '#EF4444' }}
        >
          <ListItemIcon>
            <LogoutIcon fontSize="small" sx={{ color: '#EF4444' }} />
          </ListItemIcon>
          <Typography variant="body2">Sign Out</Typography>
        </MenuItem>
      </Menu>

      <Dialog open={profileOpen} onClose={() => setProfileOpen(false)} maxWidth="xs" fullWidth
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F', textAlign: 'center' }}>Profile</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, pb: 3 }}>
          <Avatar src={user?.avatar || ''}
            sx={{ width: 88, height: 88, bgcolor: '#0F766E', fontSize: 34, fontWeight: 700, boxShadow: '0 4px 16px rgba(15,118,110,0.3)' }}>
            {user?.username?.[0]?.toUpperCase() || 'U'}
          </Avatar>
          <Box sx={{ textAlign: 'center' }}>
            <Typography variant="h6" fontWeight={700} sx={{ color: '#17201F' }}>{user?.username || 'User'}</Typography>
            <Typography variant="body2" sx={{ color: '#66736F' }}>{user?.email || ''}</Typography>
          </Box>
        </DialogContent>
        <DialogActions sx={{ justifyContent: 'center', pb: 2 }}>
          <Button onClick={() => setProfileOpen(false)} sx={{ color: '#66736F', fontWeight: 600 }}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
