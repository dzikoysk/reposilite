# Build stage
FROM openjdk:18-slim AS build
COPY . /home/reposilite-build
WORKDIR /home/reposilite-build
RUN \
  export GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork" && \
  chmod +x gradlew && \
  ./gradlew :reposilite-backend:shadowJar --no-daemon --stacktrace --exclude-task :reposilite-backend:integrationTest

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
FROM openjdk:18-slim
RUN mkdir -p /app/data && mkdir -p /var/log/reposilite
VOLUME /app/data
WORKDIR /app
COPY --from=build /home/reposilite-build/reposilite-backend/build/libs/reposilite-3*.jar reposilite.jar
COPY --from=build /home/reposilite-build/entrypoint.sh entrypoint.sh
RUN apt-get update && \
    apt-get -y install util-linux && \
    addgroup --gid 999 reposilite && \
    adduser --system -uid 999 --ingroup reposilite --shell /bin/sh reposilite
ENTRYPOINT ["/bin/sh", "entrypoint.sh"]
CMD []