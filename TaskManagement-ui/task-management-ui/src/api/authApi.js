import axiosClient from './axiosClient';

export const loginApi = (data) => axiosClient.post('/api/auth/login', data);

export const registerApi = (data) => axiosClient.post('/api/auth/register', data);

export const verifyEmailApi = (data) => axiosClient.post('/api/auth/verify-email', data);

export const resendVerificationApi = (data) => axiosClient.post('/api/auth/resend-verification', data);

export const refreshTokenApi = () => axiosClient.post('/api/auth/refresh');

export const logoutApi = () => axiosClient.post('/api/auth/logout');

