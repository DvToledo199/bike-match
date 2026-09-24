# Two stages: the first builds the jar, the second only runs it. The final image carries the
# Java runtime and the jar, not Maven, the sources or the build tools.

# --- Build: the project's own Maven wrapper packages the Spring Boot jar ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
# The tests need a PostgreSQL database and already run in CI on every pull request.
RUN ./mvnw -q package -DskipTests

# --- Run ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 bikematch
COPY --from=build /app/target/*.jar app.jar
USER bikematch

# Inside a container the API must listen on every interface, not only on 127.0.0.1.
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8080
# Render gives the port in PORT. The free plan has 512 MB: cap the heap and use the lightest GC.
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -XX:+UseSerialGC -jar app.jar --server.port=${PORT:-8080}"]
