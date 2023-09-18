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

