import axiosClient from './axiosClient';

export const getCommentsApi = () => axiosClient.get('/api/comments');
export const getCommentsByTaskApi = (taskId) => axiosClient.get(`/api/comments/task/${taskId}`);

export const getCommentByIdApi = (id) => axiosClient.get(`/api/comments/${id}`);

export const createCommentApi = (data) => axiosClient.post('/api/comments', data);

export const updateCommentApi = (id, data) => axiosClient.put(`/api/comments/${id}`, data);

export const deleteCommentApi = (id) => axiosClient.delete(`/api/comments/${id}`);
