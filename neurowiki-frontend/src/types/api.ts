import { KnowledgePage } from './knowledge';
import { KnowledgeDocument } from './document';

export interface ApiResponse<T> {
  data?: T;
  message?: string;
  errors?: Record<string, string>;
  status: number;
}

export interface SearchResponse {
  knowledge: KnowledgePage[];
  documents: KnowledgeDocument[];
}
