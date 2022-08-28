---
id: tokens
title: Tokens
---

To simplify management process and reduce complex permission system between users and available projects, Reposilite does not define such entities. The only source of truth is personal access token with associated metadata. 

The generalized token structure is presented below:

```yaml
Access Token {
  Identifier {                # Unique identifier of each token
    Id,                       # Numeric value
    Type                      # Type of token, enum: PERSISTENT, TEMPORARY
  }
  Name                        # Token name
  Secret                      # Encrypted secret using BCrypt function
  Created At                  # Creation date
  Description                 # Token description
  Permissions [               # List of access token permissions
    Access Token Permission   # Enum: MANAGER (m)
  ]
  Routes [                    # List of routes associated with access token
    Route {
      Path                    # Path identifier
      Route Permission        # Enum: READ (r), WRITE (w)
    }
  ]
}
```

This guide covers only raw access token, that by default, does not have access to any path in any repository.
When you'll generate your tokens, go to [Guide / Routes](/guide/routes) to learn more about paths management.

### Permissions

| Name | Shortcut | Description |
| :--- | :--: | :---: |
| `MANAGER` | `m` | Marks token as manager's (admin) token, grants full access to any path in the repository and allows you to access remote CLI through the dashboard |

### Generating token
You can generate command via CLI _(in terminal or web console)_:

```bash
$ token-generate [--secret=<secret>] <name> [<permissions>]
```

`secret` is optional and it's recommended to let Reposilite generate strong password,
but it might be useful for some people that want to migrate from Reposilite 2.x or other repository manager.

`permissions` is optional list of token permissions. Currently, you can define only one, 
which is management permission. You have to use permissions shortcuts.

#### Temporary tokens
For bootstrapping your permissions setup, or fixing a broken one, you may have to use a temporary manager token. Via the `--token root:root-secret` command line parameter to your Reposilite instance, you can create a manager token named `root` with the secret `root-secret`. This token will not be persisted in the database and cease to exist if the Reposilite process is restarted without the command line option.

#### First access visualization
If you're confused about your first auth configuration, you may find this video helpful:

  <Spoiler title="Open visualization">
    <video controls>
      <source src="/images/guides/token-generate.webm" type="video/webm" />
      Your browser does not support the video tag.
    </video>
  </Spoiler>

### Export and import
Reposilite allows you to export your tokens to JSON file, so you can backup them or exchange between instances.
You can do this using `token-export <file>` and `token-import <file>` commands as follows:

```bash
$ token-export tokens.json
3 token(s) found.
All tokens have been exported to $working-directory/tokens.json in JSON format.
```

By default Reposilite puts the file in your working directory, but you can also specify an absolute path.
To import such JSON file, just specify it in the import command:

```bash
$ token-import tokens.json
Importing 3 token(s) from $working-directory/tokens.json file:
Access token 'token-0' has been imported.
Access token 'token-1' has been imported.
Access token 'token-2' has been imported.
```

### Other commands
Commands related to tokens you may find useful:

```bash
$ tokens
14:13:41.456 INFO | Tokens (1)
14:13:41.456 INFO | - root:
14:13:41.456 INFO |   > ~ no routes ~

$ token-revoke root
14:20:03.834 INFO | Token for 'root' has been revoked

$ token-rename root super-user
14:28:47.502 INFO | Token name has been changed from 'root' to 'super-user'

$ token-modify super-user m
14:30:26.320 INFO | Permissions have been changed from '[]' to 'm'

$ token-regenerate super-user
16:49:45.211 INFO | New secret for 'test': 4n1NAKgAuzqdZ+mS/OIhqdpqjTyjI78OkgC/Yym5+3vGoXaefYQ2DKqK14c80xNl
```