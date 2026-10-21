package com.matdev.assistantdoc.extraction;

public record ExtractionResult(String text, ExtractionMethod method, int pageCount) {
}
