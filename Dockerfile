# Build stage
FROM openjdk:16-slim AS build
COPY . /home/reposilite-build
WORKDIR /home/reposilite-build/reposilite-backend
RUN export GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork" && chmod +x gradlew && ./gradlew shadowJar --no-daemon --stacktrace

# Build-time metadata stage
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Reposilite" \
      org.label-schema.description="Lightweight repository management software dedicated for the Maven artifacts" \
      org.label-schema.url="https://reposilite.com" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/dzikoysk/reposilite" \
      org.label-schema.vendor="dzikoysk" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# Run stage
FROM openjdk:16-slim
WORKDIR /app
RUN mkdir -p /app/data
VOLUME /app/data
COPY --from=build /home/reposilite-build/reposilite-backend/build/libs/reposilite-3*.jar ./reposilite.jar
ENTRYPOINT exec java $JAVA_OPTS -jar reposilite.jar -wd=/app/data $REPOSILITE_OPTS
