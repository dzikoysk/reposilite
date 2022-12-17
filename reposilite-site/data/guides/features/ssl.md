---
id: ssl
title: SSL
---

In most cases people does not really want to use SSL directly in Reposilite, because they serve it behind some proxy services like Nginx and usually that's where they should set it up. 

If you're not sure how to bootstrap your infrastructure and SSL, take a look on the official Javalin SSL guide:

* [Javalin.io / SSL Tutorial](https://javalin.io/tutorials/javalin-ssl-tutorial)

### PEM

You can find several tutorials on how to generate PKCS certificate from e.g. `Let's Encrypt`.
We can recommend to take a look on the official 
[Javalin.io / SSL Tutorial - Generate self-signed certificate](https://javalin.io/tutorials/javalin-ssl-tutorial#securing-javalin-with-ssl), 
because we're using this integration to support PEM certificates in Reposilite.

### JKS

JKS stands for Java KeyStore format. 
You may find various resources related to this topic. 
Take a look at e.g. on Jenkov's blog or directly in Javalin's/Jetty examples:

* http://tutorials.jenkov.com/java-cryptography/keytool.html
* https://github.com/tipsy/javalin-http2-example

JKS file is directly handled by Jetty [Jetty](https://github.com/eclipse/jetty.project) instance, 
but in general we recommend to use PEM certificates, as it's just easier and more popular approach.
