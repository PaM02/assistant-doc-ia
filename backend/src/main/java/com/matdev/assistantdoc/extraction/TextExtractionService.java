package com.matdev.assistantdoc.extraction;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Extrait le texte d'un document :
 * - PDF numérique : lecture directe du texte (PDFBox) ;
 * - PDF scanné ou image : OCR (Tesseract).
 */
@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

    public static final String PDF = "application/pdf";
    public static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/tiff");

    /** En dessous de ce nombre moyen de caractères par page, on considère le PDF comme scanné. */
    private static final int MIN_CHARS_PER_PAGE = 50;
    /** Résolution du rendu des pages avant OCR (300 DPI est la valeur conseillée pour Tesseract). */
    private static final int OCR_DPI = 300;

    private final OcrService ocrService;

    public TextExtractionService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public ExtractionResult extract(byte[] data, String contentType) {
        if (PDF.equals(contentType)) {
            return extractFromPdf(data);
        }
        if (IMAGE_TYPES.contains(contentType)) {
            return extractFromImage(data);
        }
        throw new UnsupportedDocumentTypeException(contentType);
    }

    private ExtractionResult extractFromPdf(byte[] data) {
        try (PDDocument pdf = Loader.loadPDF(data)) {
            int pages = pdf.getNumberOfPages();
            String text = new PDFTextStripper().getText(pdf);

            if (countVisibleChars(text) >= (long) MIN_CHARS_PER_PAGE * pages) {
                return new ExtractionResult(text.strip(), ExtractionMethod.TEXT, pages);
            }

            // Presque pas de texte lisible : c'est un scan, on passe chaque page à l'OCR
            log.info("PDF sans couche texte ({} page(s)) : passage à l'OCR", pages);
            PDFRenderer renderer = new PDFRenderer(pdf);
            StringBuilder ocrText = new StringBuilder();
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, OCR_DPI, ImageType.GRAY);
                ocrText.append(ocrService.recognize(image)).append("\n\n");
            }
            return result(ocrText.toString(), ExtractionMethod.OCR, pages);
        } catch (IOException e) {
            throw new ExtractionException("PDF illisible ou corrompu", e);
        }
    }

    private ExtractionResult extractFromImage(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new ExtractionException("Image illisible ou format non reconnu");
            }
            return result(ocrService.recognize(image), ExtractionMethod.OCR, 1);
        } catch (IOException e) {
            throw new ExtractionException("Image illisible", e);
        }
    }

    private ExtractionResult result(String text, ExtractionMethod method, int pages) {
        String cleaned = text.strip();
        if (cleaned.isEmpty()) {
            throw new ExtractionException("Aucun texte n'a pu être extrait du document");
        }
        return new ExtractionResult(cleaned, method, pages);
    }

    private static long countVisibleChars(String text) {
        return text.chars().filter(c -> !Character.isWhitespace(c)).count();
    }
}
