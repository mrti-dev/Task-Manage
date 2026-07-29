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
  const { user } = useAuth();
  const clientRef = useRef(null);

  const refreshUnreadCount = useCallback(async () => {
    if (!user?.id) return;
    try {
      const { data: res } = await countUnreadNotificationsApi(user.id);
      setUnreadCount(res?.data ?? res);
    } catch {}
  }, [user]);

  const markAsReadFn = useCallback(async (id) => {
    if (!user?.id) return;
    try {
      await markAsReadApi(user.id, id);
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {}
  }, [user]);

  const markAllAsRead = useCallback(async () => {
    if (!user?.id) return;
    try {
      await markAllAsReadApi(user.id);
      setUnreadCount(0);
    } catch {}
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
  }, []);

  const stompSubscribe = useCallback((destination, callback) => {
    const client = clientRef.current;
    if (!client?.active) return null;
    try {
      return client.subscribe(destination, (message) => {
        try { callback(JSON.parse(message.body)); } catch {}
      });
    } catch { return null; }
  }, []);

  const connectWs = useCallback(() => {
    const token = getAccessToken();
    if (!token) return;

    if (clientRef.current?.active) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const notif = JSON.parse(message.body);
            handleIncomingNotification(notif);
          } catch {}
        });
      },
      onStompError: () => {},
    });

    client.activate();
    clientRef.current = client;
  }, [handleIncomingNotification]);

  const disconnectWs = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (!user?.id) return;

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
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotification() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotification must be used within a NotificationProvider');
  }
  return context;
}
