# Build stage
FROM maven:3.6.3-openjdk-14-slim AS build
COPY ./ /app/
RUN mvn -f /app/pom.xml clean package

# Run stage
FROM openjdk:14-alpine
COPY --from=build /app/target/nanomaven*.jar /app/nanomaven.jar
ENTRYPOINT [ "java", "-Xmx128M", "-jar", "/app/nanomaven.jar"]