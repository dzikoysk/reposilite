---
id: routes
title: Routes
---

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