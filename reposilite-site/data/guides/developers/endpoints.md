---
id: endpoints
title: Endpoints
---

All endpoints are described using OpenApi. Useful links:

* [Reposilite / OpenAPI Scheme](https://maven.reposilite.com/openapi)
* [Reposilite / Swagger](https://maven.reposilite.com/swagger)

Raw JSON scheme:

```json
{
  "openapi": "3.0.3",
  "info": {
    "title": "Reposilite Repository",
    "version": "3.5.19-SNAPSHOT",
    "description": "Official public Maven repository powered by Reposilite 💜"
  },
  "paths": {
    "/api/auth/me": {
      "get": {
        "tags": [
          "Auth"
        ],
        "summary": "Get token details",
        "description": "Returns details about the requested token",
        "parameters": [
          {
            "name": "Authorization",
            "in": "header",
            "description": "Name and secret provided as basic auth credentials",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Details about the token for succeeded authentication",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/SessionDetails"
                }
              }
            }
          },
          "401": {
            "description": "Error message related to the unauthorized access in case of any failure",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/badge/latest/{repository}/{gav}": {
      "get": {
        "tags": [
          "Maven",
          "Badge"
        ],
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Artifact's repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifacts' GAV",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/console/execute": {
      "post": {
        "tags": [
          "Cli"
        ],
        "summary": "Remote command execution",
        "description": "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
        "parameters": [
          {
            "name": "Authorization",
            "in": "header",
            "description": "Name and secret provided as basic auth credentials",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Status of the executed command",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ExecutionResponse"
                }
              }
            }
          },
          "400": {
            "description": "Error message related to the invalid command format (0 < command length < 1024)",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          },
          "401": {
            "description": "Error message related to the unauthorized access",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/console/log": {
      "get": {
        "tags": [
          "Console"
        ],
        "description": "Streams the output of logs through an SSE Connection.",
        "parameters": [
          {
            "name": "Authorization",
            "in": "header",
            "description": "Name and secret provided as basic auth credentials",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Continuously sends out the log as messages under the `log` event. Sends a keepalive ping through comments."
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/console/sock": {
      "patch": {
        "tags": [
          "Console"
        ],
        "parameters": [],
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/details/{repository}/{gav}": {
      "get": {
        "tags": [
          "Maven"
        ],
        "summary": "Browse the contents of repositories using API",
        "description": "Get details about the requested file as JSON response",
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Returns document (different for directory and file) that describes requested resource",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/FileDetails"
                }
              }
            }
          },
          "401": {
            "description": "Returns 401 in case of unauthorized attempt of access to private repository",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          },
          "404": {
            "description": "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository"
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/generate/pom/{repository}/{gav}": {
      "post": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "requestBody": {
          "description": "GroupId, ArtifactId and Version of the stub POM",
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/PomDetails"
              }
            }
          },
          "required": true
        },
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/latest/details/{repository}/{gav}": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "extension",
            "in": "query",
            "description": "Changes extension of matched file (by default matches 'jar')",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "classifier",
            "in": "query",
            "description": "Appends classifier suffix to matched file",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "filter",
            "in": "query",
            "description": "Version (prefix) filter to apply",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Details about the given file",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/FileDetails"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/latest/file/{repository}/{gav}": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "extension",
            "in": "query",
            "description": "Changes extension of matched file (by default matches 'jar')",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "classifier",
            "in": "query",
            "description": "Appends classifier suffix to matched file",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "filter",
            "in": "query",
            "description": "Version (prefix) filter to apply",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/latest/version/{repository}/{gav}": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "extension",
            "in": "query",
            "description": "Changes extension of matched file (by default matches 'jar')",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "classifier",
            "in": "query",
            "description": "Appends classifier suffix to matched file",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "filter",
            "in": "query",
            "description": "Version (prefix) filter to apply",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "type",
            "in": "query",
            "description": "Format of expected response type: empty (default) for json; 'raw' for plain text",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "",
            "content": {
              "text/plain": {
                "schema": {
                  "type": "string"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/versions/{repository}/{gav}": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "filter",
            "in": "query",
            "description": "Version (prefix) filter to apply",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/settings/domain/{name}": {
      "get": {
        "tags": [
          "Settings"
        ],
        "summary": "Find configuration",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of configuration to fetch",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Returns dto representing configuration"
          },
          "401": {
            "description": "Returns 401 if token without moderation permission has been used to access this resource"
          },
          "404": {
            "description": "Returns 404 if non-existing configuration is requested"
          }
        },
        "deprecated": false,
        "security": []
      },
      "put": {
        "tags": [
          "Settings"
        ],
        "summary": "Update configuration",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of configuration to update",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Returns 200 if configuration has been updated successfully"
          },
          "401": {
            "description": "Returns 401 if token without moderation permission has been used to access this resource"
          },
          "404": {
            "description": "Returns 404 if non-existing configuration is requested"
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/settings/domains": {
      "get": {
        "tags": [
          "Settings"
        ],
        "summary": "List configurations",
        "parameters": [],
        "responses": {
          "200": {
            "description": "Returns list of configuration names"
          },
          "401": {
            "description": "Returns 401 if token without moderation permission has been used to access this resource"
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/settings/schema/{name}": {
      "get": {
        "tags": [
          "Settings"
        ],
        "summary": "Get schema",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of schema to get",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Returns dto representing configuration schema",
            "content": {
              "text/plain": {
                "schema": {
                  "type": "string"
                }
              }
            }
          },
          "401": {
            "description": "Returns 401 if token without moderation permission has been used to access this resource"
          },
          "404": {
            "description": "Returns 404 if non-existing configuration schema is requested"
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/statistics/resolved/all": {
      "get": {
        "tags": [
          "Statistics"
        ],
        "parameters": [],
        "responses": {
          "200": {
            "description": "Aggregated list of statistics per each repository",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/AllResolvedResponse"
                }
              }
            }
          },
          "401": {
            "description": "When non-manager token is used",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/statistics/resolved/phrase/{limit}/{repository}/{gav}": {
      "get": {
        "tags": [
          "Statistics"
        ],
        "parameters": [
          {
            "name": "limit",
            "in": "path",
            "description": "Amount of records to find (Maximum: 100",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "repository",
            "in": "path",
            "description": "Repository to search in",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Phrase to search for",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Aggregated sum of resolved requests with list a list of them all",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ResolvedCountResponse"
                }
              }
            }
          },
          "401": {
            "description": "When invalid token is used",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/statistics/resolved/unique": {
      "get": {
        "tags": [
          "Statistics"
        ],
        "parameters": [],
        "responses": {
          "200": {
            "description": "Number of all unique requests",
            "content": {
              "application/json": {
                "schema": {
                  "type": "integer",
                  "format": "int64"
                }
              }
            }
          },
          "401": {
            "description": "When non-manager token is used",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/ErrorResponse"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/status/instance": {
      "get": {
        "tags": [],
        "parameters": [],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/InstanceStatusResponse"
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/status/snapshots": {
      "get": {
        "tags": [],
        "parameters": [],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": {
                    "$ref": "#/components/schemas/StatusSnapshot"
                  }
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/tokens": {
      "get": {
        "tags": [
          "Tokens"
        ],
        "summary": "Returns all existing tokens and data such as their permissions. Note: Requires Manager",
        "parameters": [],
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/tokens/{name}": {
      "get": {
        "tags": [
          "Tokens"
        ],
        "summary": "Returns data about the token given via it's name. Note: Requires manager or you must be the token owner",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of the token to be deleted",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {},
        "deprecated": false,
        "security": []
      },
      "put": {
        "tags": [
          "Tokens"
        ],
        "summary": "Creates / Updates a token via the specified body. Note: Requires manager permission.",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of the token to be deleted",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "requestBody": {
          "description": "Data about the account including the secret and it's permissions",
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/CreateAccessTokenWithNoNameRequest"
              }
            }
          },
          "required": true
        },
        "responses": {},
        "deprecated": false,
        "security": []
      },
      "delete": {
        "tags": [
          "Tokens"
        ],
        "summary": "Deletes the token specified via it's name. Note: Requires Manager",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of the token to be deleted",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/{repository}/{gav}": {
      "get": {
        "tags": [
          "Maven"
        ],
        "summary": "Browse the contents of repositories",
        "description": "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Input stream of requested file",
            "content": {
              "multipart/form-data": {}
            }
          },
          "404": {
            "description": "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository"
          }
        },
        "deprecated": false,
        "security": []
      },
      "post": {
        "tags": [
          "Maven"
        ],
        "summary": "Deploy artifact to the repository",
        "description": "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
        "parameters": [
          {
            "name": "X-Generate-Checksums",
            "in": "header",
            "description": "Determines if Reposilite should generate checksums for this file",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Input stream of requested file",
            "content": {
              "multipart/form-data": {}
            }
          },
          "401": {
            "description": "Returns 401 for invalid credentials"
          },
          "507": {
            "description": "Returns 507 if Reposilite does not have enough disk space to store the uploaded file"
          }
        },
        "deprecated": false,
        "security": []
      },
      "put": {
        "tags": [
          "Maven"
        ],
        "summary": "Deploy artifact to the repository",
        "description": "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
        "parameters": [
          {
            "name": "X-Generate-Checksums",
            "in": "header",
            "description": "Determines if Reposilite should generate checksums for this file",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Input stream of requested file",
            "content": {
              "multipart/form-data": {}
            }
          },
          "401": {
            "description": "Returns 401 for invalid credentials"
          },
          "507": {
            "description": "Returns 507 if Reposilite does not have enough disk space to store the uploaded file"
          }
        },
        "deprecated": false,
        "security": []
      },
      "delete": {
        "tags": [
          "Maven"
        ],
        "summary": "Delete the given file from repository",
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {},
        "deprecated": false,
        "security": []
      }
    }
  },
  "components": {
    "schemas": {
      "SessionDetails": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "accessToken": {
            "$ref": "#/components/schemas/AccessTokenDto"
          },
          "permissions": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/AccessTokenPermission"
            }
          },
          "routes": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/Route"
            }
          }
        },
        "required": [
          "accessToken",
          "permissions",
          "routes"
        ]
      },
      "CommandStatus": {
        "type": "string",
        "enum": [
          "SUCCEEDED",
          "FAILED"
        ]
      },
      "ExecutionResponse": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "status": {
            "$ref": "#/components/schemas/CommandStatus"
          },
          "response": {
            "type": "array",
            "items": {
              "type": "string"
            }
          }
        },
        "required": [
          "status",
          "response"
        ]
      },
      "LatestVersionResponse": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "snapshot": {
            "type": "boolean"
          },
          "version": {
            "type": "string"
          }
        },
        "required": [
          "snapshot",
          "version"
        ]
      },
      "PomDetails": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "groupId": {
            "type": "string"
          },
          "artifactId": {
            "type": "string"
          },
          "version": {
            "type": "string"
          }
        },
        "required": [
          "groupId",
          "artifactId",
          "version"
        ]
      },
      "ErrorResponse": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "status": {
            "type": "integer",
            "format": "int32"
          },
          "message": {
            "type": "string"
          }
        },
        "required": [
          "status",
          "message"
        ]
      },
      "AllResolvedResponse": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "statisticsEnabled": {
            "type": "boolean"
          },
          "repositories": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/RepositoryStatistics"
            }
          }
        },
        "required": [
          "statisticsEnabled",
          "repositories"
        ]
      },
      "IntervalRecord": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "date": {
            "type": "integer",
            "format": "int64"
          },
          "count": {
            "type": "integer",
            "format": "int64"
          }
        },
        "required": [
          "date",
          "count"
        ]
      },
      "RepositoryStatistics": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "name": {
            "type": "string"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/IntervalRecord"
            }
          }
        },
        "required": [
          "name",
          "data"
        ]
      },
      "ResolvedCountResponse": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "sum": {
            "type": "integer",
            "format": "int64"
          },
          "requests": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/ResolvedEntry"
            }
          }
        },
        "required": [
          "sum",
          "requests"
        ]
      },
      "ResolvedEntry": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "gav": {
            "type": "string"
          },
          "count": {
            "type": "integer",
            "format": "int64"
          }
        },
        "required": [
          "gav",
          "count"
        ]
      },
      "InstanceStatusResponse": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "version": {
            "type": "string"
          },
          "latestVersion": {
            "type": "string"
          },
          "uptime": {
            "type": "integer",
            "format": "int64"
          },
          "usedMemory": {
            "type": "number",
            "format": "double"
          },
          "maxMemory": {
            "type": "integer",
            "format": "int32"
          },
          "usedThreads": {
            "type": "integer",
            "format": "int32"
          },
          "maxThreads": {
            "type": "integer",
            "format": "int32"
          },
          "failuresCount": {
            "type": "integer",
            "format": "int32"
          }
        },
        "required": [
          "version",
          "latestVersion",
          "uptime",
          "usedMemory",
          "maxMemory",
          "usedThreads",
          "maxThreads",
          "failuresCount"
        ]
      },
      "StatusSnapshot": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "at": {
            "type": "integer",
            "format": "int64"
          },
          "memory": {
            "type": "integer",
            "format": "int32"
          },
          "threads": {
            "type": "integer",
            "format": "int32"
          }
        },
        "required": [
          "at",
          "memory",
          "threads"
        ]
      },
      "FileDetails": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "type": {
            "$ref": "#/components/schemas/FileType"
          },
          "name": {
            "type": "string"
          }
        },
        "required": [
          "type",
          "name"
        ]
      },
      "FileType": {
        "type": "string",
        "enum": [
          "FILE",
          "DIRECTORY"
        ]
      },
      "AccessTokenIdentifier": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "type": {
            "$ref": "#/components/schemas/AccessTokenType"
          },
          "value": {
            "type": "integer",
            "format": "int32"
          }
        },
        "required": [
          "type",
          "value"
        ]
      },
      "AccessTokenPermission": {
        "type": "string",
        "enum": [
          "MANAGER"
        ]
      },
      "AccessTokenType": {
        "type": "string",
        "enum": [
          "PERSISTENT",
          "TEMPORARY"
        ]
      },
      "Route": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "path": {
            "type": "string"
          },
          "permission": {
            "$ref": "#/components/schemas/RoutePermission"
          }
        },
        "required": [
          "path",
          "permission"
        ]
      },
      "RoutePermission": {
        "type": "string",
        "enum": [
          "READ",
          "WRITE"
        ]
      },
      "AccessTokenDto": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "identifier": {
            "$ref": "#/components/schemas/AccessTokenIdentifier"
          },
          "name": {
            "type": "string"
          },
          "createdAt": {
            "type": "string",
            "format": "date"
          },
          "description": {
            "type": "string"
          }
        },
        "required": [
          "identifier",
          "name",
          "createdAt",
          "description"
        ]
      },
      "CreateAccessTokenWithNoNameRequest": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "type": {
            "$ref": "#/components/schemas/AccessTokenType"
          },
          "secretType": {
            "$ref": "#/components/schemas/SecretType"
          },
          "secret": {
            "type": "string",
            "nullable": true
          },
          "permissions": {
            "type": "array",
            "items": {
              "type": "string"
            }
          }
        },
        "required": [
          "type",
          "secretType",
          "permissions"
        ]
      },
      "SecretType": {
        "type": "string",
        "enum": [
          "RAW",
          "ENCRYPTED"
        ]
      }
    },
    "securitySchemes": {}
  },
  "servers": [],
  "security": null
}
```