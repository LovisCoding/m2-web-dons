FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cacher des dépendances
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur no root
RUN useradd --system --create-home donner \
    && mkdir -p /app/data \
    && chown -R donner:donner /app
USER donner

COPY --from=build /app/target/*.jar app.jar

VOLUME ["/app/data"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
