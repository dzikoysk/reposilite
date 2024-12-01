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

  # GH-2288: Dockerfile Step-1 migration warning
  # shellcheck disable=SC2012
  existing_uid=$(ls -dln data | awk '{print $3}')
  # shellcheck disable=SC2012
  existing_gid=$(ls -dln data | awk '{print $4}')
  if [ $existing_uid != "977" ]||[ $existing_gid != "977" ]; then
    printf "\033[1;31mStarting with Reposilite 3.5.20 the standard user id will be 977, I will make those changes now.\033[0m\n" 1>&2
    printf "\033[1;31mAfter 3.6.0 docker installations with user id of 999 will no longer work.\033[0m\n" 1>&2
    printf "\033[1;31mFor more information see: https://github.com/dzikoysk/reposilite/issues/2288\033[0m\n" 1>&2
    printf "\033[1;31mIF YOU DOWNGRADE PAST THIS POINT \"Hic sunt dracones\"\033[0m\n" 1>&2
  fi

  # GH-1634: support custom user and group ids
  GROUP_ID="${PGID:-977}"
  if ! grep -q "^reposilite" /etc/group;
  then
    addgroup --gid "$GROUP_ID" reposilite;
  fi
  USER_ID="${PUID:-977}"
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
