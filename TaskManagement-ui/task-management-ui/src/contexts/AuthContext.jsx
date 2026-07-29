import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginApi, registerApi, logoutApi, refreshTokenApi } from '../api/authApi';
import { getWorkspaceMembersApi } from '../api/workspaceMemberApi';
import { setAccessToken, setOnAuthFailed, getAccessToken } from '../api/axiosClient';

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
    setAccessToken(data.accessToken);
    setAccessTokenState(data.accessToken);
    setUser(data.user);
    return data;
  };

  const logout = async () => {
    try {
      await logoutApi();
    } catch {
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
    } catch {
      setAccessToken(null);
      setAccessTokenState(null);
      setUser(null);
      throw new Error('Refresh failed');
    }
  };

  const isOwner = async (workspaceId) => {
    try {
      const { data: res } = await getWorkspaceMembersApi();
      const members = res?.data ?? res;
      if (!Array.isArray(members)) return false;
      const member = members.find(
        (m) => m.user?.id === user?.id && m.role === 'OWNER'
      );
      return !!member;
    } catch {
      return false;
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
        logout,
        refresh,
        isOwner,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
