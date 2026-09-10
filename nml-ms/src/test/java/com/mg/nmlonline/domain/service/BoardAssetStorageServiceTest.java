package com.mg.nmlonline.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardAssetStorageServiceTest {

    @TempDir
    Path tempDir;

    private BoardAssetStorageService service() {
        return new BoardAssetStorageService(tempDir.toString());
    }

    @Test
    void storeSvgRejectsActiveContent() {
        MockMultipartFile evil = new MockMultipartFile("svgOverlay", "evil.svg", "image/svg+xml",
                ("<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\">"
                        + "<path id=\"path1\" d=\"M0 0\"/></svg>").getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> service().storeSvg(evil));
    }

    @Test
    void storeSvgAcceptsCleanContent() throws Exception {
        MockMultipartFile clean = new MockMultipartFile("svgOverlay", "map.svg", "image/svg+xml",
                ("<svg xmlns=\"http://www.w3.org/2000/svg\">"
                        + "<path id=\"path1\" d=\"M0 0\"/></svg>").getBytes(StandardCharsets.UTF_8));

        BoardAssetStorageService.StoredSvg stored = service().storeSvg(clean);

        assertEquals(1, stored.sectorCount());
        assertTrue(stored.url().startsWith("/boards/"));
    }
}
