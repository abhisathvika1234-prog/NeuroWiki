import { apiFetch } from '../api/apiClient';
import { AiQuestionRequest, AiResponse } from '../types/ai';

export const aiService = {
  async askQuestion(data: AiQuestionRequest) {
    return apiFetch<AiResponse>('/api/ai/ask', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async getHistory() {
    return apiFetch<AiResponse[]>('/api/ai/history');
  },
};
