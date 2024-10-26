# Database

We want to try out Sqiffy for the database layer, so we can reduce codebase size + have something more stable than Exposed.

### Schema

We'll figure out the schema as we go, but we'll start with the following entities:

```
                 Repository                                                  
                                                                             
                      ^                                                      
                      |                                                      
                      |                                                      
                      |                                                      
                                                                             
Project <---------- Route                                                    
                                                                             
                      ^                                                      
                      |                                                      
                      |                                                      
                      |                                                      
                      v                                                      
                                                                             
                 RouteAccess  <-------  Token  <--------  User <------> Team 
                                                                             
```