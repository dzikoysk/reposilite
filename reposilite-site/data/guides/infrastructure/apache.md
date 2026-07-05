---
id: apache
title: Apache
---

Apache HTTP is another popular http server with support for reverse proxy.
Make sure you enabled the proxy modules.
```bash
sudo a2enmod proxy proxy_http
```

The most basic configuration should look like this:

```json5
# reposilite is listening on 127.0.0.1:8081
ProxyPass / http://127.0.0.1:8081/
ProxyPassReverse / http://127.0.0.1:8081/
```

This configuration assumes that:
* Reposilite runs on `127.0.0.1:8081`

To secure the port and IP on which Reposilite runs (e.g. to `127.0.0.1` so that it cannot be accessed from outside Apache), change the local configuration (by default `configuration.cdn`):
```yaml
# Hostname
# It is recommended to use 127.0.0.1 so that it cannot be accessed outside of the server or Apache.
hostname: 127.0.0.1
# Custom port instead of 8080, also needs to be changed in Apache configuration
port: 8081
```

### Custom base path

If you're running reposilite under a custom base path (e.g. `repo.example.com/reposilite/`), use the following:

```apacheconf
<VirtualHost *:80>
    # reposilite is listening on 127.0.0.1:8081
    ProxyPass /reposilite/ http://127.0.0.1:8081/
    ProxyPassReverse /reposilite/ http://127.0.0.1:8081/
</VirtualHost>
```

And update the base path property in the local configuration (by default `configuration.cdn`):

```yaml
# Custom base path
basePath: /reposilite/
```

### SSL

It's recommended to configure SSL via apache instead of the local configuration (by default `configuration.cdn`).

#### Step 1 - Install certbot

To install certbot, take a look at the [official instructions](https://certbot.eff.org/instructions).

#### Step 2 - Configure Apache2
Make sure you enabled the ssl and proxy modules.
enable it with:
```bash
sudo a2enmod ssl proxy proxy_http
```

And use a similar configuration like this:

```apacheconf
<IfModule mod_ssl.c>
<VirtualHost *:443>
    # Your Domain, needed for SSL
    ServerName repo.example.com

    # probably configured by Certbot or whatever you're using.
    Include /etc/letsencrypt/options-ssl-apache.conf
    # Use HTTP/2, not required but improves performance
    Protocols h2 http/1.1

    ProxyPass / http://127.0.0.1:8081/
    ProxyPassReverse / http://127.0.0.1:8081/

    # probably configured by Certbot or whatever you're using.
    SSLCertificateFile /etc/letsencrypt/live/repo.example.com/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/repo.example.com/privkey.pem
</VirtualHost>
</IfModule>
```

#### Generate certificates

To generate a certificate with certbot for apache2 and the domain `repo.example.com`, run:
```bash
sudo certbot --apache -d repo.example.com
```

Then restart apache.

```bash
sudo systemctl restart apache2
```

### Web console

The web console streams logs from `/api/console/log` using [Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events) (a long-lived `GET`).
Older versions used a WebSocket, so the previous `RewriteCond %{HTTP:Upgrade}` rules are no longer needed — just make sure the stream is not compressed:

```apacheconf
<Location /api/console/log>
    SetEnv no-gzip 1
</Location>
```

If you require a client certificate, set `SSLVerifyClient require` at the server/virtual-host level, not per `<Location>`.
Browsers won't present a certificate during the TLS renegotiation a per-location rule triggers on the console's background request, which fails it with `Re-negotiation handshake failed` ([#2417](https://github.com/dzikoysk/reposilite/issues/2417)).
