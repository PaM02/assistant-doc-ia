package com.matdev.assistantdoc.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.matdev.assistantdoc.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Indexation et recherche des passages dans pgvector.
 *
 * Le VectorStore de Spring AI calcule lui-même les embeddings (avec le modèle local configuré
 * dans application.properties) avant de les enregistrer dans la table document_chunk.
 */
@Service
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    static final String DOCUMENT_ID = "documentId";
    static final String FILE_NAME = "fileName";
    static final String CHUNK_INDEX = "chunkIndex";

    private final VectorStore vectorStore;
    private final TextChunker chunker;

    public IndexingService(VectorStore vectorStore, TextChunker chunker) {
        this.vectorStore = vectorStore;
        this.chunker = chunker;
    }

    /** Découpe le document en passages, calcule leurs embeddings et les stocke. Renvoie le nombre de passages. */
    public int index(Document document) {
        List<String> chunks = chunker.split(document.getContent());
        List<org.springframework.ai.document.Document> passages = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            passages.add(new org.springframework.ai.document.Document(chunks.get(i), Map.of(
                    DOCUMENT_ID, document.getId(),
                    FILE_NAME, document.getFileName(),
                    CHUNK_INDEX, i)));
        }
        vectorStore.add(passages);
        log.info("Document {} ({}) indexé : {} passage(s)", document.getId(), document.getFileName(), passages.size());
        return passages.size();
    }

    /** Recherche par le sens : renvoie les passages les plus proches de la question. */
    public List<SearchHit> search(String query, int topK) {
        return search(query, topK, 0);
    }

    /** Idem, en ignorant les passages dont le score de similarité est inférieur à minScore (0 à 1). */
    public List<SearchHit> search(String query, int topK, double minScore) {
        SearchRequest request = SearchRequest.builder().query(query).topK(topK).similarityThreshold(minScore).build();
        return vectorStore.similaritySearch(request)
                .stream()
                .map(SearchHit::from)
                .toList();
    }

    public record SearchHit(long documentId, String fileName, int chunkIndex, double score, String content) {

        static SearchHit from(org.springframework.ai.document.Document passage) {
            Map<String, Object> metadata = passage.getMetadata();
            return new SearchHit(
                    ((Number) metadata.get(DOCUMENT_ID)).longValue(),
                    (String) metadata.get(FILE_NAME),
                    ((Number) metadata.get(CHUNK_INDEX)).intValue(),
                    passage.getScore() == null ? 0 : passage.getScore(),
                    passage.getText());
        }
    }
}
