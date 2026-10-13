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
    // oxlint-disable-next-line react/set-state-in-effect -- đồng bộ trạng thái với breakpoint
    if (isMobile) setSidebarOpen(false);
  }, [isMobile]);

  return (
    <NotificationProvider>
      <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
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
              px: { xs: 4, sm: 6, lg: 8 },
              py: { xs: 5, md: 7 },
              overflow: 'auto',
              width: '100%',
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
