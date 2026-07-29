import axiosClient from './axiosClient';

export const getNotificationsByUserApi = (userId, params) =>
  axiosClient.get(`/api/notifications/user/${userId}`, { params });

export const getUnreadNotificationsApi = (userId, params) =>
  axiosClient.get(`/api/notifications/user/${userId}/unread`, { params });

export const countUnreadNotificationsApi = (userId) =>
  axiosClient.get(`/api/notifications/user/${userId}/unread-count`);

export const markAsReadApi = (userId, id) =>
  axiosClient.put(`/api/notifications/user/${userId}/read/${id}`);

export const markAllAsReadApi = (userId) =>
  axiosClient.put(`/api/notifications/user/${userId}/read-all`);
