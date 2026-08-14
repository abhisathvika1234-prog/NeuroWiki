import { KnowledgePage } from './knowledge';
import { KnowledgeDocument } from './document';

export interface DashboardStats {
  knowledgeCount: number;
  documentCount: number;
  favoritesCount: number;
  aiQuestionsCount: number;
  recentKnowledge: KnowledgePage[];
  recentDocuments: KnowledgeDocument[];
}
