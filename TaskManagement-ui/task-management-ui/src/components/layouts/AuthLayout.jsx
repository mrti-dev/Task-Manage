import { Outlet, Link } from 'react-router-dom';
import { Box, Container, Typography, Paper } from '@mui/material';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';

const CURRENT_YEAR = new Date().getFullYear();

export default function AuthLayout() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#EEF3F0',
        py: 4,
        px: 2,
      }}
    >
      <Container disableGutters sx={{ width: '100%', maxWidth: '480px !important' }}>
        <Box
          component={Link}
          to="/"
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 1.5,
            mb: 4,
            textDecoration: 'none',
          }}
        >
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 3,
              background: '#0F766E',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 12px rgba(15,118,110,0.3)',
            }}
          >
            <CheckCircleRoundedIcon sx={{ color: '#fff', fontSize: 26 }} />
          </Box>
          <Typography
            variant="h5"
            fontWeight={800}
            sx={{ color: '#17201F', letterSpacing: '-0.02em' }}
          >
            Task Manager
          </Typography>
        </Box>

        <Paper
          elevation={0}
          sx={{
            p: { xs: 4, sm: 6 },
            borderRadius: 4,
            border: '1px solid #DCE4E0',
            boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
          }}
        >
          <Outlet />
        </Paper>

        <Typography
          variant="caption"
          align="center"
          display="block"
          sx={{ mt: 3, color: '#84918D' }}
        >
          &copy; {CURRENT_YEAR} Task Manager. All rights reserved.
        </Typography>
      </Container>
    </Box>
  );
}
