# Configuration
 
```
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
    Name
    Type (Genric, Maven, Docker)
    Owner (user)
    Storage provider {
        Local
        S3
    }
    Mirros[] {
    
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
    Title
    Logo
    Description 
}
```