# ======================
# Frontend build (Angular)
# ======================
FROM node:20-alpine AS frontend-build

WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .
RUN npm run build -- --configuration production

# ======================
# Backend build (Spring)
# ======================
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline

COPY backend/ .
RUN mvn clean package -DskipTests

# ======================
# Runtime image
# ======================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Backend
COPY --from=backend-build /app/target/*.jar app.jar

# Frontend (servi par Spring Boot via static/)
COPY --from=frontend-build /app/dist /app/static

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
