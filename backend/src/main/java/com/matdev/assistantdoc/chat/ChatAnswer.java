package com.matdev.assistantdoc.chat;

import java.util.List;

/** Réponse du chatbot, accompagnée des passages utilisés (numérotés comme dans les citations [1], [2]…). */
public record ChatAnswer(String answer, List<Source> sources) {

    public record Source(int number, long documentId, String fileName, int chunkIndex, double score,
                         String excerpt) {
    }
}
