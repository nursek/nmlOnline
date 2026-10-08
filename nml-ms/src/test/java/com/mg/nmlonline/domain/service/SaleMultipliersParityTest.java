package com.mg.nmlonline.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@DisplayName("Parité des multiplicateurs de vente front/back")
class SaleMultipliersParityTest {

    private static final Pattern TS_ARRAY = Pattern.compile("SALE_MULTIPLIERS\\s*=\\s*\\[([^\\]]+)]");

    @Test
    void frontendTableMatchesBackend() throws IOException {
        Path tsFile = Path.of("..", "nml-ui", "src", "app", "core", "sale-multiplier.ts");
        String source = Files.readString(tsFile);
        Matcher matcher = TS_ARRAY.matcher(source);
        if (!matcher.find()) {
            throw new IllegalStateException("SALE_MULTIPLIERS introuvable dans " + tsFile.toAbsolutePath());
        }
        double[] frontend = Arrays.stream(matcher.group(1).split(","))
                .map(String::trim)
                .mapToDouble(Double::parseDouble)
                .toArray();
        assertArrayEquals(ResourceService.SALE_MULTIPLIERS, frontend,
                "La table front (sale-multiplier.ts) a divergé de ResourceService.SALE_MULTIPLIERS");
    }
}
