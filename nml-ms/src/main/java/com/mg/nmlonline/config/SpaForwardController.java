package com.mg.nmlonline.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Recharge d'une route Angular (ex. /carte) : forward vers index.html pour le routeur client. */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/accueil",
            "/accueil/**",
            "/parties",
            "/parties/**",
            "/carte",
            "/carte/**",
            "/joueur",
            "/joueur/**",
            "/boutique",
            "/boutique/**",
            "/ordres",
            "/ordres/**",
            "/rapports",
            "/rapports/**",
            "/regles",
            "/regles/**",
            "/login",
            "/login/**",
            "/register",
            "/register/**",
            "/admin",
            "/admin/**",
            "/not-found",
            "/not-found/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

