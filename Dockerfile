FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
RUN mvn --batch-mode dependency:go-offline
COPY backend/src ./src
RUN mvn --batch-mode test package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S delta && adduser -S delta -G delta
COPY --from=build /workspace/target/delta-esports-2.0.0.jar /app/app.jar
USER delta
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:8080/api/health >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
