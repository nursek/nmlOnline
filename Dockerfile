FROM node:24-alpine@sha256:ebfe2f90462722a7a4de65e91990e97fe0d401c70e0e762c5b53302f905ec1c1 AS frontend-build

WORKDIR /app-ui

# Install dependencies first for layer caching
COPY nml-ui/package*.json ./
RUN npm ci

COPY nml-ui/ .
RUN npm run build -- --configuration production

FROM maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976 AS backend-build

WORKDIR /app-ms

COPY pom.xml ./
COPY nml-ms/pom.xml ./nml-ms/pom.xml
RUN mvn dependency:go-offline -B

# Copy the backend source and package it (tests are run in CI)
COPY nml-ms/ ./nml-ms/
WORKDIR /app-ms/nml-ms
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61

LABEL org.opencontainers.image.title="NML Online"
LABEL org.opencontainers.image.description="NML Online - Turn-based strategy game"
LABEL org.opencontainers.image.source="https://github.com/nursek/nmlOnline"

# Create a non-root user to run the application
RUN addgroup -S nmlonline && adduser -S nmlonline -G nmlonline

WORKDIR /app

COPY --from=backend-build --chown=nmlonline:nmlonline /app-ms/nml-ms/target/nml-ms-*.jar app.jar

COPY --from=frontend-build --chown=nmlonline:nmlonline /app-ui/dist/nml-ui-copilot-angular/browser /app/static

# Logback écrit /app/logs ; les volumes nommés héritent du propriétaire si le dossier existe dans l'image.
RUN mkdir -p /app/logs /app/static/boards && chown nmlonline:nmlonline /app/logs /app/static/boards

USER nmlonline:nmlonline

# Spring Boot serves static files from /app/static and classpath:/static/
EXPOSE 8080

# JWT_SECRET and JWT_PEPPER must be provided at runtime:
#   docker run -e JWT_SECRET=<secret> -e JWT_PEPPER=<pepper> ...
ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1
