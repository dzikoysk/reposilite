#!/usr/bin/env sh

# shellcheck disable=SC2086
set -e

REPOSILITE_ARGS="$REPOSILITE_OPTS"
case "$REPOSILITE_OPTS" in
  *"--working-directory"*) ;;
  *"-wd"*                ) ;;
  *                      ) REPOSILITE_ARGS="--working-directory=/app/data $REPOSILITE_ARGS";;
esac

# GH-1762: support for running as non-root user

if [ "$(id -u)" != 0 ]; then
  exec java \
       -Dtinylog.writerFile.file="/var/log/reposilite/log_{date}.txt" \
       -Dtinylog.writerFile.latest=/var/log/reposilite/latest.log \
       $JAVA_OPTS \
       -jar reposilite.jar \
       $REPOSILITE_ARGS
# GH-1200: run as non-root user
else
  # GH-1634: support custom user and group ids
  GROUP_ID="${PGID:-999}"
  if ! grep -q "^reposilite" /etc/group;
  then
    addgroup --gid "$GROUP_ID" reposilite;
  fi
  USER_ID="${PUID:-999}"
  if ! grep "^reposilite" /etc/passwd;
  then
    adduser --system -uid "$USER_ID" --ingroup reposilite --shell /bin/sh reposilite;
  fi

  chown -R reposilite:reposilite /app
  chown -R reposilite:reposilite /var/log/reposilite

  exec runuser -u reposilite -- \
    java \
       -Dtinylog.writerFile.file="/var/log/reposilite/log_{date}.txt" \
       -Dtinylog.writerFile.latest=/var/log/reposilite/latest.log \
       $JAVA_OPTS \
       -jar reposilite.jar \
       $REPOSILITE_ARGS
fi
