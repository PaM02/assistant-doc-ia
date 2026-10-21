package com.matdev.assistantdoc.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChatServiceTest {

    private static ChatAnswer.Source source(int number) {
        return new ChatAnswer.Source(number, 1, "doc.pdf", number - 1, 0.5, "extrait " + number);
    }

    @Test
    void keepsOnlySourcesCitedInTheAnswer() {
        List<ChatAnswer.Source> sources = List.of(source(1), source(2), source(3));

        List<ChatAnswer.Source> kept = ChatService.keepCitedSources("Réponse [1] et aussi [3][3].", sources);

        assertThat(kept).extracting(ChatAnswer.Source::number).containsExactly(1, 3);
    }

    @Test
    void fullWidthCitationsAreNormalized() {
        assertThat(ChatService.normalizeCitations("Code **7600**【1】 et 【 2 】.")).isEqualTo("Code **7600**[1] et [2].");
    }

    @Test
    void noCitationMeansNoSource() {
        assertThat(ChatService.keepCitedSources(ChatService.NO_INFORMATION, List.of(source(1)))).isEmpty();
    }
}
