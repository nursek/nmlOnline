package com.mg.nmlonline.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Assets visuels d'un Board sur disque (app.boards.storage-dir, servi via spring.web.resources.static-locations). Renvoie des URLs relatives (/boards/...) consommées par Board.mapImageUrl/svgOverlayUrl. */
@Service
public class BoardAssetStorageService {

    private static final Logger logger = LoggerFactory.getLogger(BoardAssetStorageService.class);

    /** Convention du frontend (carte.component.ts) : id="pathN" où N est le numéro de secteur. */
    private static final Pattern SECTOR_PATH_ID = Pattern.compile("id=\"path(\\d+)\"");

    /** Refuse le contenu actif : le SVG est réinjecté en innerHTML côté client (bypassSecurityTrustHtml). */
    private static final Pattern ACTIVE_SVG_CONTENT = Pattern.compile(
            "(?i)(<\\s*script|<!\\s*entity|<\\s*foreignObject|<\\s*iframe|<\\s*object|<\\s*embed"
                    + "|javascript\\s*:|(\\s|\"|'|<|/)on[a-z]+\\s*=)");

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

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
        // Le Content-Type client est déclaratif : on décide sur les magic bytes.
        String ext = detectImageExtension(file);
        if (ext == null) {
            throw new IllegalArgumentException("Format d'image non supporté (attendu : PNG ou JPEG).");
        }
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
        if (ACTIVE_SVG_CONTENT.matcher(content).find()) {
            throw new IllegalArgumentException("SVG refusé : contenu actif détecté.");
        }
        int sectorCount = countSectorIds(content);

        String filename = UUID.randomUUID() + "-overlay.svg";
        write(file, filename);
        return new StoredSvg(urlPrefix + filename, sectorCount);
    }

    private static String detectImageExtension(MultipartFile file) throws IOException {
        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(PNG_MAGIC.length);
        }
        if (Arrays.equals(header, PNG_MAGIC)) {
            return ".png";
        }
        if (header.length >= JPEG_MAGIC.length
                && header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2]) {
            return ".jpg";
        }
        return null;
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