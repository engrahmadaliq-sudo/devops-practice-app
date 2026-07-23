# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies separately for faster rebuilds
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Run stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user (good DevOps/security practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/devops-practice-app.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s \
  CMD wget -qO- http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
