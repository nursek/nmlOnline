package com.mg.nmlonline.config;

import com.mg.nmlonline.EmbeddedPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedPostgresTest
@AutoConfigureMockMvc
class StaticResourceCacheConfigTest {

    @DynamicPropertySource
    static void staticLocations(DynamicPropertyRegistry registry) throws Exception {
        Path dir = Files.createTempDirectory("nml-static");
        Files.createDirectories(dir.resolve("boards"));
        Files.createDirectories(dir.resolve("assets"));
        Files.writeString(dir.resolve("boards/ok.png"), "x");
        Files.writeString(dir.resolve("assets/ok.webp"), "x");
        String base = dir.toUri().toString();
        registry.add("spring.web.resources.static-locations", () -> base.endsWith("/") ? base : base + "/");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sertLesUploadsImmuablesEtRevalideLesDerives() throws Exception {
        mockMvc.perform(get("/boards/ok.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("immutable")));
        mockMvc.perform(get("/assets/ok.webp"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void neMarquePasLes404CommeImmuables() throws Exception {
        mockMvc.perform(get("/boards/missing.png"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", not(containsString("immutable"))));
    }
}
