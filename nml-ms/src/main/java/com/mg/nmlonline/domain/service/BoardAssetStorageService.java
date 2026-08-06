package com.mg.nmlonline.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stocke sur disque les assets visuels d'un Board uploadés par l'admin (PNG/JPG + SVG overlay).
 * <p>
 * Les fichiers sont écrits sous {@code app.boards.storage-dir} qui doit correspondre à un
 * chemin servi par Spring via {@code spring.web.resources.static-locations} (en prod :
 * {@code /app/static/boards}, volume Docker). L'URL renvoyée est relative (ex. {@code /boards/xyz.png})
 * et directement consommable par le frontend via {@link com.mg.nmlonline.domain.model.board.Board#getMapImageUrl()}
 * et {@link com.mg.nmlonline.domain.model.board.Board#getSvgOverlayUrl()}.
 * <p>
 * ponytail: noms UUID pour éviter toute collision et tout parcours de répertoire ; pas de cleanup
 * des anciens fichiers sur remplacement (l'admin peut le faire à la main si le volume grossit).
 */
@Service
public class BoardAssetStorageService {

    private static final Logger logger = LoggerFactory.getLogger(BoardAssetStorageService.class);

    /** Convention du frontend (carte.component.ts) : id="pathN" où N est le numéro de secteur. */
    private static final Pattern SECTOR_PATH_ID = Pattern.compile("id=\"path(\\d+)\"");

    private final Path storageDir;
    private final String urlPrefix;

    public BoardAssetStorageService(@Value("${app.boards.storage-dir:./target/board-assets}") String storageDir) {
        this.storageDir = Path.of(storageDir);
        // URL servie par static-locations=file:/app/static/ → /boards/...
        this.urlPrefix = "/boards/";
        logger.info("BoardAssetStorageService initialisé sur {}", this.storageDir.toAbsolutePath());
    }

    /** Stocke l'image de fond et renvoie son URL relative. */
    public String storeImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image de la carte absente ou vide.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/png") || contentType.equals("image/jpeg"))) {
            throw new IllegalArgumentException("Format d'image non supporté : " + contentType
                    + " (attendu : image/png ou image/jpeg).");
        }
        String ext = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + ext;
        write(file, filename);
        return urlPrefix + filename;
    }

    /**
     * Stocke le SVG overlay, renvoie son URL relative et le nombre de secteurs
     * détectés (id="pathN"). Ce compte aide l'admin à vérifier la cohérence
     * avec le board.json qu'il uploadera ensuite.
     */
    public StoredSvg storeSvg(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("SVG overlay absent ou vide.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("image/svg+xml") && !contentType.equals("image/svg")) {
            throw new IllegalArgumentException("Format SVG non supporté : " + contentType
                    + " (attendu : image/svg+xml).");
        }

        String content = new String(file.getBytes());
        int sectorCount = countSectorIds(content);

        String filename = UUID.randomUUID() + "-overlay.svg";
        write(file, filename);
        return new StoredSvg(urlPrefix + filename, sectorCount);
    }

    private void write(MultipartFile file, String filename) throws IOException {
        Files.createDirectories(storageDir);
        Path target = storageDir.resolve(filename).normalize();
        if (!target.startsWith(storageDir)) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Asset board écrit : {}", target);
    }

    private int countSectorIds(String svgContent) {
        Matcher matcher = SECTOR_PATH_ID.matcher(svgContent);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** Résultat du stockage du SVG : URL relative + nombre de secteurs détectés. */
    public record StoredSvg(String url, int sectorCount) {}
}