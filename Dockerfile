# --- Etape 1 : build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache des dependances
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Build (les tests tournent dans la CI ; on les saute ici pour un build d'image rapide)
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Etape 2 : runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# Utilisateur non-root
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/banking-api-*.jar app.jar
USER appuser
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
