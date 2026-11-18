FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache des dépendances Maven
COPY pom.xml .
RUN mvn -B dependency:resolve

# Compilation et packaging de l'application
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root et création des répertoires de données
RUN useradd --system --create-home donner \
    && mkdir -p /app/data/uploads \
    && chown -R donner:donner /app

# Copie du jar avec les bons droits d'accès
COPY --from=build --chown=donner:donner /app/target/donner-*.jar /app/app.jar

USER donner

VOLUME ["/app/data"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
