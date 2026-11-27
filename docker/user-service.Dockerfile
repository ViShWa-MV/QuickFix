# Render image for user-service.
# Build context: repository root.
# Render settings: Root Directory blank, Dockerfile Path docker/user-service.Dockerfile.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x mvnw \
 && ./mvnw -B -q -DskipTests -f user-service/pom.xml package \
 && cp user-service/target/*.jar /app.jar

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 quickfix
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
USER quickfix
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
