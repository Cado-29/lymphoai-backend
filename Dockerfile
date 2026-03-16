# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy source and build jar
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built Spring Boot jar
COPY --from=build /app/target/*.jar app.jar

# App port
EXPOSE 8080

# Start app
ENTRYPOINT ["java", "-jar", "app.jar"]