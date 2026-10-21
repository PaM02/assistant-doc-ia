package com.matdev.assistantdoc.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.matdev.assistantdoc.rag.IndexingService;
import com.matdev.assistantdoc.rag.IndexingService.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Chatbot RAG :
 * 1. Retrieval   : recherche des passages les plus proches de la question (pgvector) ;
 * 2. Augmented   : ajout de ces passages, numérotés, dans le message envoyé au modèle ;
 * 3. Generation  : le modèle rédige la réponse à partir de ces seuls passages, en citant ses sources.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    static final String NO_INFORMATION = "Je ne trouve pas cette information dans les documents importés.";
    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)]");

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant documentaire. Tu réponds aux questions en t'appuyant UNIQUEMENT sur les extraits \
            de documents fournis, numérotés [1], [2], etc.
            Règles :
            - Réponds dans la langue de la question, de façon claire et concise.
            - Cite les extraits utilisés avec leur numéro entre crochets, par exemple [1] ou [2][3].
            - Si les extraits ne contiennent pas la réponse, réponds exactement : "%s"
            - N'invente rien et n'utilise pas tes connaissances générales.
            """.formatted(NO_INFORMATION);

    private final IndexingService indexingService;
    private final ChatClient chatClient;
    private final int topK;
    private final double minScore;

    public ChatService(IndexingService indexingService, ChatClient.Builder chatClientBuilder,
                       @Value("${app.rag.top-k}") int topK, @Value("${app.rag.min-score}") double minScore) {
        this.indexingService = indexingService;
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.topK = topK;
        this.minScore = minScore;
    }

    public ChatAnswer ask(String question) {
        // 1. Retrieval
        List<SearchHit> hits = indexingService.search(question, topK, minScore);
        if (hits.isEmpty()) {
            // Aucun passage pertinent : inutile d'appeler le modèle (et d'utiliser le quota)
            return new ChatAnswer(NO_INFORMATION, List.of());
        }

        // 2. Augmented : les passages sont ajoutés au message, avec un numéro pour les citations
        List<ChatAnswer.Source> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            SearchHit hit = hits.get(i);
            int number = i + 1;
            sources.add(new ChatAnswer.Source(number, hit.documentId(), hit.fileName(), hit.chunkIndex(),
                    hit.score(), hit.content()));
            context.append("[%d] (document : %s)%n%s%n%n".formatted(number, hit.fileName(), hit.content()));
        }
        String userMessage = "Extraits de documents :\n\n" + context + "Question : " + question;

        // 3. Generation
        String answer;
        try {
            answer = chatClient.prompt().user(userMessage).call().content();
        } catch (RuntimeException e) {
            log.error("Échec de l'appel au modèle d'IA", e);
            throw new AiUnavailableException(e);
        }
        if (answer == null || answer.isBlank()) {
            return new ChatAnswer(NO_INFORMATION, List.of());
        }
        String normalized = normalizeCitations(answer.strip());
        return new ChatAnswer(normalized, keepCitedSources(normalized, sources));
    }

    /** Certains modèles écrivent les citations 【1】 au lieu de [1] : on uniformise. */
    static String normalizeCitations(String answer) {
        return answer.replaceAll("【\\s*(\\d+)\\s*】", "[$1]");
    }

    /** Ne garde que les passages réellement cités dans la réponse ([1], [2]…). */
    static List<ChatAnswer.Source> keepCitedSources(String answer, List<ChatAnswer.Source> sources) {
        Set<Integer> cited = CITATION.matcher(answer).results()
                .map(m -> Integer.parseInt(m.group(1)))
                .collect(Collectors.toSet());
        return sources.stream().filter(s -> cited.contains(s.number())).toList();
    }
}
