# Stage 1: Build the Spring Boot application using Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY pom.xml .
# Download dependencies first to cache them
RUN mvn dependency:go-offline -B
# Copy source code
COPY src ./src
# Build package (skipping tests)
RUN mvn clean package -DskipTests

# Stage 2: Create runtime container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the built jar from Stage 1
COPY --from=build /build/target/nmdc-hr-analytics.jar /app/app.jar

# Copy runtime dependencies required by the application
COPY Users.xlsx /app/Users.xlsx
COPY nmdc_login.html /app/nmdc_login.html
COPY frontend /app/frontend
COPY "NMDC LOGO.jpg" "/app/NMDC LOGO.jpg"

EXPOSE 5000

# Run the spring boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
