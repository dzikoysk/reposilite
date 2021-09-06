# Build stage

FROM openjdk:16 AS build
COPY . /home/reposilite-build
WORKDIR /home/reposilite-build
#ENV GRADLE_OPTS = "--add-opens java.base/java.util=ALL-UNNAMED \
#                   --add-opens java.base/java.lang=ALL-UNNAMED \
#                   --add-opens java.base/java.lang.invoke=ALL-UNNAMED \
#                   --add-opens java.base/java.util=ALL-UNNAMED \
#                   --add-opens java.prefs/java.util.prefs=ALL-UNNAMED \
#                   --add-opens java.prefs/java.util.prefs=ALL-UNNAMED \
#                   --add-opens java.base/java.nio.charset=ALL-UNNAMED \
#                   --add-opens java.base/java.net=ALL-UNNAMED \
#                   --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED \
#                   -XX:MaxMetaspaceSize=256m \
#                   -XX:+HeapDumpOnOutOfMemoryError \
#                   -Xms256m \
#                   -Xmx512m \
#                   -Dfile.encoding=UTF-8 \
#                   -Duser.country \
#                   -Duser.language=en \
#                   -Duser.variant"
RUN chmod +x gradlew && ./gradlew \
                    --add-opens java.base/java.util=ALL-UNNAMED \
                    --add-opens java.base/java.lang=ALL-UNNAMED \
                    --add-opens java.base/java.lang.invoke=ALL-UNNAMED \
                    --add-opens java.base/java.util=ALL-UNNAMED \
                    --add-opens java.prefs/java.util.prefs=ALL-UNNAMED \
                    --add-opens java.prefs/java.util.prefs=ALL-UNNAMED \
                    --add-opens java.base/java.nio.charset=ALL-UNNAMED \
                    --add-opens java.base/java.net=ALL-UNNAMED \
                    --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED \
                    -XX:MaxMetaspaceSize=256m \
                    -XX:+HeapDumpOnOutOfMemoryError \
                    -Xms256m \
                    -Xmx512m \
                    -Dfile.encoding=UTF-8 \
                    -Duser.country \
                    -Duser.language=en \
                    -Duser.variant \
                    shadowJar --no-daemon --stacktrace --debug

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

FROM openjdk:16-slim
WORKDIR /app
RUN mkdir -p /app/data
VOLUME /app/data
COPY --from=build /home/reposilite-build/reposilite-backend/build/libs/*.jar ./reposilite.jar
ENTRYPOINT exec java $JAVA_OPTS -jar reposilite.jar -wd=/app/data $REPOSILITE_OPTS
