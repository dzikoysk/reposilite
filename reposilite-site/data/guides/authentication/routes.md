---
id: routes
title: Routes
---

### Structure

Every route represents relation of [path](#path) to [permissions](#permissions).
You can add multiple routes to each [access token](/guide/tokens).

#### Path

The path must follow the given pattern: `/{repository}/{gav}`:
* `{repository}` - required name of repository used to distinguish repositories
* `{gav}` - optional GAV (`group-artifact-version`) path

Some examples of path declaration and matched URLs:

| Path | Matches | Status |
| :--  | :--     | :--:   |
| / | /releases/* <br/>/snapshots/* | Ok<br/>Ok |
| /releases | /releases/* <br/>/snapshots/* | Ok<br/>Unauthorized |
| /releases/groupId/artifactId | /releases/groupId/artifactId/* <br/>/releases/groupId/* <br/>/snapshots/*| Ok<br/>Unauthorized<br/>Unauthorized |

#### Permissions
Currently supported permissions:
* `r` - allows token to read resources under the associated path
* `w` - allows token to write (deploy) resources under the associated path

> Note: Permissions can be combined (`rw`) to allow read and write on associated paths.

### Commands

#### Adding write access to route
You can add access to specified route for token using the `route-add <token> <path> <permissions>` command in Reposilite CLI.
```bash
$ route-add reposilite-publisher /releases/com/reposilite w
Route Route(path=/releases/com/reposilite, permissions=[WRITE]) has been added to token reposilite-publisher
```

#### Adding full access to route
You can add full access (read and write) to specified route using `rw` for permissions attribute in Reposilite CLI command.
```bash
$ route-add reposilite-publisher /releases/com/reposilite rw
Route Route(path=/releases/com/reposilite, permissions=[READ, WRITE]) has been added to token reposilite-publisher
```

#### Removing access to route
You can remove access to specified route for token using the `route-remove <token> <path>` command in Reposilite CLI.

```bash
$ route-remove reposilite-publisher /releases/com/reposilite
Token reposilite-publisher has been updated, new routes: []
```