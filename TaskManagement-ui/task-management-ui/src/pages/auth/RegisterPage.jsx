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

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const redirectUrl = searchParams.get('redirect') || location.state?.redirect || null;

  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const validate = () => {
    if (!form.username || !form.email || !form.password || !form.confirmPassword) {
      return 'Please fill in all fields';
    }
    if (form.username.length < 3) {
      return 'Username must be at least 3 characters';
    }
    if (form.password.length < 8) {
      return 'Password must be at least 8 characters';
    }
    if (form.password !== form.confirmPassword) {
      return 'Passwords do not match';
    }
    return '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setLoading(true);
    try {
      const res = await register({
        username: form.username.trim(),
        email: form.email.trim().toLowerCase(),
        password: form.password,
      });
      const registeredEmail = res?.email || form.email.trim().toLowerCase();
      const expiresAt = res?.expiresAt || null;
      navigate('/verify-email', {
        replace: true,
        state: {
          email: registeredEmail,
          expiresAt,
          redirect: redirectUrl,
        },
      });
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Registration failed';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5, color: '#17201F' }}>
        Create your account
      </Typography>
      <Typography variant="body2" sx={{ color: '#66736F', mb: 3 }}>
        Start managing your tasks effectively
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2.5, borderRadius: 2 }}>
          {error}
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit}>
        <TextField
          fullWidth
          label="Username"
          name="username"
          value={form.username}
          onChange={handleChange}
          sx={{ mb: 2 }}
          autoFocus
          size="medium"
        />
        <TextField
          fullWidth
          label="Email address"
          name="email"
          type="email"
          value={form.email}
          onChange={handleChange}
          sx={{ mb: 2 }}
          size="medium"
        />
        <TextField
          fullWidth
          label="Password"
          name="password"
          type={showPassword ? 'text' : 'password'}
          value={form.password}
          onChange={handleChange}
          sx={{ mb: 2 }}
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
        <TextField
          fullWidth
          label="Confirm Password"
          name="confirmPassword"
          type="password"
          value={form.confirmPassword}
          onChange={handleChange}
          sx={{ mb: 3 }}
          size="medium"
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
          {loading ? 'Creating account...' : 'Create Account'}
        </Button>
      </Box>

      <Typography
        variant="body2"
        align="center"
        sx={{ mt: 3, color: '#66736F' }}
      >
        Already have an account?{' '}
        <Link
          component={RouterLink}
          to={redirectUrl ? `/login?redirect=${encodeURIComponent(redirectUrl)}` : '/login'}
          underline="hover"
          sx={{ fontWeight: 600, color: '#0F766E' }}
        >
          Sign in
        </Link>
      </Typography>
    </Box>
  );
}