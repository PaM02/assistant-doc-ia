package com.matdev.assistantdoc.document;

import java.io.IOException;
import java.util.List;

import com.matdev.assistantdoc.document.DocumentDtos.DocumentDetail;
import com.matdev.assistantdoc.document.DocumentDtos.DocumentSummary;
import com.matdev.assistantdoc.extraction.ExtractionException;
import com.matdev.assistantdoc.extraction.ExtractionResult;
import com.matdev.assistantdoc.extraction.TextExtractionService;
import com.matdev.assistantdoc.rag.IndexingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final TextExtractionService extractionService;
    private final IndexingService indexingService;

    public DocumentService(DocumentRepository repository, TextExtractionService extractionService,
                           IndexingService indexingService) {
        this.repository = repository;
        this.extractionService = extractionService;
        this.indexingService = indexingService;
    }

    @Transactional
    public DocumentDetail importDocument(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ExtractionException("Le fichier est vide");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ExtractionException("Impossible de lire le fichier reçu", e);
        }

        ExtractionResult result = extractionService.extract(data, file.getContentType());

        Document document = repository.save(new Document(file.getOriginalFilename(), file.getContentType(),
                file.getSize(), result.pageCount(), result.method(), result.text()));
        // Même transaction : si l'indexation échoue, le document n'est pas enregistré non plus
        indexingService.index(document);
        return DocumentDetail.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummary> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(DocumentSummary::from).toList();
    }

    /** Supprime le document ; ses passages sont supprimés en cascade par la base (migration V3). */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new DocumentNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DocumentDetail findById(Long id) {
        return repository.findById(id).map(DocumentDetail::from)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }
}
