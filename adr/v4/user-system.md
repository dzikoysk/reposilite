# User system

### Context

Reposilite 2.x & 3.x didn't have a concept of users - the whole system was based on tokens.
Although it was a simple and effective solution, 
it was also limited in terms of user management and permissions for the end-users.

### Proposal 
We will introduce a new user system that will allow users manage their own accounts 
& generate granular tokens with specific permissions.

### Design

The user system will be based on the following entities:

```
Team - a group of users sharing the same permissions {
    UserPermissions[] - a set of rules that define what the team can do {
        - manage settings
        - manage teams and users
        - create/delete repositories
        - create/write content
    }
    
    Users[] - a person that can log in to the system {
        Username
        Email (optional) - used for password recovery, notifications, etc. 
        Password (optional) - used for password-based authentication
        Oauth (optional) - used for OAuth-based authentication
        UserPermissions[] - a set of rules that define what the user can do
        
        Tokens[] - a unique identifier that can be used to authenticate the user {
            Routes[] - a set of rules that define what paths the token can access {
                <repository> - repo
                <path> (optional) - e.g. gav for maven repos
                PathPermissions[] - a set of rules that define what the token can do {
                    - read
                    - write
                }
            }
        }
    }
}
```