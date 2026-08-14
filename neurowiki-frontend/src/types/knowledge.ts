export interface KnowledgePage {
  id: number;
  title: string;
  content: string;
  category: string;
  tags: string;
  favorite: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeRequest {
  title: string;
  content: string;
  category?: string;
  tags?: string;
  favorite?: boolean;
}
