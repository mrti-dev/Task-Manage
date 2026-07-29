import axiosClient from './axiosClient';

export const getWorkspaceMembersApi = () => axiosClient.get('/api/workspace-members');

export const getWorkspaceMemberByIdApi = (id) => axiosClient.get(`/api/workspace-members/${id}`);

export const createWorkspaceMemberApi = (data) => axiosClient.post('/api/workspace-members', data);

export const updateWorkspaceMemberApi = (id, data) => axiosClient.put(`/api/workspace-members/${id}`, data);

export const deleteWorkspaceMemberApi = (id) => axiosClient.delete(`/api/workspace-members/${id}`);
