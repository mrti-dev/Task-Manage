import { useNavigate } from 'react-router-dom';
import { Snackbar, Alert, Typography } from '@mui/material';
import { useNotification } from '../../contexts/NotificationContext';

export default function NotificationToast() {
  const { toast, setToast, markAsRead } = useNotification();
  const navigate = useNavigate();

  const handleClick = () => {
    if (!toast) return;
    if (!toast.isRead) markAsRead(toast.id);
    const target = toast.link;
    if (target) {
      if (toast.type === 'TASK_DELETED') {
        navigate(target.substring(0, target.lastIndexOf('/tasks/')));
      } else {
        navigate(target);
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
          bgcolor: '#17201F',
          '& .MuiAlert-icon': { color: '#0F766E' },
        }}
      >
        <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#FFFFFF' }}>
          {toast?.title || ''}
        </Typography>
        <Typography variant="body2" noWrap sx={{ maxWidth: 280, color: '#D9F3ED' }}>
          {toast?.content || ''}
        </Typography>
      </Alert>
    </Snackbar>
  );
}
