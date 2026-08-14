import { apiFetch } from '../api/apiClient';
import { KnowledgeDocument } from '../types/document';

export const documentService = {
  async getAll() {
    return apiFetch<KnowledgeDocument[]>('/api/documents');
  },

  async getById(id: number) {
    return apiFetch<KnowledgeDocument>(`/api/documents/${id}`);
  },

  async delete(id: number) {
    return apiFetch<{ message: string }>(`/api/documents/${id}`, {
      method: 'DELETE',
    });
  },

  async uploadPdf(file: File) {
    const formData = new FormData();
    formData.append('file', file);

    return apiFetch<KnowledgeDocument>('/api/pdf/upload', {
      method: 'POST',
      body: formData,
    });
  },
};
