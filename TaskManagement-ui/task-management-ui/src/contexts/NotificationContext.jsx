import { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  countUnreadNotificationsApi,
  markAsReadApi,
  markAllAsReadApi,
} from '../api/notificationApi';
import { useAuth } from './AuthContext';
import { getAccessToken } from '../api/axiosClient';
import { emit } from '../utils/eventBus';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const [unreadCount, setUnreadCount] = useState(0);
  const [toast, setToast] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [wsStatus, setWsStatus] = useState('disconnected');
  const { user } = useAuth();
  const clientRef = useRef(null);

  const refreshUnreadCount = useCallback(async () => {
    if (!user?.id) return;
    try {
      const { data: res } = await countUnreadNotificationsApi(user.id);
      setUnreadCount(res?.data ?? res);
    } catch (err) {
      console.warn('Failed to refresh unread notification count:', err?.message);
    }
  }, [user]);

  const markAsReadFn = useCallback(async (id) => {
    if (!user?.id) return;
    try {
      await markAsReadApi(user.id, id);
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.warn('Failed to mark notification as read:', err?.message);
    }
  }, [user]);

  const markAllAsRead = useCallback(async () => {
    if (!user?.id) return;
    try {
      await markAllAsReadApi(user.id);
      setUnreadCount(0);
    } catch (err) {
      console.warn('Failed to mark all notifications as read:', err?.message);
    }
  }, [user]);

  const handleIncomingNotification = useCallback((notif) => {
    setUnreadCount((prev) => prev + 1);
    setToast(notif);
    setNotifications((prev) => [notif, ...prev]);

    emit('NOTIFICATION_NEW', notif);

    if (notif.type === 'PROJECT_CREATED') {
      emit('PROJECT_CREATED', notif);
    }
    if (notif.type === 'TASK_CREATED' || notif.type === 'TASK_DELETED' || notif.type === 'TASK_UPDATED' || notif.type === 'COMMENT_CREATED') {
      emit(notif.type, notif);
    }
    if (notif.type === 'WORKSPACE_MEMBER_ADDED') {
      emit('workspace:joined', notif.workspaceId);
    }
  }, []);

  const stompSubscribe = useCallback((destination, callback) => {
    const client = clientRef.current;
    if (!client?.active) return null;
    try {
      return client.subscribe(destination, (message) => {
        try {
          callback(JSON.parse(message.body));
        } catch (err) {
          console.warn('Failed to parse websocket message payload:', err?.message);
        }
      });
    } catch (err) {
      console.warn('Failed to subscribe to destination:', destination, err?.message);
      return null;
    }
  }, []);

  const connectWs = useCallback(() => {
    const token = getAccessToken();
    if (!token) return;

    if (clientRef.current?.active) return;

    const wsUrl = import.meta.env.VITE_WS_URL || '/ws';
    setWsStatus('connecting');

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setWsStatus('connected');
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const notif = JSON.parse(message.body);
            handleIncomingNotification(notif);
          } catch (err) {
            console.warn('Failed to parse incoming notification:', err?.message);
          }
        });
      },
      onDisconnect: () => {
        setWsStatus('disconnected');
      },
      onStompError: (frame) => {
        setWsStatus('error');
        console.warn('WebSocket STOMP error:', frame?.headers?.message);
      },
      onWebSocketError: () => {
        setWsStatus('error');
        console.warn('WebSocket transport error');
      },
    });

    client.activate();
    clientRef.current = client;
  }, [handleIncomingNotification]);

  const disconnectWs = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
      setWsStatus('disconnected');
    }
  }, []);

  useEffect(() => {
    if (!user?.id) return;

    // oxlint-disable-next-line react/set-state-in-effect -- đồng bộ dữ liệu và WebSocket theo người dùng
    refreshUnreadCount();
    connectWs();

    return () => {
      disconnectWs();
    };
  }, [user?.id, refreshUnreadCount, connectWs, disconnectWs]);

  return (
    <NotificationContext.Provider
      value={{
        unreadCount,
        setUnreadCount,
        refreshUnreadCount,
        markAsRead: markAsReadFn,
        markAllAsRead,
        toast,
        setToast,
        notifications,
        stompSubscribe,
        wsStatus,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
}

// oxlint-disable-next-line react/only-export-components -- hook dùng chung context
export function useNotification() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotification must be used within a NotificationProvider');
  }
  return context;
}
