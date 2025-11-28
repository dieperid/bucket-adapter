# Étape 1 : Build avec Maven wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copier Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Copier pom.xml pour cache des dépendances
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copier le code source
COPY src ./src

# Build Spring Boot
RUN ./mvnw clean test
RUN ./mvnw clean package -DskipTests

# Étape 2 : Image finale pour dev
FROM eclipse-temurin:25-jdk
WORKDIR /app

# Copier le jar depuis le build
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
