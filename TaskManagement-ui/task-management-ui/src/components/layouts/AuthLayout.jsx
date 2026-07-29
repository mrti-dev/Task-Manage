import { Outlet, Link } from 'react-router-dom';
import { Box, Container, Typography, Paper } from '@mui/material';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';

export default function AuthLayout() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #F1F5F9 0%, #DBEAFE 50%, #E2E8F0 100%)',
        py: 4,
        px: 2,
      }}
    >
      <Container maxWidth="sm">
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
              background: 'linear-gradient(135deg, #3B82F6, #60A5FA)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 12px rgba(59,130,246,0.3)',
            }}
          >
            <CheckCircleRoundedIcon sx={{ color: '#fff', fontSize: 26 }} />
          </Box>
          <Typography
            variant="h5"
            fontWeight={800}
            sx={{ color: '#0F172A', letterSpacing: '-0.02em' }}
          >
            Task Manager
          </Typography>
        </Box>

        <Paper
          elevation={0}
          sx={{
            p: 4,
            borderRadius: 4,
            border: '1px solid #E2E8F0',
            boxShadow: '0 4px 24px rgba(0,0,0,0.06)',
          }}
        >
          <Outlet />
        </Paper>

        <Typography
          variant="caption"
          align="center"
          display="block"
          sx={{ mt: 3, color: '#94A3B8' }}
        >
          &copy; {new Date().getFullYear()} Task Manager. All rights reserved.
        </Typography>
      </Container>
    </Box>
  );
}