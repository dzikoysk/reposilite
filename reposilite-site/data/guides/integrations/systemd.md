---
id: systemd
title: Systemd
---

You can launch Reposilite as a service using [systemd](https://en.wikipedia.org/wiki/Systemd).
Here is an example configuration of `/etc/systemd/system/reposilite.service` file:

```json5
[Unit]
Description=Reposilite Service

[Service]
# Non-root user
User=reposilite-user
# Reposilite workspace directory
WorkingDirectory=/opt/reposilite
# Path to Reposilite executable/script and its configuration.
ExecStart=java -jar reposilite-3.0.0.jar --config=/etc/reposilite/reposilite.cdn
# Policy
SuccessExitStatus=0
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

~ Associated issue on GitHub: [GH-468 Service file for Linux environments](https://github.com/dzikoysk/reposilite/issues/468)