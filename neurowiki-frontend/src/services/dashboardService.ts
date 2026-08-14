import { apiFetch } from '../api/apiClient';
import { DashboardStats } from '../types/dashboard';

export const dashboardService = {
  async getStats() {
    return apiFetch<DashboardStats>('/api/dashboard/stats');
  },
};
