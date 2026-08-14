export interface KnowledgeDocument {
  id: number;
  title: string;
  type: 'PDF' | 'URL' | 'TEXT' | string;
  content: string;
  sourceUrl?: string;
  status: string;
  fileSize: string;
  addedAt: string;
  conceptsExtractedCount: number;
}
