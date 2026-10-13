import { Box, Typography } from '@mui/material';
import InboxRoundedIcon from '@mui/icons-material/InboxRounded';

export default function EmptyState({ icon, title = 'No data', description, action }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        py: 8,
        px: 2,
      }}
    >
      <Box
        sx={{
          width: 80,
          height: 80,
          borderRadius: '50%',
          bgcolor: '#EEF3F0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mb: 2.5,
        }}
      >
        {icon || <InboxRoundedIcon sx={{ fontSize: 36, color: '#0F766E' }} />}
      </Box>
      <Typography variant="h6" fontWeight={600} sx={{ mb: 0.5, color: '#17201F' }}>
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" sx={{ color: '#84918D', mb: 2.5, textAlign: 'center', maxWidth: 300 }}>
          {description}
        </Typography>
      )}
      {action}
    </Box>
  );
}