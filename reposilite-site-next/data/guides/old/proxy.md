---
id: proxy
title: Proxy
sidebar_label: Proxy and caching
---

Our projects often use many repositories.
To simplify your build files, 
you can list all of these repositories in `proxy` section 
and Reposilite will also search for requested artifacts among them:

 ```json5
# List of proxied repositories associated with this repository.
# Reposilite will search for a requested artifact in remote repositories listed below.
# Supported flags:
# --store - Reposilite can store proxied artifacts locally to reduce response time and improve stability
# --connectTimeout=<seconds> - How long Reposilite can wait for establishing the connection with a remote host (default: 3s)
# --readTimeout=<seconds> - How long Reposilite can read data from remote proxy. (default: 15s)
proxied: [
  https://repo.panda-lang.org/releases --store 
]
```

**Note**: Remember about increasing disk quota! 
Caching may allocate thousands of artifacts, especially at the beginning - for the first few builds. 

```json5
# Used storage type. Supported storage providers:
# > File system (local) provider. Supported flags:
# --quota 10GB = control the maximum amount of data stored in this repository. (Supported formats: 90%, 500MB, 10GB)
# Example usage:
# storageProvider: fs --quota 50GB
# > S3 provider. Supported flags:
# --endpoint = overwrite the AWS endpoint (optional)
# --access-key = overwrite AWS access-key used to authenticate (optional)
# --secret-key = overwrite AWS secret-key used to authenticate (optional)
# --region = overwrite AWS region (optional)
# See software.amazon.awssdk.services.s3.S3Client for default values
# Example usage:
# storageProvider: s3 bucket-name --endpoint custom.endpoint.com --access-key accessKey --secret-key secretKey --region region
storageProvider: fs --quota 85%
```

Stable Reposilite instance should guarantee much better availability than any other public repository - even Maven Central repository.