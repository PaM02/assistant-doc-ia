-- Passages (chunks) des documents et leurs embeddings, pour la recherche par le sens (RAG).
-- Les colonnes id / content / metadata / embedding sont celles attendues par le PgVectorStore de Spring AI.
CREATE TABLE document_chunk (
    id          UUID         PRIMARY KEY,
    content     TEXT         NOT NULL,
    metadata    JSONB        NOT NULL,
    embedding   VECTOR(384)  NOT NULL,   -- 384 = taille des vecteurs du modèle paraphrase-multilingual-MiniLM-L12-v2
    -- Lien vers le document d'origine, calculé à partir des métadonnées :
    -- supprimer un document supprime automatiquement ses passages
    document_id BIGINT GENERATED ALWAYS AS ((metadata ->> 'documentId')::BIGINT) STORED
                REFERENCES document (id) ON DELETE CASCADE
);

-- Index HNSW : recherche rapide des vecteurs les plus proches (distance cosinus)
CREATE INDEX document_chunk_embedding_idx ON document_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX document_chunk_document_id_idx ON document_chunk (document_id);
