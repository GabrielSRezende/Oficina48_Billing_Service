# ==========================================
# Build
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

# ==========================================
# Runtime
# ==========================================
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && \
    apt-get install -y curl unzip ca-certificates && \
    curl -L https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
    -o newrelic-java.zip && \
    unzip newrelic-java.zip && \
    rm newrelic-java.zip && \
    apt-get clean

COPY newrelic.yml /app/newrelic/newrelic.yml

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "/app/app.jar"]