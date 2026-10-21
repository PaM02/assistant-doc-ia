package com.matdev.assistantdoc.document;

import java.time.Instant;

import com.matdev.assistantdoc.extraction.ExtractionMethod;

/** Objets renvoyés par l'API (on n'expose jamais directement l'entité JPA). */
public final class DocumentDtos {

    private DocumentDtos() {
    }

    /** Résumé utilisé dans la liste : sans le texte complet, qui peut être volumineux. */
    public record DocumentSummary(Long id, String fileName, String contentType, long sizeBytes,
                                  int pageCount, ExtractionMethod extractionMethod,
                                  int characterCount, Instant createdAt) {

        static DocumentSummary from(Document d) {
            return new DocumentSummary(d.getId(), d.getFileName(), d.getContentType(), d.getSizeBytes(),
                    d.getPageCount(), d.getExtractionMethod(), d.getContent().length(), d.getCreatedAt());
        }
    }

    /** Détail d'un document, avec le texte extrait. */
    public record DocumentDetail(Long id, String fileName, String contentType, long sizeBytes,
                                 int pageCount, ExtractionMethod extractionMethod,
                                 Instant createdAt, String content) {

        static DocumentDetail from(Document d) {
            return new DocumentDetail(d.getId(), d.getFileName(), d.getContentType(), d.getSizeBytes(),
                    d.getPageCount(), d.getExtractionMethod(), d.getCreatedAt(), d.getContent());
        }
    }
}
