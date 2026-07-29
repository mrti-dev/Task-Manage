import { useNavigate } from 'react-router-dom';
import { Snackbar, Alert, Typography } from '@mui/material';
import { useNotification } from '../../contexts/NotificationContext';

export default function NotificationToast() {
  const { toast, setToast, markAsRead } = useNotification();
  const navigate = useNavigate();

  const handleClick = () => {
    if (!toast) return;
    if (!toast.isRead) markAsRead(toast.id);
    if (toast.link) {
      if (toast.type === 'TASK_DELETED') {
        navigate(toast.link.substring(0, toast.link.lastIndexOf('/tasks/')));
      } else {
        navigate(toast.link);
      }
    }
    setToast(null);
  };

  return (
    <Snackbar
      open={Boolean(toast)}
      autoHideDuration={5000}
      onClose={() => setToast(null)}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
    >
      <Alert
        onClick={handleClick}
        onClose={() => setToast(null)}
        severity="info"
        variant="filled"
        sx={{
          borderRadius: 3,
          minWidth: 340,
          cursor: 'pointer',
          bgcolor: '#0F172A',
          '& .MuiAlert-icon': { color: '#3B82F6' },
        }}
      >
        <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#FFFFFF' }}>
          {toast?.title || ''}
        </Typography>
        <Typography variant="body2" noWrap sx={{ maxWidth: 280, color: '#DBEAFE' }}>
          {toast?.content || ''}
        </Typography>
      </Alert>
    </Snackbar>
  );
}