import { useState } from 'react';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  Typography,
  Link,
  Alert,
  InputAdornment,
  IconButton,
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const from = location.state?.from;
  const redirectUrl = searchParams.get('redirect') || location.state?.redirect ||
    (from ? `${from.pathname || ''}${from.search || ''}${from.hash || ''}` : '/') || '/';

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [isUnverified, setIsUnverified] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsUnverified(false);
    if (!form.email || !form.password) {
      setError('Please fill in all fields');
      return;
    }
    setLoading(true);
    try {
      await login({
        email: form.email.trim().toLowerCase(),
        password: form.password,
      });
      navigate(redirectUrl, { replace: true });
    } catch (err) {
      const errorCode = err.response?.data?.errorCode;
      if (errorCode === 'EMAIL_NOT_VERIFIED') {
        setIsUnverified(true);
        setError('Tài khoản chưa được xác minh email. Vui lòng xác minh mã OTP để đăng nhập.');
      } else {
        const message =
          err.response?.data?.message ||
          err.response?.data?.error ||
          'Invalid email or password';
        setError(message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5, color: '#17201F' }}>
        Welcome back
      </Typography>
      <Typography variant="body2" sx={{ color: '#66736F', mb: 3 }}>
        Sign in to your account to continue
      </Typography>

      {error && (
        <Alert
          severity="error"
          sx={{ mb: 2.5, borderRadius: 2 }}
          action={
            isUnverified ? (
              <Button
                color="inherit"
                size="small"
                onClick={() =>
                  navigate('/verify-email', {
                    state: {
                      email: form.email.trim().toLowerCase(),
                      redirect: redirectUrl !== '/' ? redirectUrl : null,
                    },
                  })
                }
                sx={{ fontWeight: 700 }}
              >
                Xác minh ngay
              </Button>
            ) : null
          }
        >
          {error}
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit}>
        <TextField
          fullWidth
          label="Email address"
          name="email"
          type="email"
          value={form.email}
          onChange={handleChange}
          sx={{ mb: 2 }}
          autoFocus
          size="medium"
        />
        <TextField
          fullWidth
          label="Password"
          name="password"
          type={showPassword ? 'text' : 'password'}
          value={form.password}
          onChange={handleChange}
          sx={{ mb: 3 }}
          size="medium"
          slotProps={{
            input: {
              endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={() => setShowPassword(!showPassword)}
                  edge="end"
                  size="small"
                >
                  {showPassword ? <VisibilityOff /> : <Visibility />}
                </IconButton>
              </InputAdornment>
              ),
            },
          }}
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={loading}
          sx={{
            py: 1.5,
            fontSize: '1.25rem',
            background: '#0F766E',
            '&:hover': {
              background: '#115E59',
            },
          }}
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </Button>
      </Box>

      <Typography
        variant="body2"
        align="center"
        sx={{ mt: 3, color: '#66736F' }}
      >
        Don&apos;t have an account?{' '}
        <Link
          component={RouterLink}
          to={redirectUrl !== '/' ? `/register?redirect=${encodeURIComponent(redirectUrl)}` : '/register'}
          underline="hover"
          sx={{ fontWeight: 600, color: '#0F766E' }}
        >
          Create one
        </Link>
      </Typography>
    </Box>
  );
}
