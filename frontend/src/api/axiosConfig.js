import axios from 'axios';

// We set up a base axios instance here so we don't have to keep passing the baseURL and headers
// everywhere. We also add a timeout so the frontend doesn't just hang forever if the backend is slow or down.
const API_BASE_URL = process.env.REACT_APP_API_URL;

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Before any request goes out, we check if we've got a JWT sitting in local storage.
// If we do, we slap it onto the Authorization header. This saves us from having to manually
// inject the token into every single API call across the app.
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sms_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// This acts as a global error handler for our API calls, primarily looking out for 401s.
// If the backend kicks us out (usually because the token expired or is invalid), we wipe out
// the stale auth data and bump the user back to the login page so they can re-authenticate.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('sms_token');
      localStorage.removeItem('sms_user');
      localStorage.removeItem('sms_role');
      
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
