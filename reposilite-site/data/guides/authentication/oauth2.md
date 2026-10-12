---
id: oauth2
title: OAuth2
---

Reposilite can delegate authentication to an external identity provider that supports OpenID Connect,
such as Keycloak, Authentik, Microsoft Entra ID, Okta or Google.
The integration is disabled by default and covers two independent use cases:

* **Dashboard sign in** - users sign in to the dashboard through the identity provider (authorization code flow with PKCE)
* **Bearer tokens** - clients authenticate with a JWT issued by the identity provider instead of an access token

In both cases the user is mapped to an [access token](/guide/tokens) named after the username provided by the identity provider.
If such a token does not exist yet, Reposilite creates it **without any permissions**,
so you have to assign [routes](/guide/routes) to it, just like you'd do for any other token.

To use it, register Reposilite as a client in your identity provider with the following redirect URI:

```
https://<your-reposilite-domain>/api/auth/oauth2/callback
```

Then, enable OAuth2 in the authentication section of shared settings and configure it:

|        Property        |             Example value             |                                                 Description                                                  |
|:----------------------:|:-------------------------------------:|:------------------------------------------------------------------------------------------------------------:|
|      providerName      |               Keycloak                |                                  Name displayed on the sign in button                                   |
|         issuer         | https://sso.domain.com/realms/company | Issuer URL of the provider, the one that serves `/.well-known/openid-configuration`                          |
|        clientId        |              reposilite               |                               Identifier of the client registered in the provider                               |
|      clientSecret      |              client-secret              |                         Secret of the client, leave it empty if the client is public                          |
|         scopes         |         openid profile email          |                                      Scopes requested during sign in                                       |
|     usernameClaim      |          preferred_username           |                       Claim that provides the unique username used as access token name                        |
|        userType        |        TEMPORARY or PERSISTENT        |                                             Type of mapped token                                             |
|    redirectBaseUrl     |       https://repo.domain.com         | Public URL of Reposilite used to build the redirect URI, by default it is detected from the incoming request |
| sessionDurationMinutes |                  480                  |                                       Duration of the dashboard session                                       |
|     bearerEnabled      |             true or false             |                                  Accept JWTs issued by the provider as credentials                                  |
|     bearerAudience     |                 maven                 |                    Audience (`aud` claim) that the JWT has to be issued for, required for bearer tokens                     |

`Tip` If Reposilite runs behind a reverse proxy, set `redirectBaseUrl` explicitly.
Reposilite does not read `X-Forwarded-Proto` and `X-Forwarded-Host` headers,
so the detected URL may point to the internal address.

#### Dashboard sign in

Once enabled, the login dialog offers an additional _Sign in with ..._ button.
Dashboard sessions are kept in memory, so users have to sign in again after a restart of Reposilite.

A token created this way has a random secret that is not known to anyone.
To let such a user access the repository with Maven or Gradle, use bearer tokens
or regenerate the secret of the token as a manager.

#### Bearer tokens

A JWT can be provided in the `Authorization: Bearer <token>` header, or as a password of standard basic credentials (the username is ignored),
which is convenient for build tools that only support basic authentication:

```bash
$ curl -H "Authorization: Bearer $TOKEN" https://repo.domain.com/private/com/company/artifact/1.0.0/artifact-1.0.0.jar
$ curl -u "ci:$TOKEN" https://repo.domain.com/private/com/company/artifact/1.0.0/artifact-1.0.0.jar
```

Reposilite verifies the signature against the keys published by the provider and requires the token to be issued by the configured `issuer`,
for the configured `bearerAudience` and not to be expired. Only asymmetric signature algorithms (RSA and ECDSA) are accepted.

#### Good to know

* If an access token with the given username already exists, it is reused together with its permissions.
  Make sure that users of your identity provider cannot pick a username of an existing privileged token, e.g. your management token.
* Usernames that contain `:` or are longer than 254 characters cannot be mapped to access tokens and are rejected.
* Only one identity provider can be configured.
