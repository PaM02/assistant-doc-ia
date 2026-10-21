package com.matdev.assistantdoc.extraction;

/** Manière dont le texte d'un document a été obtenu. */
public enum ExtractionMethod {
    /** Texte lu directement dans le PDF (PDF « numérique »). */
    TEXT,
    /** Texte reconnu sur une image ou un PDF scanné. */
    OCR
}
