# Production Multi-Stage Dockerfile for CivicShield
# Compatible with Docker, Render, Railway, Fly.io, Cloud Run
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/civicshield-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE=demo
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-demo} -jar app.jar"]
