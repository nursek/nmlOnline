package com.mg.nmlonline.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Recharge d'une route Angular (ex. /carte) : forward vers index.html pour le routeur client. */
@Controller
public class SpaForwardController {

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
            "/login/**",
            "/admin",
            "/admin/**",
            "/not-found",
            "/not-found/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

