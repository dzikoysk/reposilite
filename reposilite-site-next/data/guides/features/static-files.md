---
id: static-files
title: Static files
---

Despite of content served by each repository, 
Reposilite supports also static files. 
By default, Reposilite creates `public` directory in workdir. 

```yaml
reposilite_working_directory/
  public/
    index.html
  # ...
```

Endpoints used to serve files located in `public` directory have the lowest priority.
Due to this fact, you cannot override routes registered by Reposilite using public files.

#### Placeholders

| Placeholder | Description |
| :-- | :---: |
| \{\{REPOSILITE.BASE_PATH}} | Location where Reposilite instance has been mounted |
| \{\{REPOSILITE.ID}} | Identifier |
| \{\{REPOSILITE.TITLE}} | Title |
| \{\{REPOSILITE.ID}} | Description |
| \{\{REPOSILITE.ORGANIZATION_WEBSITE}} | Link to organization website |
| \{\{REPOSILITE.ORGANIZATION_LOGO}} | Link to an image with logo |

#### Custom frontend

Using `public` directory you can implement frontend on your own.
Because assets have lower priority, 
you have to disable frontend in shared configuration / settings in the dashboard:

```yaml
frontend: false
```

`Warning` - With disabled dashboard, you'll lose access to settings tab.
Since then, you may need to mount shared configuration manually as file.

