---
id: apache
title: Apache
---

Apache HTTP is another popular http server with support for reverse proxy.
Make sure, youn enabled the rewrite-module.
```bash
sudo a2enmod rewrite
```

The most basic configuration should look like this:

```json5
# reposilite is listening on 127.0.0.1:8081
<IfModule mod_rewrite.c>
    RewriteEngine On

    RewriteCond %{HTTP:Upgrade} =websocket [NC]
    RewriteRule ^/api/(.*) ws://127.0.0.1:8081/api/$1 [P,L]
<IfModule mod_rewrite.c>

ProxyPass / http://127.0.0.1:8081/
ProxyPassReverse / http://127.0.0.1:8081/
```

This configuration assumes that:
* Reposilite runs on `127.0.0.1:8081`
* Reverse proxy should support websocket connection to CLI

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
    <IfModule mod_rewrite.c>
        RewriteEngine On

        # reposilite is listening on 127.0.0.1:8081
        RewriteCond %{HTTP:Upgrade} =websocket [NC]
        RewriteRule ^/reposilite/api/(.*) ws://127.0.0.1:8081/api/$1 [P,L]
    <IfModule mod_rewrite.c>

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
Make sure, youn enabled the rewrite-module.
enable it with:
```bash
sudo a2enmod ssl rewrite
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

    <IfModule mod_rewrite.c>
            RewriteCond %{HTTP:Upgrade} =websocket [NC]
            RewriteRule ^/api/(.*) ws://127.0.0.1:8081/api/$1 [P,L]
    </IfModule>

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