package com.matdev.assistantdoc.rag;

import java.util.List;

import com.matdev.assistantdoc.rag.IndexingService.SearchHit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recherche sémantique dans les documents (la partie « Retrieval » du RAG). */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final IndexingService indexingService;

    public SearchController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping
    public List<SearchHit> search(@RequestParam @NotBlank String q,
                                  @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK) {
        return indexingService.search(q, topK);
    }
}
