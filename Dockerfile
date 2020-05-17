FROM openjdk:14-alpine

COPY target/nanomaven-*.jar /app/nanomaven.jar

CMD ["java", "-Xmx128M", "-jar", "/app/nanomaven.jar"]