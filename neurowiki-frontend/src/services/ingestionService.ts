import { apiFetch } from '../api/apiClient';
import { IngestionRequest } from '../types/ingestion';
import { KnowledgeDocument } from '../types/document';

export const ingestionService = {
  async ingest(data: IngestionRequest) {
    return apiFetch<KnowledgeDocument>('/api/ingestion', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};
