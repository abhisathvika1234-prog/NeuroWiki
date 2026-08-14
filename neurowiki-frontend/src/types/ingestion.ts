export interface IngestionRequest {
  title: string;
  content?: string;
  url?: string;
  sourceType: 'URL' | 'TEXT' | string;
}
