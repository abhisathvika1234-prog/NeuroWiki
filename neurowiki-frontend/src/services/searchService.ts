import { apiFetch } from '../api/apiClient';
import { SearchResponse } from '../types/api';

export const searchService = {
  async search(query: string) {
    const q = encodeURIComponent(query);
    return apiFetch<SearchResponse>(`/api/search?q=${q}`);
  },
};
