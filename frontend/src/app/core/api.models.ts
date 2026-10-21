/** Types des réponses de l'API Spring Boot (voir DocumentDtos.java et ChatAnswer.java). */

export type ExtractionMethod = 'TEXT' | 'OCR';

export interface DocumentSummary {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  pageCount: number;
  extractionMethod: ExtractionMethod;
  characterCount: number;
  createdAt: string;
}

export interface DocumentDetail extends Omit<DocumentSummary, 'characterCount'> {
  content: string;
}

export interface ChatSource {
  number: number;
  documentId: number;
  fileName: string;
  chunkIndex: number;
  score: number;
  excerpt: string;
}

export interface ChatAnswer {
  answer: string;
  sources: ChatSource[];
}

/** Erreur renvoyée par l'API au format ProblemDetail (RFC 9457). */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
}
