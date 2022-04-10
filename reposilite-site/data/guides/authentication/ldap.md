---
id: ldap
title: LDAP
---

LDAP support in Reposilite is quite limited. 
It's stale and oldish tool with a very specific approach to domain configuration.
If you feel that current implementation could be improved or slightly changed, let us know.

First of all, you have to enable LDAP authenticator in [shared settings](). 
Then, you can configure configuration to your LDAP server.

| Property | Example value | Description |
| :--: | :---: | :---: |
| baseDn | dc=company,dc=com | Base DN with users | 
| searchUserDn | cn=reposilite,ou=admins,dc=domain,dc=com | User used to perform searches in LDAP server (requires permissions to read all LDAP entries) |
| searchUserPassword | reposilite-admin-secret | Search user's password |
| userAttribute | cn | Attribute in LDAP that represents unique username used to create access token |
| userFilter | (&(objectClass=person)(ou=Maven Users)) | LDAP user filter |
| userType | TEMPORARY or PERSISTENT | Type of mapped token |

`Tip` If you're not familiar with LDAP, 
you may also try to find some detailed docs about LDAP integration in other open source tools such as e.g. [GitLab Docs / LDAP](https://docs.gitlab.com/ee/administration/auth/ldap/). 
LDAP integration in Reposilite was highly inspired by existing implementations, 
so you should find a lot of similarities :)