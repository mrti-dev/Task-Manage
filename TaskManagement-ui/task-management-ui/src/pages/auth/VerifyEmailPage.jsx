import { useState, useEffect, useCallback } from 'react';
import { useLocation, useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  Typography,
  Alert,
  Link,
  CircularProgress,
  Stack,
} from '@mui/material';
import MarkEmailReadRoundedIcon from '@mui/icons-material/MarkEmailReadRounded';
import { useAuth } from '../../contexts/AuthContext';
import { resendVerificationApi } from '../../api/authApi';

const DEFAULT_EXPIRATION_SECONDS = 300; // 5 minutes
const DEFAULT_COOLDOWN_SECONDS = 60; // 60s cooldown

export default function VerifyEmailPage() {
  const { verifyEmail } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);

  const initialEmail = location.state?.email || searchParams.get('email') || '';
  const redirectUrl = location.state?.redirect || searchParams.get('redirect') || '/';

  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  const [expirationSeconds, setExpirationSeconds] = useState(() => {
    const initialExpiresAt = location.state?.expiresAt;
    return initialExpiresAt
      ? Math.max(0, Math.ceil((new Date(initialExpiresAt).getTime() - Date.now()) / 1000))
      : DEFAULT_EXPIRATION_SECONDS;
  });
  const [cooldown, setCooldown] = useState(DEFAULT_COOLDOWN_SECONDS);

  // Expiration countdown
  useEffect(() => {
    if (expirationSeconds <= 0) return;
    const timer = setInterval(() => {
      setExpirationSeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [expirationSeconds]);

  // Cooldown countdown
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    const trimmedEmail = email.trim().toLowerCase();
    const trimmedCode = code.trim();

    if (!trimmedEmail) {
      setError('Vui lòng nhập địa chỉ email');
      return;
    }
    if (!trimmedCode || trimmedCode.length !== 6) {
      setError('Vui lòng nhập mã OTP gồm 6 chữ số');
      return;
    }

    if (expirationSeconds <= 0) {
      setError('Mã xác minh đã hết hạn. Vui lòng gửi lại mã mới.');
      return;
    }

    setLoading(true);
    try {
      const result = await verifyEmail({
        email: trimmedEmail,
        code: trimmedCode,
      });
      if (!result) {
        setError('Mã xác minh không chính xác.');
        return;
      }
      setSuccess('Xác minh email thành công! Đang chuyển hướng...');
      setTimeout(() => {
        navigate(redirectUrl, { replace: true });
      }, 1000);
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Xác minh không thành công. Vui lòng kiểm tra lại mã.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = useCallback(async () => {
    if (cooldown > 0 || resending) return;
    setError('');
    setSuccess('');

    const trimmedEmail = email.trim().toLowerCase();
    if (!trimmedEmail) {
      setError('Vui lòng nhập địa chỉ email để gửi lại mã');
      return;
    }

    setResending(true);
    try {
      await resendVerificationApi({ email: trimmedEmail });
      setSuccess('Mã xác minh mới đã được gửi tới email của bạn!');
      setCode('');
      setExpirationSeconds(DEFAULT_EXPIRATION_SECONDS);
      setCooldown(DEFAULT_COOLDOWN_SECONDS);
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Không thể gửi lại mã xác minh. Vui lòng thử lại sau.';
      setError(message);
    } finally {
      setResending(false);
    }
  }, [cooldown, resending, email]);

  return (
    <Box>
      <Box sx={{ textAlign: 'center', mb: 3 }}>
        <Box
          sx={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            bgcolor: '#F0FDFA',
            color: '#0F766E',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            mb: 2,
          }}
        >
          <MarkEmailReadRoundedIcon sx={{ fontSize: 32 }} />
        </Box>
        <Typography variant="h5" fontWeight={700} sx={{ mb: 0.5, color: '#17201F' }}>
          Xác minh tài khoản
        </Typography>
        <Typography variant="body2" sx={{ color: '#66736F' }}>
          Mã xác minh 6 chữ số đã được gửi đến
        </Typography>
        <Typography variant="body2" fontWeight={600} sx={{ color: '#0F766E', mt: 0.5 }}>
          {email || 'email của bạn'}
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2.5, borderRadius: 2 }}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2.5, borderRadius: 2 }}>
          {success}
        </Alert>
      )}

      <Box component="form" onSubmit={handleVerify}>
        {!initialEmail && (
          <TextField
            fullWidth
            label="Địa chỉ email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            sx={{ mb: 2.5 }}
            size="medium"
          />
        )}

        <TextField
          fullWidth
          label="Mã OTP (6 chữ số)"
          placeholder="••••••"
          value={code}
          onChange={(e) => {
            const val = e.target.value.replace(/\D/g, '').slice(0, 6);
            setCode(val);
          }}
          autoFocus
          size="medium"
          sx={{ mb: 2 }}
          slotProps={{
            htmlInput: {
              maxLength: 6,
              style: {
                textAlign: 'center',
                letterSpacing: '0.4em',
                fontSize: '1.6875rem',
                fontWeight: 700,
              },
            },
          }}
        />

        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
          <Typography variant="caption" sx={{ color: expirationSeconds > 0 ? '#66736F' : '#DC2626' }}>
            {expirationSeconds > 0
              ? `Mã hết hạn trong: ${formatTime(expirationSeconds)}`
              : 'Mã đã hết hạn'}
          </Typography>

          <Button
            size="small"
            onClick={handleResend}
            disabled={cooldown > 0 || resending}
            sx={{
              textTransform: 'none',
              fontSize: '1rem',
              fontWeight: 600,
              color: cooldown > 0 ? '#9CA3AF' : '#0F766E',
            }}
          >
            {resending ? (
              <CircularProgress size={14} sx={{ mr: 1, color: '#0F766E' }} />
            ) : null}
            {cooldown > 0 ? `Gửi lại sau (${cooldown}s)` : 'Gửi lại mã'}
          </Button>
        </Stack>

        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={loading || code.length !== 6 || expirationSeconds <= 0}
          sx={{
            py: 1.5,
            fontSize: '1.25rem',
            background: '#0F766E',
            '&:hover': {
              background: '#115E59',
            },
          }}
        >
          {loading ? (
            <Stack direction="row" spacing={1} alignItems="center">
              <CircularProgress size={20} color="inherit" />
              <span>Đang xác minh...</span>
            </Stack>
          ) : (
            'Xác minh email'
          )}
        </Button>
      </Box>

      <Typography
        variant="body2"
        align="center"
        sx={{ mt: 3, color: '#66736F' }}
      >
        Đã có tài khoản đã xác minh?{' '}
        <Link
          component={RouterLink}
          to={redirectUrl !== '/' ? `/login?redirect=${encodeURIComponent(redirectUrl)}` : '/login'}
          underline="hover"
          sx={{ fontWeight: 600, color: '#0F766E' }}
        >
          Đăng nhập
        </Link>
      </Typography>
    </Box>
  );
}
