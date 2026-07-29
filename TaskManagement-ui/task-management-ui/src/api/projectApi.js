import axiosClient from './axiosClient';

export const getProjectsApi = (params) => axiosClient.get('/api/projects', { params });

export const getProjectByIdApi = (id) => axiosClient.get(`/api/projects/${id}`);

export const createProjectApi = (data) => axiosClient.post('/api/projects', data);

export const updateProjectApi = (id, data) => axiosClient.put(`/api/projects/${id}`, data);

export const deleteProjectApi = (id) => axiosClient.delete(`/api/projects/${id}`);
