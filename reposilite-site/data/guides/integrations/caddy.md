---
id: caddy
title: Caddy
---

Caddy is a http server with support for reverse proxy and automatic https. The automatic https feature 
takes care of requesting, installing and updating SSL certificates which means that you need much less configuration settings
or maintenance compared with e.g. Nginx.

Note that websockets are also transparent in the case of reverse proxying.

First [download and install caddy](https://caddyserver.com/docs/install) and modify your caddy configuration file, 
typically found in `/etc/caddy/Caddyfile`.

```json5

(revproxy) {
         #compress responses
         encode zstd gzip

         #redirect from http to https
         @http {
                protocol http
         }
         redir @http https://{host}{uri}

         reverse_proxy localhost:{args.0}
         file_server
}

domain.com {
        root * /opt/reposilite/static
        import revproxy 8080
}
```

You might need to modify a few things:

* If Reposilite is running on an other machine, replace `localhost` in the `reverse_proxy localhost:{args.0}` statement to the IP or host of that machine.
* Here the `domain.com` refers to the domain name in used. Typically this needs to be changed.
* The port of the Reposilite instance here is 8080, this needs to match your configuration.
* With URL-rewriting it is also possible to redirect a directory `/reposilite' to your instance. I leave this as an excercise for the reader. A good hint can be found in the official [caddy documentation under the matcher header](https://caddyserver.com/docs/caddyfile-tutorial#matchers).

### SSL Configuration

The caddy reverse proxy can be configured to take care of SSL certificates and server Reposilite over HTTPS. 
To enable HTTPS follow these steps: first get a coffee and then start the caddy service. Seriously: you do not need
to configure anything, HTTPS works out of the box. 

### Start the caddy service

If caddy is installed with systemd reloading caddy can be done on the fly and can look like the following. On your system this 
might differ slightly but the idea remains the same: reload the changes to the Caddy configuration and optionally restart.

```
sudo systemctl reload caddy
```

