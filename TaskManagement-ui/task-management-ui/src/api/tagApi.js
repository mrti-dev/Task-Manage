import axiosClient from './axiosClient';

export const getWorkspaceTagsApi = (workspaceId) =>
  axiosClient.get(`/api/workspaces/${workspaceId}/tags`);

export const createTagApi = (workspaceId, data) =>
  axiosClient.post(`/api/workspaces/${workspaceId}/tags`, data);

export const updateTagApi = (tagId, data) =>
  axiosClient.put(`/api/tags/${tagId}`, data);

export const deleteTagApi = (tagId) =>
  axiosClient.delete(`/api/tags/${tagId}`);

export const attachTagApi = (taskId, tagId) =>
  axiosClient.post(`/api/tasks/${taskId}/tags/${tagId}`);

export const detachTagApi = (taskId, tagId) =>
  axiosClient.delete(`/api/tasks/${taskId}/tags/${tagId}`);
