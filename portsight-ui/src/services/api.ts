import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:8080/api/v1' : '/api/v1'),
});

// ─── JWT interceptor ──────────────────────────────────────────────────────────
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Auth ─────────────────────────────────────────────────────────────────────
export const authService = {
  login: (email: string, password: string) =>
    api.post<{ access_token: string; refresh_token: string; token_type: string }>(
      '/auth/login', { email, password }
    ),
  register: (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }) => api.post('/auth/register', data),

  logout: () => api.post('/auth/logout'),
};

// ─── Market ───────────────────────────────────────────────────────────────────
export const marketService = {
  advanceDay: () => api.post('/market/advance'),
  getMarketData: () => api.get('/market/assets'),
};

// ─── Portfolio ────────────────────────────────────────────────────────────────
export const portfolioService = {
  getAll: () => api.get('/portfolios'),
  getById: (id: string) => api.get(`/portfolios/${id}`),
  create: (data: {
    portfolioName: string;
    riskProfile: string;
    benchmark: string;
  }) => api.post('/portfolios', data),
  update: (id: string, data: any) => api.put(`/portfolios/${id}`, data),
  archive: (id: string) => api.delete(`/portfolios/${id}`),
};

// ─── Holdings ─────────────────────────────────────────────────────────────────
export const holdingsService = {
  getByPortfolio: (portfolioId: string) =>
    api.get(`/portfolios/${portfolioId}/holdings`),
  add: (portfolioId: string, data: {
    assetId: string;
    quantity: number;
    averagePrice: number;
  }) => api.post(`/portfolios/${portfolioId}/holdings`, data),
};

// ─── Transactions ─────────────────────────────────────────────────────────────
export const transactionService = {
  getAll: () => api.get('/transactions'),
  getByPortfolio: (portfolioId: string) =>
    api.get(`/transactions?portfolioId=${portfolioId}`),
  create: (data: {
    portfolioId: string;
    assetId: string;
    type: string;
    quantity: number;
    price: number;
  }) => api.post('/transactions', data),
  reverse: (data: { originalTransactionId: string; reason: string }) =>
    api.post('/transactions/reversal', data),
};

// ─── Analytics ────────────────────────────────────────────────────────────────
export const analyticsService = {
  getPortfolioAnalytics: (portfolioId: string) =>
    api.get(`/analytics/portfolio/${portfolioId}`),
  getPerformance: (portfolioId: string) =>
    api.get(`/analytics/performance/${portfolioId}`),
  getGrowthHistory: (portfolioId: string) =>
    api.get(`/analytics/snapshots/${portfolioId}`),
};

// ─── Risk ─────────────────────────────────────────────────────────────────────
export const riskService = {
  getRiskMetrics: (portfolioId: string) =>
    api.get(`/risk/${portfolioId}`),
};

// ─── Stress Test ──────────────────────────────────────────────────────────────
export const stressTestService = {
  run: (portfolioId: string, scenario: string) =>
    api.post('/stress-test', { portfolioId, scenario }),
};

// ─── Assets ───────────────────────────────────────────────────────────────────
export const assetService = {
  getAll: () => api.get('/assets'),
  search: (params: { ticker?: string; sector?: string; assetType?: string }) =>
    api.get('/assets', { params }),
};

// ─── Reports ──────────────────────────────────────────────────────────────────
export const reportService = {
  generate: (portfolioId: string, reportType: string) =>
    api.post('/reports/generate', { portfolioId, reportType }),
  list: (portfolioId: string) =>
    api.get(`/reports?portfolioId=${portfolioId}`),
  download: (reportId: string) =>
    api.get(`/reports/${reportId}`, { responseType: 'arraybuffer' }),
  delete: (reportId: string) =>
    api.delete(`/reports/${reportId}`),
};

export default api;