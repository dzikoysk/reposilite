# Build-time metadata stage
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Reposilite" \
      org.label-schema.description="Lightweight repository management software dedicated for the Maven artifacts" \
      org.label-schema.url="https://reposilite.com/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/dzikoysk/reposilite" \
      org.label-schema.vendor="dzikoysk" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# Build stage
FROM maven:3.6.3-openjdk-14-slim AS build
COPY ./ /app/
RUN mvn -f /app/pom.xml clean package

# Run stage
FROM openjdk:14-alpine
WORKDIR /app
COPY --from=build /app/target/reposilite*.jar reposilite.jar
ENTRYPOINT [ "java", "-Xmx128M", "-jar", "reposilite.jar"]
