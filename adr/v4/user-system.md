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
            UserPermissions[] - selected permissions that define what the token can do
            Routes[] {
                Project.Route
                ProjectAccessPermissions[] - a set of rules that define what the token can do {
                    - read
                    - write
                }
            }
        }
    }
}

Project {
    Name
    Routes[] - a set of rules that define what paths the token can access {
        Repository - repo
        Path (optional) - e.g. gav for maven repos
    }
}
```

### UX

##### Case 1: From zero to first successful deployment

1. User starts Reposilite
2. On the first run, the user is asked to create an admin account
3. User creates an admin account (the admin team is pre-created)
4. It is suggested to user to create a new access token
5. User creates a new access token <br>
5.1. Access token with full permissions is created _(simple way)_<br>
5.2. User generates token with a specific set of permissions _(recommended)_<br>
     - Create a project and assign routes
     - Select following routes in the access token UI
6. User copies the token and uses it to authenticate with Reposilite


##### Case 2: A new user joins the team

1. Manager (user with management permissions) goes to the user management section
2. Manager creates a new user account with permission
3. User logs in to the UI, chooses a password, and logs in
4. User creates a new access token
   4.1. Access token with full permissions is created _(simple way)_<br>
   4.2. User generates token with a specific set of permissions _(recommended)_<br>
      - Create a project and assign routes
      - Select following routes in the access token UI
5. User can deploy artifacts to the repository using the token