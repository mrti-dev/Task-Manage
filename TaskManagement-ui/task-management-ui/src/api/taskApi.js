import axiosClient from './axiosClient';

export const getTasksApi = (params) => axiosClient.get('/api/tasks', { params });

export const getTaskByIdApi = (id) => axiosClient.get(`/api/tasks/${id}`);

export const createTaskApi = (data) => axiosClient.post('/api/tasks', data);

export const updateTaskApi = (id, data) => axiosClient.put(`/api/tasks/${id}`, data);

export const deleteTaskApi = (id) => axiosClient.delete(`/api/tasks/${id}`);
