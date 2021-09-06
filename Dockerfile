# Build stage

FROM adoptopenjdk/openjdk16:jdk16u-ubuntu-nightly AS build
COPY . /home/reposilite-build
WORKDIR /home/reposilite-build
RUN su - && chmod +x gradlew && ./gradlew shadowJar --no-daemon --stacktrace --debug

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

# Run stage

FROM adoptopenjdk/openjdk16:jdk16u-ubuntu-nightly
WORKDIR /app
RUN mkdir -p /app/data
VOLUME /app/data
COPY --from=build /home/reposilite-build/reposilite-backend/build/libs/*.jar ./reposilite.jar
ENTRYPOINT exec java $JAVA_OPTS -jar reposilite.jar -wd=/app/data $REPOSILITE_OPTS
