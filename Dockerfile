# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Run the test suite in CI / locally (./mvnw verify) before building the image.
RUN mvn -B -q clean package -DskipTests

# ---- run ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S dairy && adduser -S dairy -G dairy
COPY --from=build /app/target/*.jar app.jar
USER dairy
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
