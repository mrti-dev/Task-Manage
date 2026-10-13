import axiosClient from './axiosClient';

export const getSavedFiltersApi = (params) =>
  axiosClient.get('/api/saved-filters', { params });

export const createSavedFilterApi = (data) =>
  axiosClient.post('/api/saved-filters', data);

export const updateSavedFilterApi = (id, data) =>
  axiosClient.put(`/api/saved-filters/${id}`, data);

export const deleteSavedFilterApi = (id) =>
  axiosClient.delete(`/api/saved-filters/${id}`);
