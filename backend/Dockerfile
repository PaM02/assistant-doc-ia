# ---------- Étape 1 : compilation (image avec JDK + Maven) ----------
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /app

# Les dépendances Maven changent rarement : on les télécharge dans une couche à part (mise en cache)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Les tests sont exécutés par le pipeline CI, pas pendant la construction de l'image
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# ---------- Étape 2 : exécution (image légère avec seulement le JRE) ----------
# Ubuntu 26.04 : Tesseract 5.5 et Leptonica 1.86, compatibles avec Tess4J 5.20
# (la 24.04 fournit Leptonica 1.82, trop ancienne : fonctions manquantes)
FROM eclipse-temurin:21-jre-resolute

# Tesseract (OCR) et ses langues, installés depuis Ubuntu : sous Linux, Tess4J utilise la bibliothèque du système
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-fra tesseract-ocr-eng curl \
    && rm -rf /var/lib/apt/lists/* \
    # Tess4J cherche « libtesseract.so » et « libleptonica.so » : Ubuntu ne fournit que les noms versionnés
    && cd /usr/lib/x86_64-linux-gnu \
    && ln -sf "$(ls libtesseract.so.* | head -n 1)" libtesseract.so \
    && ln -sf "$(ls libleptonica.so.* liblept.so.* 2>/dev/null | head -n 1)" libleptonica.so \
    && ls -l libtesseract.so libleptonica.so

# L'application ne tourne pas en root
RUN useradd --system --create-home --uid 1001 app
USER app
WORKDIR /home/app
# Dossier du cache du modèle d'embedding : créé ici pour que le volume Docker appartienne à « app »
RUN mkdir -p /home/app/.cache

COPY --from=build --chown=app:app /app/app.jar app.jar

ENV OCR_DATA_PATH=/usr/share/tesseract-ocr/5/tessdata \
    SPRING_DOCKER_COMPOSE_ENABLED=false \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=5 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
