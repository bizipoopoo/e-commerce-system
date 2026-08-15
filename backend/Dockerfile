FROM maven:3.9.15-eclipse-temurin-26-alpine AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
RUN mvn --batch-mode dependency:go-offline
COPY backend/src ./src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S commerce \
    && adduser -S commerce -G commerce
WORKDIR /app
COPY --from=build --chown=commerce:commerce /workspace/target/aurora-commerce-*.jar app.jar
USER commerce
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
