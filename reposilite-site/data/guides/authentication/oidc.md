---
id: oidc
title: OpenID Connect
---

With OpenID Connect (OIDC), people sign in to the web interface through your identity provider (Keycloak, FusionAuth, Microsoft Entra ID, ...) and create their own access tokens for Maven & Gradle.
Administrators no longer hand out and renew tokens, they only decide which paths a user may access.
Builds keep using plain access tokens, so nothing changes for Maven, Gradle or build servers.

### How it works

* A **user** is an access token named after the user, e.g. `john@company.com`.
  Its routes are the maximum paths the user's own tokens may get. Its secret is never shown, it can't be used by Maven.
* A **user token** is named `<user>#<label>`, e.g. `john@company.com#laptop`.
  Users create them in the **Tokens** tab with a subset of their paths, they expire after `tokenLifetimeDays`.
* User tokens never exceed the **current** paths of their user. 
  Removing a path from a user, or deleting the user, applies to all of the user's tokens immediately.
* Managers add users and their paths in the **Users** tab. A user signing in for the first time is created automatically, without any paths.

### Identity provider

Register Reposilite as a confidential client (authorization code flow, PKCE is used as well):

| Setting              | Value                                                   |
|:---------------------|:--------------------------------------------------------|
| Redirect URL         | `https://repo.company.com/oidc/callback`                |
| Logout URL           | `https://repo.company.com/` (used after signing out)    |
| Scopes               | `openid profile email`                                  |
| Roles                | `reposilite-user`, `reposilite-manager`                 |

The roles have to be part of the userinfo response, the ID token or the access token, by default in the `roles` claim.

### Configuration

Enable OIDC in the [shared settings]() under *Authentication / OIDC*:

|     Property      |          Example value           |                                        Description                                         |
|:-----------------:|:--------------------------------:|:------------------------------------------------------------------------------------------:|
|      enabled      |               true               |                       The *Sign in* button uses OpenID Connect                             |
|   discoveryUrl    | https://idp.company.com/realms/x |              Issuer, `/.well-known/openid-configuration` is appended                       |
|     clientId      |            reposilite            |                           Client ID registered in the identity provider                    |
|   clientSecret    |              secret              |                         Client secret registered in the identity provider                  |
|    redirectUri    | https://repo.company.com/oidc/callback |                   Public URL of the callback, must match the identity provider     |
|   usernameClaim   |              email               |                                Claim used as the user name                                 |
|    rolesClaim     |              roles               |                                Claim with the list of roles                                |
|     userRole      |         reposilite-user          |                 Role required to sign in, empty allows every user of the provider         |
|    managerRole    |        reposilite-manager        | Role that grants the manager permission at each sign in, empty to manage it in Reposilite  |
| tokenLifetimeDays |                90                |                               Lifetime of tokens created by users                          |
|   sessionHours    |                8                 |                            How long a web interface login stays valid                      |

`Tip` The *Token* button next to *Sign in* still accepts access tokens, e.g. for administrators while the identity provider is down.

### Using a token

After creating a token in the **Tokens** tab, the secret is shown once together with snippets for Maven & Gradle:

```xml
<server>
  <id>reposilite</id>
  <username>john@company.com#laptop</username>
  <password>secret</password>
</server>
```

### Security notes

* **Offboarding** - disabling a person in the identity provider does **not** disable their tokens, they keep working until they expire.
  Delete the user in the **Users** tab, this disables all of their tokens immediately.
* Sign in is refused if the identity provider reports `email_verified: false`, so nobody can claim someone else's address.
* Sign in never takes over existing access tokens that were not created for OIDC users (e.g. an `admin` or build server token with the same name).
* E-mail addresses are stored in lower case.
* Each user can have at most 20 tokens, expired tokens are removed when the user opens the **Tokens** tab.
* Web interface sessions are signed with a key that exists only in memory, a restart (or a second instance) signs users out of the web interface. Their Maven & Gradle tokens are not affected.
* A deleted token may keep working for up to a minute while it's in use, because Reposilite caches successful logins.
