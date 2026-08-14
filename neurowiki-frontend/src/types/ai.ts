export interface RagSource {
  type: string; // KNOWLEDGE, PDF, URL, TEXT
  title: string;
  id: number;
}

export interface AiQuestionRequest {
  question: string;
}

export interface AiResponse {
  id?: number;
  question: string;
  answer: string;
  sources?: RagSource[];
  timestamp: string;
  serviceConfigured: boolean;
}
