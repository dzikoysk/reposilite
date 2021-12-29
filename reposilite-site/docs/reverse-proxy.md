---
id: reverse-proxy
title: Reverse proxy
sidebar_label: Reverse proxy
---

Reposilite uses websocket connection to provide remote CLI functionality. 
Proxing such a connection through services like [Nginx](https://www.nginx.com/) or [Apache](https://httpd.apache.org/) requires additional configuration. 


Related GitHub Issue: [#346](https://github.com/dzikoysk/reposilite/issues/346)

## Nginx
Note: You can also use Nginx with SSL on [this page](reverse-proxy-ssl)

```conf
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

# reposilite ip and port
upstream reposilite {
    server domain:8081;
}

server {                                                                                                                                                   
    server_name domain;                                                                                                                                    
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

To use custom base path (e.g. `/reposilite`), modify the configuration just like this:

```conf
location /reposilite/ {
    rewrite /reposilite/(.*) /$1 break;
}
```

And update the base path in Reposilite configuration:

```yaml
# Custom base path
basePath: /reposilite/
```

## Apache

```conf
# reposilite is listening on 127.0.0.1:8081
RewriteEngine On

RewriteCond %{HTTP:Upgrade} =websocket [NC]
RewriteRule ^/api/(.*) ws://127.0.0.1:8081/api/$1 [P,L]

ProxyPass / http://127.0.0.1:8081/
ProxyPassReverse / http://127.0.0.1:8081/
```
