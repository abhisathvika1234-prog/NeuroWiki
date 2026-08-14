import { apiFetch } from '../api/apiClient';
import { KnowledgePage, KnowledgeRequest } from '../types/knowledge';

export const knowledgeService = {
  async getAll(category?: string, favorite?: boolean) {
    const params = new URLSearchParams();
    if (category) params.append('category', category);
    if (favorite) params.append('favorite', 'true');
    const query = params.toString() ? `?${params.toString()}` : '';

    return apiFetch<KnowledgePage[]>(`/api/knowledge${query}`);
  },

  async getById(id: number) {
    return apiFetch<KnowledgePage>(`/api/knowledge/${id}`);
  },

  async create(data: KnowledgeRequest) {
    return apiFetch<KnowledgePage>('/api/knowledge', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async update(id: number, data: KnowledgeRequest) {
    return apiFetch<KnowledgePage>(`/api/knowledge/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async toggleFavorite(id: number) {
    return apiFetch<KnowledgePage>(`/api/knowledge/${id}/favorite`, {
      method: 'PUT',
    });
  },

  async delete(id: number) {
    return apiFetch<{ message: string }>(`/api/knowledge/${id}`, {
      method: 'DELETE',
    });
  },
};
