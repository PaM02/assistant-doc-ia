-- Documents importés et texte extrait (directement ou par OCR)
CREATE TABLE document (
    id                BIGSERIAL    PRIMARY KEY,
    file_name         VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    page_count        INTEGER      NOT NULL,
    extraction_method VARCHAR(20)  NOT NULL,   -- TEXT (lecture directe) ou OCR
    content           TEXT         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
