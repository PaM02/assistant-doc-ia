package com.matdev.assistantdoc.extraction;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Reconnaissance optique de caractères (OCR) avec Tesseract. */
@Service
public class OcrService {

    private final String dataPath;
    private final String language;

    public OcrService(@Value("${app.ocr.data-path}") String dataPath,
                      @Value("${app.ocr.language}") String language) {
        // La bibliothèque native Tesseract a besoin d'un chemin absolu
        Path path = Path.of(dataPath).toAbsolutePath().normalize();
        // Sans ses fichiers de langue, Tesseract plante au lieu de renvoyer une erreur :
        // on vérifie donc leur présence dès le démarrage de l'application
        for (String lang : language.split("\\+")) {
            if (!Files.isRegularFile(path.resolve(lang + ".traineddata"))) {
                throw new IllegalStateException("Fichier de langue OCR manquant : " + path.resolve(lang + ".traineddata")
                        + " (lancer scripts/download-tessdata.ps1)");
            }
        }
        this.dataPath = path.toString();
        this.language = language;
    }

    public String recognize(BufferedImage image) {
        // Une instance Tesseract n'est pas thread-safe : on en crée une par appel
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(language);
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new ExtractionException("Échec de l'OCR : " + e.getMessage(), e);
        }
    }
}
