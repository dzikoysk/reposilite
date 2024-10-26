# Configuration
 
```
Authentication {
    Local {
        Allow registration
    }
    SSO {
        Google {
            Client ID
            Client Secret
            // todo: check extra fields
        }
    }
    LDAP {
        Host
        Port
        Base DN
        Search-User DN
        Search-User Password
        Type attribute
        User attribute
        User filter
        User type (persistent, temporary)
    }
}

Users {
    Existing users[] {
        Edit
        Delete
    }
    Add new user {
        Username
        Email
        Password
        OAuth
        Permissions {
            Manage settings
            Manage teams and users
            Create/delete repositories
            Create/write content
        }
    }
}

Teams[] {
   Existing teams[] {
       Edit
       Delete
   }
   Add new team {
     Name
     Permissions {
          Manage settings
          Manage teams and users
          Create/delete repositories
          Create/write content
     }
   }
}

Repositories[] {
    Name (Identifier)
    Redeployments Enabled
    Type (Genric, Maven, Docker)
    Owner (user)
    Storage provider {
        Local {
            Quota
            Mount
        }
        S3
    }
    ...on Maven {
        Preserve snapshots
        Mirros[] {
            Link (URL or local id)
            Store
            Groups # GAVs that can be requested from this mirror
            Allowed extensions
            Timeouts {
                Connect
                Read
            }
            Authentication {
                Basic {
                    Username
                    Password
                }
                Header {
                    Name
                    Value
                }
            }
            HTTP Proxy {
                HTTP or Socks
            }
        }
    }
    ...on Docker {
    }
}

Projects {
    Identifier
    Owner (user)
    Display Name
    Pinned (on front page)
    Logo (optional)
    Routes[] {
        Repository
        Path
    }
}

UI {
    Id
    Title
    Description
    Website 
    Logo
    ICP License
}

Statistics {
    Enabled
    Accuracy (daily, weekly, monthly, yearly)
}

Other {
    Forwarded IP (header)
}
```