#!/bin/bash
set -e

REPOSILITE_ARGS="$REPOSILITE_OPTS"
case "$REPOSILITE_OPTS" in
  *"--working-directory"*) ;;
  *"-wd"*                ) ;;
  *                      ) REPOSILITE_ARGS="--working-directory=/app/data $REPOSILITE_ARGS";;
esac

if [ "$UID" -ne "0" ]; then
  exec java \
       -Dtinylog.writerFile.file="/var/log/reposilite/log_{date}.txt" \
       -Dtinylog.writerFile.latest=/var/log/reposilite/latest.log \
       $JAVA_OPTS \
       -jar reposilite.jar \
       $REPOSILITE_ARGS
else
  GROUP_ID="${PGID:-999}"
  grep "^reposilite" /etc/group > /dev/null
  if [ $? -ne 0 ]
  then
    addgroup --gid "$GROUP_ID" reposilite
  fi

  USER_ID="${PUID:-999}"
  grep "^reposilite" /etc/passwd > /dev/null
  if [ $? -ne 0 ]
  then
    adduser --system -uid "$USER_ID" --ingroup reposilite --shell /bin/sh reposilite
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

