# syntax=docker/dockerfile:1

# Step 1 : Build Maven
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml
# //TODO NGY write a solution without wrapper
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# //TODO NGY simplify this command (wrapper)
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copy the source code
COPY src ./src

# Step 2 : Validation (run tests)
FROM build AS test
RUN ./mvnw clean test

# Step 3 : Package for production
FROM build AS package
RUN ./mvnw clean package -DskipTests

# Step 4 : Final image (prod) lightweight
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the jar from the build stage
COPY --from=package /app/target/*-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
