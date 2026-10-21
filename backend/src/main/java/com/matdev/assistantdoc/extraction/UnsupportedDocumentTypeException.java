package com.matdev.assistantdoc.extraction;

public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException(String contentType) {
        super("Type de fichier non supporté : " + contentType
                + " (formats acceptés : PDF, PNG, JPEG, TIFF)");
    }
}
