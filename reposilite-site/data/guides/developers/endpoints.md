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
    "description": "Public Maven repository hosted through the Reposilite",
    "version": "3.0.0-alpha.25"
  },
  "paths": {
    "/api/console/sock": {
      "patch": {
        "tags": [],
        "parameters": [],
        "requestBody": {
          "content": {},
          "required": false
        },
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
        "description": "Execute command using POST request. The commands are the same as in the console and can be listed using the \u0027help\u0027 command.",
        "parameters": [
          {
            "name": "Authorization",
            "in": "header",
            "description": "Name and secret provided as basic auth credentials",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
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
            "description": "Error message related to the invalid command format (0 \u003c command length \u003c 1024)",
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
    "/api/statistics/resolved/phrase/{limit}/{repository}/*": {
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
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Phrase to search for",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
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
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {
          "200": {
            "description": "Number of all unique requests",
            "content": {
              "application/json": {
                "schema": {
                  "type": "number",
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
    "/api/tokens": {
      "get": {
        "tags": [
          "tokens"
        ],
        "summary": "Returns all existing tokens and data such as their permissions. Note: Requires Manager",
        "parameters": [],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/tokens/{name}": {
      "get": {
        "tags": [
          "tokens"
        ],
        "summary": "Returns data about the token given via it\u0027s name. Note: Requires manager or you must be the token owner",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of the token to be deleted",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {},
        "deprecated": false,
        "security": []
      },
      "put": {
        "tags": [
          "tokens"
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
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "description": "Data about the account including the secret and it\u0027s permissions",
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
          "tokens"
        ],
        "summary": "Deletes the token specified via it\u0027s name. Note: Requires Manager",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of the token to be deleted",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/settings/content/{name}": {
      "get": {
        "tags": [
          "Settings"
        ],
        "summary": "Find configuration content",
        "parameters": [
          {
            "name": "name",
            "in": "path",
            "description": "Name of configuration to fetch",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {
          "200": {
            "description": "Returns dto representing configuration",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/SettingsResponse"
                }
              }
            }
          },
          "401": {
            "description": "Returns 401 if token without moderation permission has been used to access this resource",
            "content": {}
          },
          "404": {
            "description": "Returns 404 if non-existing configuration is requested",
            "content": {}
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
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {
          "200": {
            "description": "Returns 200 if configuration has been updated successfully",
            "content": {}
          },
          "401": {
            "description": "Returns 401 if token without moderation permission has been used to access this resource",
            "content": {}
          },
          "404": {
            "description": "Returns 404 if non-existing configuration is requested",
            "content": {}
          }
        },
        "deprecated": false,
        "security": []
      }
    },
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
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
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
    "/{repository}/*": {
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {
          "200": {
            "description": "Input stream of requested file",
            "content": {}
          },
          "404": {
            "description": "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository",
            "content": {}
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
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {
          "200": {
            "description": "Input stream of requested file",
            "content": {}
          },
          "401": {
            "description": "Returns 401 for invalid credentials",
            "content": {}
          },
          "507": {
            "description": "Returns 507 if Reposilite does not have enough disk space to store the uploaded file",
            "content": {}
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
            "name": "repository",
            "in": "path",
            "description": "Destination repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {
          "200": {
            "description": "Input stream of requested file",
            "content": {}
          },
          "401": {
            "description": "Returns 401 for invalid credentials",
            "content": {}
          },
          "507": {
            "description": "Returns 507 if Reposilite does not have enough disk space to store the uploaded file",
            "content": {}
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/latest/version/{repository}/*": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "extension",
            "in": "query",
            "description": "Changes extension of matched file (by default matches \u0027jar\u0027)",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "type",
            "in": "query",
            "description": "Format of expected response type: empty (default) for json; \u0027raw\u0027 for plain text",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {
          "200": {
            "description": "",
            "content": {
              "text/plain": {
                "schema": {
                  "type": "string",
                  "format": ""
                }
              }
            }
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/latest/details/{repository}/*": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "extension",
            "in": "query",
            "description": "Changes extension of matched file (by default matches \u0027jar\u0027)",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
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
    "/api/maven/latest/file/{repository}/*": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "extension",
            "in": "query",
            "description": "Changes extension of matched file (by default matches \u0027jar\u0027)",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/badge/latest/{repository}/{gav}": {
      "get": {
        "tags": [
          "badge"
        ],
        "parameters": [
          {
            "name": "repository",
            "in": "path",
            "description": "Artifact\u0027s repository",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "gav",
            "in": "path",
            "description": "Artifacts\u0027 GAV",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {},
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/details/{repository}/*": {
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
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
            "description": "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository",
            "content": {}
          }
        },
        "deprecated": false,
        "security": []
      }
    },
    "/api/maven/versions/{repository}/*": {
      "get": {
        "tags": [
          "Maven"
        ],
        "parameters": [
          {
            "name": "filter",
            "in": "query",
            "description": "Version (prefix) filter to apply",
            "required": false,
            "deprecated": false,
            "allowEmptyValue": false,
            "schema": {
              "type": "string",
              "format": ""
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
              "type": "string",
              "format": ""
            }
          },
          {
            "name": "*",
            "in": "path",
            "description": "Artifact path qualifier",
            "required": true,
            "deprecated": false,
            "allowEmptyValue": true,
            "schema": {
              "type": "string",
              "format": ""
            }
          }
        ],
        "requestBody": {
          "content": {},
          "required": false
        },
        "responses": {},
        "deprecated": false,
        "security": []
      }
    }
  },
  "components": {
    "schemas": {
      "ExecutionResponse": {
        "type": "object",
        "properties": {
          "status": {
            "$ref": "#/components/schemas/CommandStatus"
          },
          "response": {
            "type": "array",
            "items": {
              "type": "string",
              "format": ""
            }
          }
        }
      },
      "ErrorResponse": {
        "type": "object",
        "properties": {
          "status": {
            "type": "integer",
            "format": "int32"
          },
          "message": {
            "type": "string",
            "format": ""
          }
        }
      },
      "ResolvedCountResponse": {
        "type": "object",
        "properties": {
          "sum": {
            "type": "number",
            "format": "int64"
          },
          "requests": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/ResolvedEntry"
            }
          }
        }
      },
      "CreateAccessTokenWithNoNameRequest": {
        "type": "object",
        "properties": {
          "type": {
            "$ref": "#/components/schemas/AccessTokenType"
          },
          "secret": {
            "type": "string",
            "format": ""
          },
          "permissions": {
            "type": "array",
            "items": {
              "type": "string",
              "format": ""
            }
          }
        }
      },
      "SettingsResponse": {
        "type": "object",
        "properties": {
          "type": {
            "$ref": "#/components/schemas/ContentType"
          },
          "content": {
            "type": "string",
            "format": ""
          }
        }
      },
      "SessionDetails": {
        "type": "object",
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
        }
      },
      "LatestVersionResponse": {
        "type": "object",
        "properties": {
          "snapshot": {
            "type": "boolean",
            "format": ""
          },
          "version": {
            "type": "string",
            "format": ""
          }
        }
      },
      "FileDetails": {
        "type": "object",
        "properties": {
          "type": {
            "$ref": "#/components/schemas/FileType"
          },
          "name": {
            "type": "string",
            "format": ""
          }
        }
      },
      "CommandStatus": {
        "type": "object",
        "properties": {}
      },
      "ResolvedEntry": {
        "type": "object",
        "properties": {
          "gav": {
            "type": "string",
            "format": ""
          },
          "count": {
            "type": "number",
            "format": "int64"
          }
        }
      },
      "AccessTokenType": {
        "type": "object",
        "properties": {}
      },
      "ContentType": {
        "type": "object",
        "properties": {
          "mimeType": {
            "type": "string",
            "format": ""
          },
          "humanReadable": {
            "type": "boolean",
            "format": ""
          },
          "extensions": {
            "type": "array",
            "items": {
              "type": "string",
              "format": ""
            }
          },
          "contentType": {
            "$ref": "#/components/schemas/ContentType"
          },
          "contentTypeByExtension": {
            "$ref": "#/components/schemas/ContentType"
          },
          "mimeTypeByExtension": {
            "type": "string",
            "format": ""
          }
        }
      },
      "AccessTokenDto": {
        "type": "object",
        "properties": {
          "identifier": {
            "$ref": "#/components/schemas/AccessTokenIdentifier"
          },
          "name": {
            "type": "string",
            "format": ""
          },
          "createdAt": {
            "type": "string",
            "format": "date"
          },
          "description": {
            "type": "string",
            "format": ""
          }
        }
      },
      "AccessTokenPermission": {
        "type": "object",
        "properties": {
          "identifier": {
            "type": "string",
            "format": ""
          },
          "shortcut": {
            "type": "string",
            "format": ""
          }
        }
      },
      "Route": {
        "type": "object",
        "properties": {
          "path": {
            "type": "string",
            "format": ""
          },
          "permission": {
            "$ref": "#/components/schemas/RoutePermission"
          }
        }
      },
      "FileType": {
        "type": "object",
        "properties": {}
      },
      "AccessTokenIdentifier": {
        "type": "object",
        "properties": {
          "type": {
            "$ref": "#/components/schemas/AccessTokenType"
          },
          "value": {
            "type": "integer",
            "format": "int32"
          }
        }
      },
      "RoutePermission": {
        "type": "object",
        "properties": {
          "identifier": {
            "type": "string",
            "format": ""
          },
          "shortcut": {
            "type": "string",
            "format": ""
          }
        }
      }
    }
  }
}
```