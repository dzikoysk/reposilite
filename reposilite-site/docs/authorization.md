---
id: authorization
title: Authorization
sidebar_label: Authorization
---

To simplify management process and reduce complex permission system between users and available projects,
Reposilite uses path based token system.

## Access token
The access token consists of four elements:

* Name - the short name associated with token
* Secret - generated secret token used to access associated path
* Permissions - the permissions associated with token

### Generate token
Tokens are generated using the `token-generate` command in Reposilite CLI:

```log
$ token-generate <secret> <name> [<permissions>]
```

As an example, we can generate access token for `root`:
```bash
$ token-generate -s=my-secret-token root m
14:11:49.872 INFO | Generated new access token for root with 'm' permissions. Secret:
14:11:49.872 INFO | my-secret-token

$ token-generate root m
14:17:57.400 INFO | Generated new access token for root with 'm' permissions. Secret:
14:17:57.400 INFO | klRvqUGjxCAPnKpmmKCLlXnQhm4w06/aYQSFFgvjUjPkjG+HpwMAokO7BL+sIvJb
```

## Properties

### Permission
Currently supported permissions:
* `m` - marks token as manager's *(admin)* token, grants full access to any path in the repository and allows you to access remote CLI through the dashboard

### Path

The path must follow the given pattern: `/{repository}/{gav}` or `x.y.z` gav pattern:
* `{repository}` - required name of repository used to distinguish repositories
* `{gav}` - optional GAV (`group-artifact-version`) path

Some examples:

| Path | Matches | Status |
| :--  | :--     | :--:   |
| /releases | /releases/* | Ok |
| /releases/abc | /releases/abc* | Ok |
| /snapshots | /snapshots/* | Ok |
| / | /releases/* | Ok (if `rewrite-paths` enabled) |
| */ | /* | Ok |

As an example, we can imagine we have several projects located in our repository. 
In most cases, the administrator want to have permission to whole repository, so the credentials for us should look like this:

```properties
path: */
alias: admin
permissions: m
```

Access to requested paths is resolved by comparing the access token path with the beginning of current uri. Our `admin` user associated with `*/` has access to all paths, because all of requests starts with this path separator:

| URI | Status |
| :-- | :----: |
| / | Ok |
| /releases | Ok |
| /releases/groupId/artifactId | Ok |
| /snapshots | Ok |

We also might add some of co-workers to their projects. 
For instance, we will add *Daenerys Targaryen* user as `khaleesi` to `com.hbo.got` project:

```properties
path: /releases/com/hbo/got
alias: khaleesi
```

The access table for the following configuration:

| URI | Status |
| :-- | :----: |
| / | Unauthorized |
| /releases/com/hbo/got | Ok |
| /releases/com/hbo/got/sub-project | Ok |

We recommend to use user-specific names for individual access tokens.
In case of a larger teams, 
it might be a good idea to use project name as an alias and distribute shared access token between them:

```properties
# for a crew
alias: got
# or mixed solution to increase traffic control
alias: got_khaleesi
```

Finally, we can also grant access to multiple repositories using `*` wildcard operator.
As you can see, we have to provide the repository name in access token path. 
In a various situations, we want to maintain `releases` and `snapshots` repositories for the same project.
Instead of generating separate access token, we can just replace repository name with wildcard operator:

```properties
path: */com/hbo/got
alias: khaleesi
```

## Other commands

### List tokens
To display list of all generated tokens, just use `tokens` command in Reposilite CLI:

```bash
$ tokens
14:13:41.456 INFO | Tokens (2)
14:13:41.456 INFO | - root:
14:13:41.456 INFO |   > ~ no routes ~
14:13:41.456 INFO | - root1:
14:13:41.456 INFO |   > ~ no routes ~
```

### Revoke tokens
You can revoke token using the `token revoke <alias>` command in Reposilite CLI.
```bash
$token revoke root
14:20:03.834 INFO | Token for 'root' has been revoked
```