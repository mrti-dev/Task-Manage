import axiosClient from './axiosClient';

export const getDependenciesApi = (taskId) =>
  axiosClient.get(`/api/tasks/${taskId}/dependencies`);

export const addDependencyApi = (taskId, data) =>
  axiosClient.post(`/api/tasks/${taskId}/dependencies`, data);

export const deleteDependencyApi = (taskId, dependencyId) =>
  axiosClient.delete(`/api/tasks/${taskId}/dependencies/${dependencyId}`);
