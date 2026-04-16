FROM maven:3.9.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY apps/api/pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline
COPY apps/api/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/target/campus-platform-api-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
