#!/usr/bin/env sh
# shellcheck disable=SC2086

set -e

REPOSILITE_ARGS="$REPOSILITE_OPTS"
case "$REPOSILITE_OPTS" in
  *"--working-directory"*) ;;
  *"-wd"*                ) ;;
  *                      ) REPOSILITE_ARGS="--working-directory=/app/data $REPOSILITE_ARGS";;
esac

if [ "$(id -u)" = 0 ] ; then
  echo "This script should only be run as a non-root user, preferably in a docker container"
  return 1
fi

exec java \
     -Dtinylog.writerFile.file="/var/log/reposilite/log_{date}.txt" \
     -Dtinylog.writerFile.latest=/var/log/reposilite/latest.log \
     $JAVA_OPTS \
     -jar reposilite.jar \
     $REPOSILITE_ARGS