import axiosClient from './axiosClient';

export const getDashboardApi = (workspaceId, params) =>
  axiosClient.get(`/api/workspaces/${workspaceId}/dashboard`, { params });
