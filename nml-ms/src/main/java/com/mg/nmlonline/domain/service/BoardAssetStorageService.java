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

/** Assets visuels d'un Board sur disque (app.boards.storage-dir, servi via spring.web.resources.static-locations). Renvoie des URLs relatives (/boards/...) consommées par Board.mapImageUrl/svgOverlayUrl. */
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

    /** Stocke le SVG overlay ; renvoie URL relative + nombre de secteurs détectés (id="pathN") pour vérif de cohérence avec le board.json. */
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

    public record StoredSvg(String url, int sectorCount) {}
}