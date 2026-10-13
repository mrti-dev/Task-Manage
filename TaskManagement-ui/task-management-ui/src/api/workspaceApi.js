import axiosClient from './axiosClient';

export const getWorkspacesApi = () => axiosClient.get('/api/workspaces');

export const getWorkspaceByIdApi = (id) => axiosClient.get(`/api/workspaces/${id}`);

export const createWorkspaceApi = (data) => axiosClient.post('/api/workspaces', data);

export const updateWorkspaceApi = (id, data) => axiosClient.put(`/api/workspaces/${id}`, data);

export const deleteWorkspaceApi = (id) => axiosClient.delete(`/api/workspaces/${id}`);

export const transferWorkspaceOwnershipApi = (id, data) =>
  axiosClient.post(`/api/workspaces/${id}/transfer-owner`, data);

