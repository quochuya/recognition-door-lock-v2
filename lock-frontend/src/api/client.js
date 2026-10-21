import axios from "axios";

// Backend Spring Boot chạy local ở cổng 8080 (đổi nếu bạn cấu hình khác)
export const API_BASE = "http://localhost:8080";

const api = axios.create({ baseURL: API_BASE });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Token hết hạn / bị từ chối -> tự đá về trang login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && (err.response.status === 401 || err.response.status === 403)) {
      if (err.config.url !== "/api/auth/login") {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        window.location.href = "/login";
      }
    }
    return Promise.reject(err);
  }
);

export default api;
