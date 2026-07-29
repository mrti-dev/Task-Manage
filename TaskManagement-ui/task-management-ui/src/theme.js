import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#3B82F6',
      light: '#60A5FA',
      dark: '#2563EB',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#F1F5F9',
      light: '#F8FAFC',
      dark: '#CBD5E1',
    },
    error: {
      main: '#EF4444',
    },
    warning: {
      main: '#F59E0B',
    },
    success: {
      main: '#22C55E',
    },
    info: {
      main: '#0EA5E9',
    },
    background: {
      default: '#F1F5F9',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#0F172A',
      secondary: '#64748B',
      disabled: '#94A3B8',
    },
    divider: '#E2E8F0',
    action: {
      hover: '#F1F5F9',
      selected: '#DBEAFE',
      focus: '#EFF6FF',
    },
  },
  shape: {
    borderRadius: 12,
  },
  typography: {
    fontFamily: '"Plus Jakarta Sans", "Inter", "system-ui", "-apple-system", sans-serif',
    h4: {
      fontWeight: 700,
      fontSize: '1.75rem',
      lineHeight: 1.25,
      letterSpacing: '-0.02em',
    },
    h5: {
      fontWeight: 700,
      fontSize: '1.25rem',
      lineHeight: 1.3,
      letterSpacing: '-0.015em',
    },
    h6: {
      fontWeight: 600,
      fontSize: '1.0625rem',
      lineHeight: 1.35,
      letterSpacing: '-0.01em',
    },
    subtitle1: {
      fontWeight: 600,
      fontSize: '0.9375rem',
      letterSpacing: '-0.005em',
    },
    subtitle2: {
      fontWeight: 600,
      fontSize: '0.8125rem',
      letterSpacing: '-0.005em',
    },
    body1: {
      fontSize: '0.9375rem',
      lineHeight: 1.65,
    },
    body2: {
      fontSize: '0.8125rem',
      lineHeight: 1.55,
    },
    caption: {
      fontSize: '0.75rem',
      lineHeight: 1.4,
      color: '#94A3B8',
    },
    button: {
      fontWeight: 600,
      letterSpacing: '0.01em',
    },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          scrollbarWidth: 'thin',
          scrollbarColor: '#CBD5E1 transparent',
          WebkitFontSmoothing: 'antialiased',
          MozOsxFontSmoothing: 'grayscale',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          borderRadius: 10,
          padding: '9px 22px',
          fontSize: '0.875rem',
          transition: 'all 200ms cubic-bezier(0.16, 1, 0.3, 1)',
          '&:active': {
            transform: 'scale(0.96)',
          },
        },
        contained: {
          boxShadow: '0 1px 3px rgba(59,130,246,0.2)',
          '&:hover': {
            boxShadow: '0 6px 16px rgba(59,130,246,0.3)',
            transform: 'translateY(-1px)',
          },
        },
        outlined: {
          '&:hover': {
            transform: 'translateY(-1px)',
            boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
          },
        },
        sizeSmall: {
          padding: '6px 14px',
          fontSize: '0.8125rem',
        },
        sizeLarge: {
          padding: '12px 28px',
          fontSize: '0.9375rem',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          boxShadow: '0 1px 2px rgba(0,0,0,0.04), 0 1px 3px rgba(0,0,0,0.04)',
          transition: 'all 250ms cubic-bezier(0.16, 1, 0.3, 1)',
          '&:hover': {
            boxShadow: '0 8px 24px rgba(0,0,0,0.06), 0 2px 8px rgba(0,0,0,0.04)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 16,
        },
        outlined: {
          border: '1px solid #E2E8F0',
        },
        elevation1: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.03)',
        },
        elevation2: {
          boxShadow: '0 4px 12px rgba(0,0,0,0.05), 0 2px 4px rgba(0,0,0,0.03)',
        },
        elevation4: {
          boxShadow: '0 8px 24px rgba(0,0,0,0.06), 0 4px 8px rgba(0,0,0,0.03)',
        },
        elevation8: {
          boxShadow: '0 16px 40px rgba(0,0,0,0.08), 0 8px 16px rgba(0,0,0,0.04)',
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 20,
          boxShadow: '0 24px 80px rgba(0,0,0,0.12), 0 8px 24px rgba(0,0,0,0.06)',
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 10,
            transition: 'all 200ms cubic-bezier(0.16, 1, 0.3, 1)',
            backgroundColor: '#FAFAFA',
            '&:hover': {
              backgroundColor: '#FFFFFF',
              '& .MuiOutlinedInput-notchedOutline': {
                borderColor: '#3B82F6',
              },
            },
            '&.Mui-focused': {
              backgroundColor: '#FFFFFF',
              '& .MuiOutlinedInput-notchedOutline': {
                borderWidth: '2px',
                borderColor: '#3B82F6',
              },
            },
            '&.Mui-error': {
              '&:hover .MuiOutlinedInput-notchedOutline': {
                borderColor: '#EF4444',
              },
            },
          },
          '& .MuiInputLabel-root': {
            fontWeight: 500,
            color: '#64748B',
            '&.Mui-focused': {
              color: '#3B82F6',
            },
          },
          '& .MuiFormHelperText-root': {
            fontWeight: 500,
            fontSize: '0.75rem',
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          fontWeight: 600,
          fontSize: '0.75rem',
          height: 28,
          transition: 'all 150ms ease',
        },
        filled: {
          '&.MuiChip-colorDefault': {
            backgroundColor: '#F1F5F9',
            color: '#475569',
          },
        },
        sizeSmall: {
          height: 22,
          fontSize: '0.6875rem',
        },
      },
    },
    MuiTableContainer: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          border: '1px solid #F1F5F9',
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            fontWeight: 700,
            fontSize: '0.6875rem',
            textTransform: 'uppercase',
            letterSpacing: '0.06em',
            color: '#64748B',
            backgroundColor: '#F8FAFC',
            borderBottom: '2px solid #E2E8F0',
            padding: '14px 16px',
          },
        },
      },
    },
    MuiTableBody: {
      styleOverrides: {
        root: {
          '& .MuiTableRow-root': {
            transition: 'background-color 120ms ease',
            '&:hover': {
              backgroundColor: '#F1F5F9',
            },
            '&:last-child .MuiTableCell-body': {
              borderBottom: 'none',
            },
          },
          '& .MuiTableCell-body': {
            padding: '14px 16px',
            fontSize: '0.875rem',
            borderBottomColor: '#F1F5F9',
          },
        },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          fontSize: '0.875rem',
          minHeight: 48,
          padding: '8px 20px',
          borderRadius: '8px 8px 0 0',
          transition: 'all 150ms ease',
          '&.Mui-selected': {
            color: '#3B82F6',
          },
          '&:hover': {
            backgroundColor: 'rgba(59,130,246,0.04)',
          },
        },
      },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: {
          backgroundColor: '#3B82F6',
          height: 3,
          borderRadius: '3px 3px 0 0',
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          fontWeight: 500,
        },
        filled: {
          boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        },
        standard: {
          border: '1px solid',
        },
      },
    },
    MuiSnackbar: {
      styleOverrides: {
        root: {
          '& .MuiAlert-root': {
            minWidth: 340,
          },
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          margin: '2px 0',
          transition: 'all 120ms ease',
          '&:hover': {
            backgroundColor: '#F1F5F9',
          },
          '&.Mui-selected': {
            backgroundColor: '#3B82F6',
            color: '#FFFFFF',
            '&:hover': {
              backgroundColor: '#2563EB',
            },
            '& .MuiListItemIcon-root': {
              color: '#FFFFFF',
            },
          },
        },
      },
    },
    MuiAvatar: {
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: '0.875rem',
        },
      },
    },
    MuiSkeleton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transform: 'scale(1)',
        },
      },
    },
    MuiDivider: {
      styleOverrides: {
        root: {
          borderColor: '#E2E8F0',
        },
      },
    },
    MuiSelect: {
      styleOverrides: {
        root: {
          borderRadius: 10,
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          backdropFilter: 'blur(8px)',
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          borderRight: '1px solid #E2E8F0',
        },
      },
    },
    MuiBadge: {
      styleOverrides: {
        badge: {
          fontWeight: 700,
          fontSize: '0.6875rem',
          minWidth: 18,
          height: 18,
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          borderRadius: 8,
          padding: '6px 12px',
          fontSize: '0.75rem',
          fontWeight: 500,
          backgroundColor: '#0F172A',
        },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          borderRadius: 14,
          boxShadow: '0 8px 32px rgba(0,0,0,0.1), 0 2px 8px rgba(0,0,0,0.04)',
          border: '1px solid #E2E8F0',
        },
      },
    },
    MuiDialogActions: {
      styleOverrides: {
        root: {
          padding: '16px 24px',
          gap: 8,
        },
      },
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: {
          padding: '24px 24px 8px',
          fontWeight: 700,
        },
      },
    },
    MuiDialogContent: {
      styleOverrides: {
        root: {
          padding: '16px 24px',
        },
      },
    },
  },
});

export default theme;
