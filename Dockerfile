# ====================================================
# Étape 1 — Angular build
# ====================================================
FROM node:20-alpine AS frontend-build

WORKDIR /app-ui
COPY nml-ui-bst-angular/package*.json ./
RUN npm ci

COPY nml-ui-bst-angular/ .
RUN npm run build -- --configuration production

# ====================================================
# Étape 2 — Backend build (Spring Boot via Maven)
# ====================================================
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app-ms
COPY pom.xml ./
COPY nml-ms/pom.xml ./nml-ms/pom.xml

# Télécharge les dépendances communes
RUN mvn dependency:go-offline -B

# Copie tout le code backend
COPY nml-ms/ ./nml-ms/
WORKDIR /app-ms/nml-ms
RUN mvn clean package -DskipTests

# ====================================================
# Étape 3 — Image finale exécutable
# ====================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Créer un utilisateur non-root pour exécuter l'application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copie le JAR Spring Boot
COPY --from=backend-build /app-ms/nml-ms/target/*.jar app.jar

# Copie la build Angular (Angular 17+ génère dans dist/nom-projet/browser)
COPY --from=frontend-build /app-ui/dist/nml-ui-copilot-angular/browser /app/static

# S'assurer que l'utilisateur a accès en lecture
RUN chown -R appuser:appgroup /app

# Exécuter en tant qu'utilisateur non-root (principe du moindre privilège)
USER appuser

EXPOSE 8080

# Vérification de santé — wget est disponible dans Alpine
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JWT_SECRET and JWT_PEPPER must be provided at runtime:
#   docker run -e JWT_SECRET=<secret> -e JWT_PEPPER=<pepper> ...
ENTRYPOINT ["java", "-jar", "app.jar"]
