import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import Sidebar from '../sidebar/Sidebar';
import Header from '../header/Header';
import NotificationToast from '../notification/NotificationToast';
import { NotificationProvider } from '../../contexts/NotificationContext';

export default function MainLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);

  useEffect(() => {
    if (isMobile) setSidebarOpen(false);
  }, [isMobile]);

  return (
    <NotificationProvider>
      <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#F1F5F9' }}>
        <Sidebar open={sidebarOpen} onToggle={() => setSidebarOpen(!sidebarOpen)} />
        <Box
          sx={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            minWidth: 0,
            transition: 'margin-left 200ms ease',
          }}
        >
          <Header onToggleSidebar={() => setSidebarOpen(!sidebarOpen)} />
          <Box
            component="main"
            sx={{
              flex: 1,
              p: { xs: 2, md: 3 },
              overflow: 'auto',
            }}
          >
            <Outlet />
          </Box>
        </Box>
      </Box>
      <NotificationToast />
    </NotificationProvider>
  );
}