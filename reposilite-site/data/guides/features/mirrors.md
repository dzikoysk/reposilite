---
id: mirrors
title: Mirrors
---

Our projects often use various repositories.
To simplify & speed up your build process, 
you can list all of these repositories in `Mirrored repositories` section 
and Reposilite will also search for requested artifacts among them:

![Mirrored Repositories](/images/guides/mirrored-repositories.png)

**Note**: Remember about increasing disk quota! 
Caching may allocate thousands of artifacts, especially at the beginning - for the first few builds. 

<Spoiler title="Disk quota configuration">

![Disk Quota](/images/guides/disk-quota.png)

Stable Reposilite instance should guarantee much better availability than any other public repository - even Maven Central repository.

</Spoiler>

### Mirror configuration
Each proxied repository has individual configuration that can be adjusted to your needs:

#### Link
The link property can handle 2 types of values:
- **URL** - direct link to remote repository, for example: `https://repo.maven.apache.org/maven2/`
- **Local ID** - ID of another local repository, for example: `releases`

#### Store
By default, proxied artifacts are not stored in the repository.
If you want to improve response time or availability,
you can enable storing artifacts in the local repository.

#### Storage policy
You can configure the storage policy of proxied artifacts.

| Name                         | Description                                                                            |
|------------------------------|----------------------------------------------------------------------------------------|
| PRIORITIZE_UPSTREAM_METADATA | _(Default)_ Try to fetch the latest version of the artifact from the remote repository |
| STRICT                       | Prioritize cached version over upstream metadata (full offline mode)                   |

**Note**: This property can be only set for a whole repository.

#### Allowed groups
You can limit the scope of proxied artifacts by specifying list of allowed groups, e.g.:

```bash
org.reposilite
```

If the list is empty, all groups are allowed.

#### Allowed extensions
You can limit the scope of proxied artifacts by specifying list of allowed extensions.
By default, Reposilite allows these extensions:

- `.jar`
- `.war`
- `.xml`
- `.pom`
- `.module`
- `.asc`
- `.md5`
- `.sha1`
- `.sha256`
- `.sha512`

#### Timeouts
If your repository requests remote repository that is often unable to respond in time,
you can increase the timeout value to prevent such issues.

| Name    | Description                                            | Default |
|---------|--------------------------------------------------------|---------|
| Connect | Time required to establish a connection                | `3s`    |
| Read    | How long Reposilite can read the data from remote host | `15s`   |

#### Credentials
Mirrored proxy may require authentication to access the repository.
Reposilite supports 3 types of authentication:

##### Basic
Basic authentication is the simplest method to authenticate the user.
It requires username and password to access the repository.

```yaml
Login: Admin
Password: Secret
```

##### Header
Some repositories may require authentication via a custom header & some kind of API key.
You can configure Reposilite to use such authentication by specifying header name and value:

```yaml
Login: X-Api-Key
Password: Token
```

##### Loopback link
Since Reposilite 3.4.x, 
private local repositories requires dedicated authentication as well.
This enhanced security layer prevents accidental access to private repositories.

If you want to proxy private local repository to another local repository (public or private),
you should generate a new token that has access to the proxied repository (requires read permission):

```yaml
Login: reposilite-proxy-token
Password: secret
```

#### Proxy

Reposilite supports 2 type of proxies:

- HTTP, for instance:

```bash
HTTP 127.0.0.1:1081 
```

- SOCKS, e.g.:

```bash
SOCKS 127.0.0.1:1080 login password 
```


