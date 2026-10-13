import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Pagination,
  Skeleton,
  Snackbar,
  Alert,
  IconButton,
} from '@mui/material';
import {
  ArrowBackRounded as BackIcon,
  CheckCircleRounded as CheckIcon,
  DoneAllRounded as DoneAllIcon,
  NotificationsRounded as NotificationsIcon,
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
import EmptyState from '../../components/common/EmptyState';
import { on } from '../../utils/eventBus';

dayjs.extend(relativeTime);

const typeIcons = {
  WORKSPACE_CREATED: { icon: <GroupIcon />, color: '#0F766E' },
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
  WORKSPACE_INVITATION_SENT: { icon: <GroupIcon />, color: '#0F766E' },
};

export default function NotificationPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { markAsRead, markAllAsRead } = useNotification();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const fetch = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    try {
      const { data: res } = await getNotificationsByUserApi(user.id, { page: page - 1, size: 20 });
      const pd = res?.data;
      setNotifications(pd?.content ?? []);
      setTotalPages(pd?.totalPages ?? 1);
    } catch {
      setSnackbar({ open: true, message: 'Failed to load notifications', severity: 'error' });
    } finally { setLoading(false); }
  }, [user, page]);

  // oxlint-disable-next-line react/set-state-in-effect -- tải dữ liệu theo trang hiện tại
  useEffect(() => { fetch(); }, [fetch]);

  useEffect(() => {
    const unsub = on('NOTIFICATION_NEW', (notif) => {
      setNotifications((prev) => {
        if (prev.some((n) => n.id === notif.id)) return prev;
        return [notif, ...prev];
      });
    });
    return unsub;
  }, []);

  const handleMarkAsRead = async (id) => {
    await markAsRead(id);
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
  };

  const handleMarkAllAsRead = async () => {
    await markAllAsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    setSnackbar({ open: true, message: 'All marked as read', severity: 'success' });
  };

  const handleClick = (notif) => {
    if (!notif.isRead) handleMarkAsRead(notif.id);
    if (notif.link) navigate(notif.link);
  };

  const getIcon = (type) => typeIcons[type] || { icon: <NotificationsIcon />, color: '#84918D' };

  return (
    <Box sx={{ maxWidth: 720, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
        <IconButton onClick={() => navigate('/')} size="small" sx={{ bgcolor: '#F7FAF8', borderRadius: 2 }}>
          <BackIcon sx={{ fontSize: 20, color: '#66736F' }} />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h4" sx={{ color: '#17201F' }}>Notifications</Typography>
        </Box>
        <Button startIcon={<DoneAllIcon />} onClick={handleMarkAllAsRead} size="small"
          sx={{ color: '#0F766E', fontWeight: 600, textTransform: 'none' }}>
          Mark all read
        </Button>
      </Box>

      {loading ? (
        <Box>
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} variant="rounded" height={72} sx={{ mb: 1, borderRadius: 3 }} />
          ))}
        </Box>
      ) : notifications.length === 0 ? (
        <EmptyState icon={<NotificationsIcon sx={{ fontSize: 36 }} />} title="No notifications" description="You're all caught up!" />
      ) : (
        <>
          <List disablePadding sx={{ bgcolor: '#FFFFFF', borderRadius: 3, overflow: 'hidden', border: '1px solid #DCE4E0' }}>
            {notifications.map((notif, index) => {
              const { icon, color } = getIcon(notif.type);
              return (
                <Box key={notif.id}>
                  {index > 0 && <Divider component="li" />}
                  <ListItemButton onClick={() => handleClick(notif)}
                    sx={{ py: 2, px: 2.5, bgcolor: notif.isRead ? 'transparent' : '#EEF3F0', '&:hover': { bgcolor: '#EEF3F0' } }}>
                    <ListItemIcon sx={{ minWidth: 44, color }}>{icon}</ListItemIcon>
                    <ListItemText
                      primary={<Typography variant="body2" fontWeight={notif.isRead ? 500 : 700} sx={{ color: '#17201F' }}>{notif.title}</Typography>}
                      secondary={
                        <Box>
                          <Typography variant="caption" sx={{ color: '#84918D' }}>{notif.content}</Typography>
                          <Typography variant="caption" display="block" sx={{ color: '#CBD5D1' }}>{dayjs(notif.createdAt).fromNow()}</Typography>
                        </Box>
                      } />
                    {!notif.isRead && (
                      <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleMarkAsRead(notif.id); }}
                        sx={{ color: '#0F766E' }}>
                        <CheckIcon fontSize="small" />
                      </IconButton>
                    )}
                  </ListItemButton>
                </Box>
              );
            })}
          </List>
          {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
              <Pagination count={totalPages} page={page} onChange={(_, v) => setPage(v)}
                color="primary" sx={{ '& .MuiPaginationItem-root.Mui-selected': { bgcolor: '#0F766E', color: '#fff' } }} />
            </Box>
          )}
        </>
      )}

      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar((p) => ({ ...p, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setSnackbar((p) => ({ ...p, open: false }))} severity={snackbar.severity}
          variant="filled" sx={{ borderRadius: 3 }}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
