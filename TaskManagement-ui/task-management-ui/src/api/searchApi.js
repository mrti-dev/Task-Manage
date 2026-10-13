import axiosClient from './axiosClient';

export const searchApi = (params) =>
  axiosClient.get('/api/search', { params });
