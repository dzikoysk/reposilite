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
* Routes - list of paths and their permissions covered by the current token

### Permission
Currently supported permissions:
* `m` - marks token as manager's *(admin)* token, grants full access to any path in the repository and allows you to access remote CLI through the dashboard

### Generate token
Tokens are generated using the `token-generate` command in Reposilite CLI:

```log
$ token-generate [--secret=<secret>] <name> [<permissions>]
```

As an example, we can generate access token for `root`:
```bash
$ token-generate root m
14:17:57.400 INFO | Generated new access token for root with 'm' permissions. Secret:
14:17:57.400 INFO | klRvqUGjxCAPnKpmmKCLlXnQhm4w06/aYQSFFgvjUjPkjG+HpwMAokO7BL+sIvJb

$ token-generate --secret=my-secret-token root m
14:11:49.872 INFO | Generated new access token for root with 'm' permissions. Secret:
14:11:49.872 INFO | my-secret-token
```

## Properties

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

## Other commands

### List tokens
To display list of all generated tokens, just use `tokens` command in Reposilite CLI.
```bash
$ tokens
14:13:41.456 INFO | Tokens (2)
14:13:41.456 INFO | - root:
14:13:41.456 INFO |   > ~ no routes ~
14:13:41.456 INFO | - root1:
14:13:41.456 INFO |   > ~ no routes ~
```

### Revoke tokens
You can revoke token using the `token-revoke <alias>` command in Reposilite CLI.
```bash
$ token-revoke root
14:20:03.834 INFO | Token for 'root' has been revoked
```
### Renaming tokens
You can rename token using the `token-rename <name> <new name>` command in Reposilite CLI.
```bash
$ token-rename root super-user
14:28:47.502 INFO | Token name has been changed from 'root' to 'super-user'
```

### Modifying tokens permissions
You can change tokens permissions using the `token-modify <name> <permissions>' command in Reposilite CLI.
```bash
$ token-modify super-user m
14:30:26.320 INFO | Permissions have been changed from '[]' to 'm'
```

## Routes

### Adding access to route
You can add access to specified route for token using the `route-add <name> <path> <permissions>` command in Reposilite CLI.
```bash
$ route-add reposilite-publisher /releases/com/reposilite w
11:53:15.880 INFO | Route Route(path=/releases/com/reposilite, permissions=[WRITE]) has been added to token reposilite-publisher
```

#### Permission
Currently supported permissions:
* `r` - allows token to read resources under the associated path
* `w` - allows token to write (deploy) resources under the associated path

### Removing access to route
You can remove access to specified route for token using the `route-remove <name> <path>` command in Reposilite CLI.
```bash
$ route-remove reposilite-publisher /releases/com/reposilite
11:57:38.289 INFO | Token reposilite-publisher has been updated, new routes: []
```
