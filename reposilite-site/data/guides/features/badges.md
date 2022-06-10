---
id: badges
title: Badges
---

Reposilite supports generation of SVG badges out of the box. 
It might be especially useful for open-source developers that may want to include well-known GitHub badges in the their fancy README files.

* `/api/badge/latest/{repository}/{gav}`

Supported path parameters:

| Parameter | Example value | Description |
| :-------: | :-----------: | :---------: |
| `color` | `40c14a` | HEX color code of badge |
| `prefix` | `v` | Text included before the version matched by Reposilite |
| `filter` | `1.0-` | Reposilite will resolve only the version that starts with the given identifier |

[Example usage](https://github.com/dzikoysk/reposilite/blob/0d9237187702126cc8ec3a70b7ea6d3aecd4c263/.github/README.md?plain=1#L8) of badges endpoint:

```bash
/api/badge/latest/releases/com/reposilite?color=40c14a&name=Reposilite&prefix=v
```

Results in such badge:

* ![Badge](https://maven.reposilite.com/api/badge/latest/releases/org/panda-lang/reposilite?color=40c14a&name=Reposilite&prefix=v)