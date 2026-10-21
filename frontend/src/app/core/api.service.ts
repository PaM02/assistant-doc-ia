import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ChatAnswer, DocumentDetail, DocumentSummary, ProblemDetail } from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  listDocuments(): Observable<DocumentSummary[]> {
    return this.http.get<DocumentSummary[]>('/api/documents');
  }

  getDocument(id: number): Observable<DocumentDetail> {
    return this.http.get<DocumentDetail>(`/api/documents/${id}`);
  }

  uploadDocument(file: File): Observable<DocumentDetail> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<DocumentDetail>('/api/documents', form);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`/api/documents/${id}`);
  }

  ask(question: string): Observable<ChatAnswer> {
    return this.http.post<ChatAnswer>('/api/chat', { question });
  }
}

/** Message d'erreur lisible à partir d'une erreur HTTP (ProblemDetail renvoyé par le backend). */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que le backend est démarré.';
    }
    const problem = error.error as ProblemDetail | null;
    return problem?.detail ?? `Erreur ${error.status}`;
  }
  return 'Une erreur inattendue est survenue.';
}
