## Build stage: compile and package the Spring Boot app
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copy Maven Wrapper files (ensures exact Maven version match with local dev)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN chmod +x mvnw

# Download dependencies (layered for cache efficiency)
RUN ./mvnw -B -q -DskipTests dependency:go-offline

# Copy source and build the application
COPY src ./src
RUN ./mvnw -B -DskipTests package


## Runtime stage: minimal JRE image
FROM eclipse-temurin:21-jre

# Create a non-root user to run the app
RUN useradd --system --uid 1001 spring && mkdir -p /app && chown -R spring /app
USER spring

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["java", "-jar", "app.jar"]

