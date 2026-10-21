package com.matdev.assistantdoc.chat;

public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(Throwable cause) {
        super("Le service d'IA est indisponible pour le moment, veuillez réessayer", cause);
    }
}
