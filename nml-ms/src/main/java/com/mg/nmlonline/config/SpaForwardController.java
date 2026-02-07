package com.mg.nmlonline.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur pour gérer le routing Angular SPA.
 * Lorsqu'un utilisateur recharge une page comme /carte ou /joueur,
 * Spring Boot doit renvoyer index.html pour que le routeur Angular
 * prenne le relais côté client.
 */
@Controller
public class SpaForwardController {

    /**
     * Forward toutes les routes Angular vers index.html.
     * Exclut les routes API et les fichiers statiques.
     */
    @GetMapping({
            "/carte",
            "/carte/**",
            "/joueur",
            "/joueur/**",
            "/boutique",
            "/boutique/**",
            "/regles",
            "/regles/**",
            "/login",
            "/login/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

