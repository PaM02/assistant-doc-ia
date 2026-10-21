package com.matdev.assistantdoc.security;

public enum Role {
    /** Peut importer et supprimer des documents. */
    ADMIN,
    /** Peut consulter les documents et utiliser l'assistant. */
    USER
}
