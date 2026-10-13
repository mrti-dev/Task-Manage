import axiosClient from './axiosClient';

export const getChecklistApi = (taskId) =>
  axiosClient.get(`/api/tasks/${taskId}/checklist`);

export const addChecklistItemApi = (taskId, data) =>
  axiosClient.post(`/api/tasks/${taskId}/checklist`, data);

export const updateChecklistItemApi = (taskId, itemId, data) =>
  axiosClient.put(`/api/tasks/${taskId}/checklist/${itemId}`, data);

export const deleteChecklistItemApi = (taskId, itemId) =>
  axiosClient.delete(`/api/tasks/${taskId}/checklist/${itemId}`);

export const reorderChecklistApi = (taskId, data) =>
  axiosClient.put(`/api/tasks/${taskId}/checklist/reorder`, data);
