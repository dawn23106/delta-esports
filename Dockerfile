FROM node:22-alpine AS mobile-build
WORKDIR /src
COPY frontend-mobile/package*.json ./
RUN npm ci
COPY frontend-mobile/ ./
ENV VITE_BASE_PATH=/app/
RUN npm run build

FROM node:22-alpine AS admin-build
WORKDIR /src
COPY frontend-admin/package*.json ./
RUN npm ci
COPY frontend-admin/ ./
ENV VITE_BASE_PATH=/admin/
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /src
COPY backend/pom.xml ./pom.xml
RUN mvn --batch-mode dependency:go-offline
COPY backend/src ./src
COPY --from=mobile-build /src/dist ./src/main/resources/static/app
COPY --from=admin-build /src/dist ./src/main/resources/static/admin
RUN mvn --batch-mode test package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /src/target/delta-helper-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=demo
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
