package com.mg.nmlonline.demo;

import lombok.extern.slf4j.Slf4j;

/**
 * DEPRECATED: Cette classe de démo ne fonctionne plus en mode standalone.
 * Les équipements sont maintenant chargés depuis la base de données (data.sql).
 * Utilisez l'application Spring Boot pour tester les fonctionnalités.
 */
@Slf4j
@Deprecated
public class PlayerDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CLASSE PLAYER ===\n");
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ATTENTION: Cette démo ne fonctionne plus en mode standalone ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Les équipements sont maintenant chargés depuis la BDD.      ║");
        System.out.println("║  Utilisez l'application Spring Boot pour tester.             ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Pour lancer l'application:                                  ║");
        System.out.println("║    mvn spring-boot:run                                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
}
