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

# Copie le JAR Spring Boot
COPY --from=backend-build /app-ms/nml-ms/target/*.jar app.jar

# Copie la build Angular
COPY --from=frontend-build /app-ui/dist /app/static

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
