# ====================================================
# Stage 1 — Angular frontend build
# ====================================================
FROM node:24-alpine AS frontend-build

WORKDIR /app-ui

# Install dependencies first for layer caching
COPY nml-ui-bst-angular/package*.json ./
RUN npm ci

# Copy the rest of the frontend source and build for production
COPY nml-ui-bst-angular/ .
RUN npm run build -- --configuration production

# ====================================================
# Stage 2 — Spring Boot backend build
# ====================================================
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app-ms

# Copy parent POM and download common dependencies
COPY pom.xml ./
COPY nml-ms/pom.xml ./nml-ms/pom.xml
RUN mvn dependency:go-offline -B

# Copy the backend source and package it (tests are run in CI)
COPY nml-ms/ ./nml-ms/
WORKDIR /app-ms/nml-ms
RUN mvn clean package -DskipTests

# ====================================================
# Stage 3 — Final runtime image
# ====================================================
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL org.opencontainers.image.title="NML Online"
LABEL org.opencontainers.image.description="NML Online - Turn-based strategy game"
LABEL org.opencontainers.image.source="https://github.com/nursek/nmlOnline"

# Install curl for the HEALTHCHECK
RUN apk add --no-cache curl

# Create a non-root user to run the application
RUN addgroup -S nmlonline && adduser -S nmlonline -G nmlonline

WORKDIR /app

# Copy the Spring Boot executable JAR
COPY --from=backend-build --chown=nmlonline:nmlonline /app-ms/nml-ms/target/nml-ms-*.jar app.jar

# Copy the compiled Angular static assets
COPY --from=frontend-build --chown=nmlonline:nmlonline /app-ui/dist/nml-ui-copilot-angular/browser /app/static

USER nmlonline:nmlonline

# Spring Boot serves static files from /app/static and classpath:/static/
EXPOSE 8080

# JWT_SECRET and JWT_PEPPER must be provided at runtime:
#   docker run -e JWT_SECRET=<secret> -e JWT_PEPPER=<pepper> ...
ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
