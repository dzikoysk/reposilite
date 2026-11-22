# syntax=docker.io/docker/dockerfile:1

# Build stage
FROM eclipse-temurin:21-jdk-noble@sha256:a09e64f4976df9c4f2029c6abe64fc26f069a342ec9a7f147dce7b1933ebda16 AS build
RUN apt-get --error-on=any -o Acquire::Retries=0 update \
    && apt-get -o Acquire::Retries=0 install -y --no-install-recommends libatomic1 \
    && rm -rf /var/lib/apt/lists/*
COPY --exclude=entrypoint.sh . /home/reposilite-build
WORKDIR /home/reposilite-build

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
FROM eclipse-temurin:21-jre-noble@sha256:ac595159aa2209f07a2331efa8a8ce4278b77a7b76a9365a0f173786d5067a9e AS run

# Disable download retries while updating the APT client affected by CVE-2026-109518.
RUN apt-get --error-on=any -o Acquire::Retries=0 update \
    && apt-get -o Acquire::Retries=0 install -y --no-install-recommends --only-upgrade apt libapt-pkg6.0t64 libpcre2-8-0 \
    && rm -rf /var/lib/apt/lists/*

# Setup runtime environment
RUN mkdir -p /app/data && mkdir -p /var/log/reposilite
VOLUME /app/data
RUN <<EOF
    mkdir -p /app/data
    mkdir -p /var/log/reposilite
EOF
WORKDIR /app

# Import application code
COPY --chmod=755 entrypoint.sh entrypoint.sh
COPY --from=build /home/reposilite-build/reposilite-backend/build/libs/reposilite-3*.jar reposilite.jar

HEALTHCHECK --interval=30s --timeout=30s --start-period=15s \
    --retries=3 CMD [ "sh", "-c", "URL=$(cat /app/data/.local/reposilite.address); echo -n \"curl $URL... \"; \
    (\
        curl -sf $URL > /dev/null\
    ) && echo OK || (\
        echo Fail && exit 2\
    )"]
ENTRYPOINT ["/app/entrypoint.sh"]
EXPOSE 8080