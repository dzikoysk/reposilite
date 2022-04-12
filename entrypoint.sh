#!/bin/bash

chown -R reposilite:reposilite /app
chown -R reposilite:reposilite /var/log/reposilite

# shellcheck disable=SC2093
# shellcheck disable=SC2086
exec runuser -u reposilite -- \
  java \
     -Dtinylog.writerFile.file="/var/log/reposilite/log_{date}.txt" \
     -Dtinylog.writerFile.latest=/var/log/reposilite/latest.log \
     $JAVA_OPTS \
     -jar reposilite.jar \
     --working-directory=/app/data \
     $REPOSILITE_OPTS