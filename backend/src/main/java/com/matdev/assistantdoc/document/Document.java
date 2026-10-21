package com.matdev.assistantdoc.document;

import java.time.Instant;

import com.matdev.assistantdoc.extraction.ExtractionMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Document importé. Table créée par la migration Flyway V2__create_document.sql. */
@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int pageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionMethod extractionMethod;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Document() {
        // requis par JPA
    }

    public Document(String fileName, String contentType, long sizeBytes, int pageCount,
                    ExtractionMethod extractionMethod, String content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.pageCount = pageCount;
        this.extractionMethod = extractionMethod;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public int getPageCount() { return pageCount; }
    public ExtractionMethod getExtractionMethod() { return extractionMethod; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
