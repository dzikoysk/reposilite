---
id: nginx
title: Nginx
---

Nginx is the most popular http server with support for reverse proxy.
The most basic configuration should look like this:

```json5
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

# Reposilite IP and port
upstream reposilite {
    server domain.com:8081;
}

server {
    server_name domain.com;
    listen 80;
    listen [::]:80;
    access_log /var/log/nginx/reverse-access.log;
    error_log /var/log/nginx/reverse-error.log;

    client_max_body_size 50m; # maximum artifact upload size

    location / {
        proxy_pass http://reposilite;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_set_header   Upgrade           $http_upgrade;
        proxy_set_header   Connection        $connection_upgrade;
        proxy_http_version 1.1;
    }
}
```

To use custom base path (e.g. `domain.com/reposilite`), modify the configuration just like this:

```json5
location /reposilite/ {
    rewrite /reposilite/(.*) /$1 break;
    # [...]
}
```

And update the base path property in shared configuration:

```yaml
# Custom base path
basePath: /reposilite/
```

### SSL Configuration

Lots of people like to use a reverse proxy like Nginx with Reposilite.
This is a page on how to setup Nginx with SSL.

#### Step 1 - Setup environment

First, install and setup Reposilite.
Make sure to setup Reposilite to listen on port 8080 (or anything other than 80 and 443).

Then, install nginx, openssl and certbot [using snapd\*](https://snapcraft.io/docs/installing-snapd/) 

```bash
$ sudo snap install certbot --classic
$ sudo ln -s /snap/bin/certbot /usr/bin/certbot
```

\*[snapd is recommended by certbot](https://certbot.eff.org/instructions?ws=other&os=ubuntufocal)

#### Step 2 - Generate certificates

Next you have to generate your certificates. 
To do this you'll need a valid domain name and have your server pointed at it.
Run:

```bash
$ sudo certbot certonly --standalone
```
And follow the instructions. Then, run:

```bash
$ sudo mkdir /etc/nginx/ssl
$ sudo openssl dhparam -out /etc/nginx/ssl/dhparam.pem 2048
```

Also make sure `www-data` can read the file. This will take a while.

#### Step 3 - Configure Nginx

Create these files (replace repo.example.com with your domain):

* `/etc/nginx/sites-available/reposilite-proxy.conf`

```json5
# Prepare easy to use header value for websocket connections - needs to be outside server block

map $http_upgrade $connection_upgrade {
  default upgrade;
  '' close;
}

server {
  server_name repo.example.com;
  listen 443 ssl http2;
  listen [::]:443 ssl http2;

  include /etc/nginx/custom-snippets/ssl.conf;

  location / {
    proxy_pass http://localhost:8080/; # 8080 is the port Reposilite is running on in this setup
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    proxy_set_header Host $host;
  }

  ssl_certificate /etc/letsencrypt/live/repo.example.com/fullchain.pem; # managed by Certbot
  ssl_certificate_key /etc/letsencrypt/live/repo.example.com/privkey.pem; # managed by Certbot
}

# Redirect all http requests to https

server {
  listen 80 default_server;
  listen [::]:80 default_server;
  return 301 https://$host$request_uri;
}
```

* `/etc/nginx/custom-snippets/ssl.conf`

`Hint`: The contents of `/etc/nginx/custom-snippets` directory can also be inlined in place of the included directive, but it's handy to keep them in a separate file so it's reusable.

```json5
# Protocols
ssl_protocols TLSv1.2 TLSv1.3;
# Ciphers
ssl_ciphers EECDH+AESGCM:EECDH+AES256;
ssl_prefer_server_ciphers on;
ssl_session_cache shared:SSL:10m;

# Diffie-Hellman key exchange with better parameters
# Needs to be created via openssl dhparam -out /etc/nginx/ssl/dhparam.pem 2048
ssl_dhparam /etc/nginx/ssl/dhparam.pem;
ssl_ecdh_curve secp384r1;

# HTTP Strict Transport Security
add_header Strict-Transport-Security "max-age=63072000;includeSubdomains;";
```

#### Step 4 - Launch

Finally, run `sudo nginx -t` to verify the config and `sudo systemctl restart nginx` to restart nginx.
This config also works with [Cloudflare](https://www.cloudflare.com/).
