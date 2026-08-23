---
id: systemd
title: Systemd
---

You can launch Reposilite as a service using [systemd](https://en.wikipedia.org/wiki/Systemd).
Here is an example configuration of `/etc/systemd/system/reposilite.service` file:
It assumes that the `reposilite-user` user and group exist and that `/opt/reposilite/reposilite.jar` is owned by root.

```json5
[Unit]
Description=Reposilite Service

[Service]
# Dedicated service identity that owns Reposilite's writable files.
User=reposilite-user
Group=reposilite-user
# systemd creates /var/lib/reposilite with the service identity and these permissions.
StateDirectory=reposilite
StateDirectoryMode=0750
# Reposilite stores repositories, configuration, its database, plugins, and other mutable data here.
WorkingDirectory=/var/lib/reposilite
# The root-owned application JAR is loaded from /opt, separately from mutable application data.
ExecStart=java -jar /opt/reposilite/reposilite.jar --local-configuration=/var/lib/reposilite/configuration.cdn --working-directory=/var/lib/reposilite
# OpenJDK reports a handled SIGTERM as exit code 143, which represents a clean shutdown here.
SuccessExitStatus=143
# Reposilite gets time to stop its HTTP server, plugins, and database before systemd sends SIGKILL.
TimeoutStopSec=60
# Unexpected failures trigger a restart after a short delay; clean shutdowns do not.
Restart=on-failure
RestartSec=5
# New files mask group write access and all access for other users.
UMask=0027
# The service receives isolated /tmp and /var/tmp directories.
PrivateTmp=true
# Reposilite and its child processes cannot gain additional privileges.
NoNewPrivileges=true
# Only API pseudo-devices such as /dev/null and /dev/random are exposed to the service.
PrivateDevices=true

[Install]
WantedBy=multi-user.target
```

~ Associated issue on GitHub: [GH-468 Service file for Linux environments](https://github.com/dzikoysk/reposilite/issues/468)
