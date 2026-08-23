# ==========================================
# Stage 1: Build & Package
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml and resolve dependencies for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build production JAR (skipping unit/integration tests during image build)
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Production Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create a non-root user and group for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the packaged executable JAR from builder stage
COPY --from=builder --chown=spring:spring /app/target/pockets-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

# Expose default HTTP port (Render will dynamically supply $PORT)
EXPOSE 8080

# Configure JVM flags and launch Spring Boot application
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
