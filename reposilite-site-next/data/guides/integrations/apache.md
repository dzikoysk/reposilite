---
id: apache
title: Apache
---

Apache HTTP is another popular http server with support for reverse proxy.
The most basic configuration should look like this:

```json5
# reposilite is listening on 127.0.0.1:8081
RewriteEngine On

RewriteCond %{HTTP:Upgrade} =websocket [NC]
RewriteRule ^/api/(.*) ws://127.0.0.1:8081/api/$1 [P,L]

ProxyPass / http://127.0.0.1:8081/
ProxyPassReverse / http://127.0.0.1:8081/
```

The following configuration assumes that:
* Reposilite runs on `127.0.0.1:8081`
* Reverse proxy should support websocket connection to CLI
