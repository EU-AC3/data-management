# Use Alpine-based JRE for minimal image size (~150MB vs ~400MB with JDK)
FROM eclipse-temurin:11-jre-alpine

# Add metadata
LABEL maintainer="sparkworks"
LABEL description="AC3 Data Management Mapper Service"

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR file (use COPY instead of ADD for simple file copy)
ARG JAR_FILE=target/mapper-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose ports
EXPOSE 5026 8026

# Use shell form to allow environment variable expansion
# Add JVM optimization flags for containerized environments
ENTRYPOINT ["java", \
    "-Dspring.profiles.active=docr", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
