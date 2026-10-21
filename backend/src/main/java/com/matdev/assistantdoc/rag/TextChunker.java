package com.matdev.assistantdoc.rag;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Découpe un texte en passages (chunks) qui se chevauchent légèrement.
 *
 * Pourquoi découper ? Le modèle d'embedding ne lit qu'un texte court à la fois (~128 à 256 tokens),
 * et un petit passage précis donne une recherche plus pertinente qu'un document entier.
 * Pourquoi un chevauchement ? Pour ne pas perdre une idée coupée à la frontière entre deux passages.
 */
@Component
public class TextChunker {

    static final int CHUNK_SIZE = 500;
    static final int OVERLAP = 100;

    public List<String> split(String text) {
        String normalized = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n").strip();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            if (end < normalized.length()) {
                end = findCutPoint(normalized, start, end);
            }
            String chunk = normalized.substring(start, end).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = nextStart(normalized, Math.max(end - OVERLAP, start + 1));
        }
        return chunks;
    }

    /** Coupe de préférence en fin de paragraphe, puis en fin de phrase, puis entre deux mots. */
    private static int findCutPoint(String text, int start, int end) {
        int minEnd = start + CHUNK_SIZE / 2;
        for (String separator : new String[] {"\n\n", ". ", "\n", " "}) {
            int index = text.lastIndexOf(separator, end - separator.length());
            if (index >= minEnd) {
                return index + separator.length();
            }
        }
        return end;
    }

    /** Fait commencer le passage suivant au début d'un mot. */
    private static int nextStart(String text, int from) {
        int space = text.indexOf(' ', from);
        return (space == -1 || space - from > 30) ? from : space + 1;
    }
}
