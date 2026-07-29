import { useState } from 'react';
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
} from '@mui/material';
import {
  MenuRounded as MenuIcon,
  LogoutRounded as LogoutIcon,
  NotificationsRounded as NotificationsIcon,
  PersonRounded as PersonIcon,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useNotification } from '../../contexts/NotificationContext';
import NotificationMenu from '../notification/NotificationMenu';

export default function Header({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const { unreadCount } = useNotification();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const [notificationAnchor, setNotificationAnchor] = useState(null);
  const [profileOpen, setProfileOpen] = useState(false);

  const handleLogout = async () => {
    setAnchorEl(null);
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        px: { xs: 2, md: 3 },
        py: 1.5,
        bgcolor: '#FFFFFF',
        borderBottom: '1px solid #E2E8F0',
      }}
    >
      <IconButton
        edge="start"
        onClick={onToggleSidebar}
        sx={{
          mr: 1,
          color: '#64748B',
          bgcolor: '#F8FAFC',
          width: 36,
          height: 36,
          borderRadius: 2,
          '&:hover': { bgcolor: '#F1F5F9' },
        }}
      >
        <MenuIcon sx={{ fontSize: 20 }} />
      </IconButton>

      <Box sx={{ flex: 1 }} />

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Tooltip title="Notifications" arrow>
          <IconButton
            onClick={(e) => setNotificationAnchor(e.currentTarget)}
            sx={{
              color: '#64748B',
              bgcolor: '#F8FAFC',
              width: 36,
              height: 36,
              borderRadius: 2,
              '&:hover': { bgcolor: '#F1F5F9' },
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
                bgcolor: '#3B82F6',
                fontSize: '0.875rem',
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
            border: '1px solid #E2E8F0',
          },
        }}
      >
        <Box sx={{ px: 2.5, py: 1.5 }}>
          <Typography variant="subtitle2" fontWeight={600} sx={{ color: '#0F172A' }}>
            {user?.username || 'User'}
          </Typography>
          <Typography variant="caption" sx={{ color: '#94A3B8' }}>
            {user?.email || ''}
          </Typography>
        </Box>
        <MenuItem onClick={() => { setAnchorEl(null); setProfileOpen(true); }}
          sx={{ mx: 1, borderRadius: 2, my: 0.25 }}>
          <ListItemIcon><PersonIcon fontSize="small" sx={{ color: '#64748B' }} /></ListItemIcon>
          <Typography variant="body2">Profile</Typography>
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => { setAnchorEl(null); navigate('/notifications'); }}
          sx={{ mx: 1, borderRadius: 2, my: 0.25 }}>
          <ListItemIcon><NotificationsIcon fontSize="small" sx={{ color: '#64748B' }} /></ListItemIcon>
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
        <DialogTitle sx={{ fontWeight: 700, color: '#0F172A', textAlign: 'center' }}>Profile</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, pb: 3 }}>
          <Avatar src={user?.avatar || ''}
            sx={{ width: 88, height: 88, bgcolor: '#3B82F6', fontSize: 32, fontWeight: 700, boxShadow: '0 4px 16px rgba(59,130,246,0.3)' }}>
            {user?.username?.[0]?.toUpperCase() || 'U'}
          </Avatar>
          <Box sx={{ textAlign: 'center' }}>
            <Typography variant="h6" fontWeight={700} sx={{ color: '#0F172A' }}>{user?.username || 'User'}</Typography>
            <Typography variant="body2" sx={{ color: '#64748B' }}>{user?.email || ''}</Typography>
          </Box>
        </DialogContent>
        <DialogActions sx={{ justifyContent: 'center', pb: 2 }}>
          <Button onClick={() => setProfileOpen(false)} sx={{ color: '#64748B', fontWeight: 600 }}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}