FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY services/api/pom.xml .
COPY services/api/src src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 travelmap
WORKDIR /app
COPY --from=build /workspace/target/travelmap-api-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
