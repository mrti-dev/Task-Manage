import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Popover,
  Box,
  Typography,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  IconButton,
  Button,
  Skeleton,
} from '@mui/material';
import {
  NotificationsRounded as NotificationsIcon,
  CheckCircleRounded as CheckIcon,
  InfoRounded as InfoIcon,
  WarningRounded as WarningIcon,
  TaskAltRounded as TaskIcon,
  FolderRounded as ProjectIcon,
  GroupRounded as GroupIcon,
  DeleteRounded as DeleteIcon,
  EditRounded as EditIcon,
  ChatRounded as CommentIcon,
} from '@mui/icons-material';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import { getNotificationsByUserApi } from '../../api/notificationApi';
import { useAuth } from '../../contexts/AuthContext';
import { useNotification } from '../../contexts/NotificationContext';

dayjs.extend(relativeTime);

const typeIcons = {
  WORKSPACE_CREATED: { icon: <GroupIcon />, color: '#3B82F6' },
  PROJECT_CREATED: { icon: <ProjectIcon />, color: '#0EA5E9' },
  PROJECT_UPDATED: { icon: <EditIcon />, color: '#F59E0B' },
  PROJECT_DELETED: { icon: <DeleteIcon />, color: '#EF4444' },
  TASK_CREATED: { icon: <TaskIcon />, color: '#22C55E' },
  TASK_ASSIGNED: { icon: <TaskIcon />, color: '#8B5CF6' },
  TASK_CLAIMED: { icon: <TaskIcon />, color: '#06B6D4' },
  TASK_STATUS_CHANGED: { icon: <InfoIcon />, color: '#F59E0B' },
  TASK_PRIORITY_CHANGED: { icon: <WarningIcon />, color: '#F97316' },
  TASK_DELETED: { icon: <DeleteIcon />, color: '#EF4444' },
  COMMENT_CREATED: { icon: <CommentIcon />, color: '#06B6D4' },
  WORKSPACE_MEMBER_ADDED: { icon: <GroupIcon />, color: '#22C55E' },
  WORKSPACE_MEMBER_ROLE_CHANGED: { icon: <GroupIcon />, color: '#F59E0B' },
};

export default function NotificationMenu({ anchorEl, onClose }) {
  const { user } = useAuth();
  const { markAsRead, markAllAsRead } = useNotification();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!anchorEl || !user?.id) return;
    const fetch = async () => {
      setLoading(true);
      try {
        const { data: res } = await getNotificationsByUserApi(user.id, { page: 0, size: 5 });
        setNotifications(res?.data?.content ?? []);
      } catch {
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [anchorEl, user]);

  const handleClick = async (notif) => {
    if (!notif.isRead) {
      await markAsRead(notif.id);
      setNotifications((prev) => prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n)));
    }
    onClose();
    if (notif.link) navigate(notif.link);
  };

  const getIcon = (type) => typeIcons[type] || { icon: <NotificationsIcon />, color: '#94A3B8' };

  return (
    <Popover
      open={Boolean(anchorEl)}
      anchorEl={anchorEl}
      onClose={onClose}
      transformOrigin={{ horizontal: 'right', vertical: 'top' }}
      anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      PaperProps={{
        sx: { width: 380, maxHeight: 500, borderRadius: 3, mt: 1, boxShadow: '0 8px 32px rgba(0,0,0,0.1)', border: '1px solid #E2E8F0' },
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2.5, py: 2 }}>
        <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#0F172A' }}>
          Notifications
        </Typography>
        <Button
          size="small"
          onClick={() => { markAllAsRead(); setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true }))); }}
          sx={{ textTransform: 'none', fontWeight: 600, color: '#3B82F6', fontSize: '0.8125rem' }}
        >
          Mark all read
        </Button>
      </Box>
      <Divider />

      {loading ? (
        <Box sx={{ p: 2 }}>
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} variant="rounded" height={64} sx={{ mb: 1 }} />
          ))}
        </Box>
      ) : notifications.length === 0 ? (
        <Box sx={{ py: 5, textAlign: 'center' }}>
          <NotificationsIcon sx={{ fontSize: 44, color: '#CBD5E1', mb: 1.5 }} />
          <Typography variant="body2" sx={{ color: '#94A3B8' }}>No notifications</Typography>
        </Box>
      ) : (
        <List disablePadding dense>
          {notifications.map((notif) => {
            const { icon, color } = getIcon(notif.type);
            return (
              <ListItemButton
                key={notif.id}
                onClick={() => handleClick(notif)}
                sx={{
                  px: 2.5,
                  py: 2,
                  bgcolor: notif.isRead ? 'transparent' : '#F1F5F9',
                  '&:hover': { bgcolor: '#F1F5F9' },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40, color }}>
                  {icon}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="body2" fontWeight={notif.isRead ? 500 : 700} sx={{ color: '#0F172A' }}>
                      {notif.title}
                    </Typography>
                  }
                  secondary={
                    <>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }} noWrap>
                        {notif.content}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#CBD5E1' }} display="block">
                        {dayjs(notif.createdAt).fromNow()}
                      </Typography>
                    </>
                  }
                />
                {!notif.isRead && (
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      markAsRead(notif.id);
                      setNotifications((prev) => prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n)));
                    }}
                    sx={{ color: '#3B82F6', alignSelf: 'flex-start', mt: 0.5 }}
                  >
                    <CheckIcon fontSize="small" />
                  </IconButton>
                )}
              </ListItemButton>
            );
          })}
        </List>
      )}

      <Divider />
      <Button
        fullWidth
        size="small"
        onClick={() => { onClose(); navigate('/notifications'); }}
        sx={{ textTransform: 'none', fontWeight: 600, color: '#3B82F6', py: 1.5 }}
      >
        View all notifications
      </Button>
    </Popover>
  );
}