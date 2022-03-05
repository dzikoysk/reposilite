---
id: remote-execution
title: Remote execution
sidebar_label: Remote execution
---

Reposilite allows you to execute commands through the API under the **POST** `/api/execute` route. 
To authenticate the request, you need to pass alias and token with manager permission as a credentials for a [basic method](https://en.wikipedia.org/wiki/Basic_access_authentication). The command to execute should be provided as a request content _(body)_.

If you configured your request properly, you should receive JSON response:

```json
{
  succeeded: true/false
  response: [
    "Multiline response"
  ]
}
```

The `succeeded` property determines the status of executed command.
For instance, in case of missing parameter for a command, 
Reposilite returns `false` and usage message.