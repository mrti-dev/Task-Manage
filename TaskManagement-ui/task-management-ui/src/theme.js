import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#0F766E',
      light: '#14B8A6',
      dark: '#115E59',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#EEF3F0',
      light: '#F7FAF8',
      dark: '#CBD5D1',
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
      default: '#EEF3F0',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#17201F',
      secondary: '#66736F',
      disabled: '#84918D',
    },
    divider: '#DCE4E0',
    action: {
      hover: '#EEF3F0',
      selected: '#D9F3ED',
      focus: '#E7F7F2',
    },
  },
  shape: {
    borderRadius: 10,
  },
  spacing: 4,
  typography: {
    fontFamily: '"Aptos", "Segoe UI Variable", "Segoe UI", sans-serif',
    fontSize: 17,
    h4: {
      fontWeight: 700,
      fontSize: '2.3125rem',
      lineHeight: 1.25,
      letterSpacing: '-0.02em',
    },
    h5: {
      fontWeight: 700,
      fontSize: '1.8125rem',
      lineHeight: 1.3,
      letterSpacing: '-0.015em',
    },
    h6: {
      fontWeight: 600,
      fontSize: '1.4375rem',
      lineHeight: 1.35,
      letterSpacing: '-0.01em',
    },
    subtitle1: {
      fontWeight: 600,
      fontSize: '1.3125rem',
      letterSpacing: '-0.005em',
    },
    subtitle2: {
      fontWeight: 600,
      fontSize: '1.1875rem',
      letterSpacing: '-0.005em',
    },
    body1: {
      fontSize: '1.3125rem',
      lineHeight: 1.6,
    },
    body2: {
      fontSize: '1.1875rem',
      lineHeight: 1.5,
    },
    caption: {
      fontSize: '1.125rem',
      lineHeight: 1.4,
      color: '#84918D',
    },
    button: {
      fontWeight: 600,
      fontSize: '1.1875rem',
      letterSpacing: '0.01em',
    },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        '.css-1x2jjo': {
          padding: '10px !important',
        },
        body: {
          fontSize: '20px',
          scrollbarWidth: 'thin',
          scrollbarColor: '#CBD5D1 transparent',
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
          padding: '8px 16px',
          minHeight: 42,
          fontSize: '1.1875rem',
          transition: 'background-color 160ms cubic-bezier(0.23, 1, 0.32, 1), transform 120ms cubic-bezier(0.23, 1, 0.32, 1), box-shadow 160ms cubic-bezier(0.23, 1, 0.32, 1)',
          '&:active': {
            transform: 'scale(0.96)',
          },
        },
        contained: {
          boxShadow: '0 1px 3px rgba(15,118,110,0.2)',
          '&:hover': {
            boxShadow: '0 6px 16px rgba(15,118,110,0.3)',

          },
        },
        outlined: {
          '&:hover': {

            boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
          },
        },
        sizeSmall: {
          padding: '6px 14px',
          fontSize: '1.125rem',
        },
        sizeLarge: {
          padding: '12px 28px',
          fontSize: '1.25rem',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: '0 1px 2px rgba(0,0,0,0.04), 0 1px 3px rgba(0,0,0,0.04)',
          transition: 'border-color 180ms cubic-bezier(0.23, 1, 0.32, 1), box-shadow 180ms cubic-bezier(0.23, 1, 0.32, 1), transform 180ms cubic-bezier(0.23, 1, 0.32, 1)',
          '&:hover': {
            boxShadow: '0 4px 14px rgba(23,32,31,0.07)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 12,
        },
        outlined: {
          border: '1px solid #DCE4E0',
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
          borderRadius: 16,
          boxShadow: '0 24px 80px rgba(0,0,0,0.12), 0 8px 24px rgba(0,0,0,0.06)',
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 10,
            transition: 'background-color 160ms cubic-bezier(0.23, 1, 0.32, 1), transform 120ms cubic-bezier(0.23, 1, 0.32, 1), box-shadow 160ms cubic-bezier(0.23, 1, 0.32, 1)',
            backgroundColor: '#FAFAFA',
            '&:hover': {
              backgroundColor: '#FFFFFF',
              '& .MuiOutlinedInput-notchedOutline': {
                borderColor: '#0F766E',
              },
            },
            '&.Mui-focused': {
              backgroundColor: '#FFFFFF',
              '& .MuiOutlinedInput-notchedOutline': {
                borderWidth: '2px',
                borderColor: '#0F766E',
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
            color: '#66736F',
            '&.Mui-focused': {
              color: '#0F766E',
            },
          },
          '& .MuiFormHelperText-root': {
            fontWeight: 500,
            fontSize: '1.0625rem',
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          fontWeight: 600,
          fontSize: '1.0625rem',
          height: 28,
          transition: 'all 150ms ease',
        },
        filled: {
          '&.MuiChip-colorDefault': {
            backgroundColor: '#EEF3F0',
            color: '#42504C',
          },
        },
        sizeSmall: {
          height: 22,
          fontSize: '1rem',
        },
      },
    },
    MuiTableContainer: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          border: '1px solid #EEF3F0',
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            fontWeight: 700,
            fontSize: '1.0625rem',
            textTransform: 'uppercase',
            letterSpacing: '0.06em',
            color: '#66736F',
            backgroundColor: '#F7FAF8',
            borderBottom: '2px solid #DCE4E0',
            padding: '16px 20px',
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
              backgroundColor: '#EEF3F0',
            },
            '&:last-child .MuiTableCell-body': {
              borderBottom: 'none',
            },
          },
          '& .MuiTableCell-body': {
            padding: '16px 20px',
            fontSize: '1.1875rem',
            borderBottomColor: '#EEF3F0',
          },
        },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          fontSize: '1.1875rem',
          minHeight: 48,
          padding: '8px 20px',
          borderRadius: '8px 8px 0 0',
          transition: 'all 150ms ease',
          '&.Mui-selected': {
            color: '#0F766E',
          },
          '&:hover': {
            backgroundColor: 'rgba(15,118,110,0.04)',
          },
        },
      },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: {
          backgroundColor: '#0F766E',
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
            backgroundColor: '#EEF3F0',
          },
          '&.Mui-selected': {
            backgroundColor: '#0F766E',
            color: '#FFFFFF',
            '&:hover': {
              backgroundColor: '#115E59',
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
          fontSize: '1.1875rem',
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
          borderColor: '#DCE4E0',
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
          borderRight: '1px solid #DCE4E0',
        },
      },
    },
    MuiBadge: {
      styleOverrides: {
        badge: {
          fontWeight: 700,
          fontSize: '1rem',
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
          fontSize: '1.0625rem',
          fontWeight: 500,
          backgroundColor: '#17201F',
        },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          borderRadius: 14,
          boxShadow: '0 8px 32px rgba(0,0,0,0.1), 0 2px 8px rgba(0,0,0,0.04)',
          border: '1px solid #DCE4E0',
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
