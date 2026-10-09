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
    void storeSvgRejectsExternalReferences() {
        MockMultipartFile evil = new MockMultipartFile("svgOverlay", "evil.svg", "image/svg+xml",
                ("<svg xmlns=\"http://www.w3.org/2000/svg\">"
                        + "<image href=\"https://evil.example/pixel.png\"/>"
                        + "<path id=\"path1\" d=\"M0 0\" style=\"fill:url(https://evil.example/x)\"/></svg>")
                        .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> service().storeSvg(evil));
    }

    @Test
    void storeSvgAcceptsCleanContent() throws Exception {
        MockMultipartFile clean = new MockMultipartFile("svgOverlay", "map.svg", "image/svg+xml",
                ("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
                        + "<path id=\"path1\" d=\"M0 0\" fill=\"url(#grad)\"/>"
                        + "<use xlink:href=\"#path1\"/></svg>").getBytes(StandardCharsets.UTF_8));

        BoardAssetStorageService.StoredSvg stored = service().storeSvg(clean);

        assertEquals(1, stored.sectorCount());
        assertTrue(stored.url().startsWith("/boards/"));
    }

    @Test
    void storeImageAcceptsPngAndJpegByMagicBytes() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

        String pngUrl = service().storeImage(
                new MockMultipartFile("mapImage", "map.bin", "application/octet-stream", png));
        String jpegUrl = service().storeImage(
                new MockMultipartFile("mapImage", "map.png", "image/png", jpeg));

        assertTrue(pngUrl.endsWith(".png"), pngUrl);
        assertTrue(jpegUrl.endsWith(".jpg"), jpegUrl);
    }

    @Test
    void storeImageRejectsContentThatIsNotPngOrJpeg() {
        MockMultipartFile disguised = new MockMultipartFile("mapImage", "map.png", "image/png",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                        .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> service().storeImage(disguised));
    }
}
