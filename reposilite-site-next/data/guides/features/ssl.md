---
id: ssl
title: SSL
---

In most cases people does not really want to use SSL directly in Reposilite, because they serve it behind some proxy services like Nginx and usually that's where they should set it up. 

If you'd like to expose Reposilite directly to public domain, 
you have to generate JKS file so we can pass it to [Jetty](https://github.com/eclipse/jetty.project) instance.

JKS stands for Java KeyStore format. 
You may find various resources related to this topic. 
Take a look at e.g. on Jenkov's blog or directly in Javalin's/Jetty examples:

* http://tutorials.jenkov.com/java-cryptography/keytool.html
* https://github.com/tipsy/javalin-http2-example

**`TODO`** - Dedicated guide for Reposilite with SSL setup