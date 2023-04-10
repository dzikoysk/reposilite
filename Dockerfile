# Build stage
FROM openjdk:19-slim AS build
COPY . /home/reposilite-build
WORKDIR /home/reposilite-build
RUN apt-get update; apt-get install -y curl \
    && curl -sL https://deb.nodesource.com/setup_14.x | bash - \
    && apt-get install -y nodejs \
    && curl -L https://www.npmjs.com/install.sh | sh
RUN \
  export GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork" && \
  chmod +x gradlew && \
  bash gradlew :reposilite-backend:shadowJar --no-daemon --stacktrace

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
FROM openjdk:19-slim
RUN mkdir -p /app/data && mkdir -p /var/log/reposilite
VOLUME /app/data
WORKDIR /app
COPY --from=build /home/reposilite-build/reposilite-backend/build/libs/reposilite-3*.jar reposilite.jar
COPY --from=build /home/reposilite-build/entrypoint.sh entrypoint.sh
RUN apt-get update && apt-get -y install util-linux curl
HEALTHCHECK --interval=30s --timeout=30s --start-period=15s \
    --retries=3 CMD [ "sh", "-c", "URL=$(cat /app/data/.local/reposilite.address); echo -n \"curl $URL... \"; \
    (\
        curl -sf $URL > /dev/null\
    ) && echo OK || (\
        echo Fail && exit 2\
    )"]
ENTRYPOINT ["/bin/sh", "entrypoint.sh"]
CMD []
