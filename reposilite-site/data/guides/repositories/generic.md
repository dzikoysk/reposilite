---
id: generic
title: Generic
---

Available since Reposilite 3.7, generic repositories store arbitrary files without Maven metadata, checksum, snapshot, or artifact-layout rules.

### Uploading and downloading files

Files are accessed through repository URLs:

```text
GET    /{repository}
HEAD   /{repository}
GET    /{repository}/{path}
HEAD   /{repository}/{path}
PUT    /{repository}/{path}
POST   /{repository}/{path}
DELETE /{repository}/{path}
```

For example, `PUT /downloads/releases/application.tar.gz` uploads a file and `GET` on the same URL downloads it. Writes require a token with write access to the route. Visibility, route permissions, filesystem quotas, [S3 storage](/guide/s3), directory browsing, and redeployment rules work in the same way as for [Maven repositories](/guide/repositories).

The dashboard's file browser remains Maven-specific, so it does not list generic repositories yet.

### Configuration

Generic repositories have their own `generic` settings domain and do not need a `type` field in Maven settings:

```json
{
  "generic": {
    "repositories": [
      {
        "id": "downloads",
        "visibility": "PUBLIC",
        "redeployment": false,
        "storageProvider": {
          "type": "fs",
          "quota": "10GB"
        }
      }
    ]
  }
}
```

Repository ids must be a single non-blank URL path segment. Keep them unique across providers: if multiple
providers expose the same id, that repository URL returns 404 until the conflict is removed.
