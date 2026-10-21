package com.matdev.assistantdoc.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void shortTextGivesSingleChunk() {
        assertThat(chunker.split("  Une seule phrase courte.  ")).containsExactly("Une seule phrase courte.");
    }

    @Test
    void longTextIsSplitIntoOverlappingChunksWithinSizeLimit() {
        String sentence = "Le pilote affiche le code transpondeur 7600 en cas de panne radio. ";
        String text = sentence.repeat(40);

        List<String> chunks = chunker.split(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(TextChunker.CHUNK_SIZE));
        // Chaque passage se termine en fin de phrase
        assertThat(chunks.subList(0, chunks.size() - 1)).allSatisfy(c -> assertThat(c).endsWith("."));
        // Chevauchement : la fin d'un passage est reprise au début du suivant
        String endOfFirst = chunks.get(0).substring(chunks.get(0).length() - 40);
        assertThat(chunks.get(1)).contains(endOfFirst.strip().substring(0, 20));
    }

    @Test
    void blankTextGivesNoChunk() {
        assertThat(chunker.split(" \n\n ")).isEmpty();
    }
}
