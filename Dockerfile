# syntax=docker.io/docker/dockerfile:1.7-labs

# Build stage
FROM bellsoft/liberica-runtime-container:jdk-21-cds-musl AS build
COPY --exclude=entrypoint.sh . /home/reposilite-build
WORKDIR /home/reposilite-build

# Get build dependencies seperately so they can cache
RUN <<EOF
   apk --no-cache add nodejs
EOF

# The below line will show an Error in some IDE's, It is valid Dockerfile.
RUN --mount=type=cache,target=/root/.gradle <<EOF
  export GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork"
  ./gradlew :reposilite-backend:shadowJar --no-daemon --stacktrace
EOF

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
FROM bellsoft/liberica-runtime-container:jre-21-slim-musl AS run

# Run everything at the lowest possible permissions
ARG PGID="977"
ARG PUID="977"

VOLUME /app/data

RUN <<EOF
    mkdir -p /app/data
    mkdir -p /var/log/reposilite
    addgroup -Sg "$PGID" reposilite

    adduser -SH \
    -h /app \
    -s "/usr/sbin/nologin" \
    -G reposilite \
    -u "$PUID" reposilite
EOF

WORKDIR /app
COPY --from=build /home/reposilite-build/reposilite-backend/build/libs/reposilite-3*.jar reposilite.jar
COPY --chmod=755 entrypoint.sh entrypoint.sh
RUN <<EOF
    chown -R "$PUID:$PGID" /app /var/log/reposilite
EOF
USER reposilite

HEALTHCHECK --interval=30s --timeout=30s --start-period=15s \
    --retries=3 CMD [ "sh", "-c", "URL=$(cat /app/data/.local/reposilite.address); echo -n \"curl $URL... \"; \
    (\
        curl -sf $URL > /dev/null\
    ) && echo OK || (\
        echo Fail && exit 2\
    )"]
ENTRYPOINT ["/app/entrypoint.sh"]
CMD []
EXPOSE 8080
