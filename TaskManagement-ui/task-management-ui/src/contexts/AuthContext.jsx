import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginApi, registerApi, logoutApi, refreshTokenApi, verifyEmailApi } from '../api/authApi';
import { setAccessToken, setOnAuthFailed } from '../api/axiosClient';

function decodeJwtPayload(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [accessToken, setAccessTokenState] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const handleAuthFailed = useCallback(() => {
    setUser(null);
    setAccessTokenState(null);
    setLoading(false);
    navigate('/login', { replace: true });
  }, [navigate]);

  useEffect(() => {
    setOnAuthFailed(handleAuthFailed);
  }, [handleAuthFailed]);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const { data } = await refreshTokenApi();
        const token = data.accessToken;
        setAccessToken(token);
        setAccessTokenState(token);
        setAccessToken(token);

        const payload = decodeJwtPayload(token);
        if (payload) {
          setUser({
            id: parseInt(payload.sub, 10),
            role: payload.role,
            email: payload.email,
          });
        }
      } catch {
        setUser(null);
        setAccessTokenState(null);
      } finally {
        setLoading(false);
      }
    };
    initAuth();
  }, []);

  const login = async (credentials) => {
    const { data } = await loginApi(credentials);
    setAccessToken(data.accessToken);
    setAccessTokenState(data.accessToken);
    setUser(data.user);
    return data;
  };

  const register = async (userData) => {
    const { data } = await registerApi(userData);
    return data;
  };

  const verifyEmail = async (verificationData) => {
    const { data } = await verifyEmailApi(verificationData);
    setAccessToken(data.accessToken);
    setAccessTokenState(data.accessToken);
    setUser(data.user);
    return data;
  };

  const logout = async () => {
    try {
      await logoutApi();
    } catch (err) {
      console.warn('Logout API error:', err?.message);
    } finally {
      setAccessToken(null);
      setAccessTokenState(null);
      setUser(null);
    }
  };

  const refresh = async () => {
    try {
      const { data } = await refreshTokenApi();
      const token = data.accessToken;
      setAccessToken(token);
      setAccessTokenState(token);
      return token;
    } catch (err) {
      setAccessToken(null);
      setAccessTokenState(null);
      setUser(null);
      throw new Error(err?.response?.data?.message || 'Refresh failed');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken,
        loading,
        login,
        register,
        verifyEmail,
        logout,
        refresh,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// oxlint-disable-next-line react/only-export-components -- hook dùng chung context
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
