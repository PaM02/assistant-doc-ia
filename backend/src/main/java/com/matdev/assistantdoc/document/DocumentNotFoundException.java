package com.matdev.assistantdoc.document;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("Document introuvable : " + id);
    }
}
