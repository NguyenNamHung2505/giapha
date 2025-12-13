# Build stage
FROM maven:3.8-openjdk-11-slim AS build
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built artifact
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown -R spring:spring /app
USER spring

# JVM settings optimized for 4GB server (768MB limit for this container)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
