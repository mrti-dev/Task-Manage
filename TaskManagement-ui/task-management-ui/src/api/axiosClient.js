import axios from 'axios';

const BASE_URL = '';

let accessToken = null;
let onAuthFailed = null;
let onTokenRefreshed = null;

export const setAccessToken = (token) => {
  accessToken = token;
};

export const getAccessToken = () => accessToken;

export const setOnAuthFailed = (callback) => {
  onAuthFailed = callback;
};

export const setOnTokenRefreshed = (callback) => {
  onTokenRefreshed = callback;
};

let isRefreshing = false;
let refreshSubscribers = [];

function onRefreshed(newToken) {
  refreshSubscribers.forEach(({ resolve, originalRequest }) => {
    originalRequest.headers.Authorization = `Bearer ${newToken}`;
    resolve(axiosClient(originalRequest));
  });
  refreshSubscribers = [];
}

function onRefreshFailed(error) {
  refreshSubscribers.forEach(({ reject }) => {
    reject(error);
  });
  refreshSubscribers = [];
}

function addRefreshSubscriber(resolve, reject, originalRequest) {
  refreshSubscribers.push({ resolve, reject, originalRequest });
}

const axiosClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

axiosClient.interceptors.request.use(
  (config) => {
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/api/auth/refresh')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          addRefreshSubscriber(resolve, reject, originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post(
          `${BASE_URL}/api/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const newToken = data.accessToken;
        setAccessToken(newToken);
        onRefreshed(newToken);
        if (onTokenRefreshed) onTokenRefreshed();
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return axiosClient(originalRequest);
      } catch (refreshError) {
        setAccessToken(null);
        onRefreshFailed(refreshError);
        if (onAuthFailed) onAuthFailed();
        return Promise.reject(refreshError || error);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
