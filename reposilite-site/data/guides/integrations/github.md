---
id: github
title: GitHub
---

### GitHub Actions

Before you can start working with [GitHub Actions](https://github.com/features/actions), 
you have to add access token to your environment.
You can do this in `Your repository -> Settings -> Security -> Actions`:

![GitHub Actions :: Secret](/images/guides/github-actions-secrets.png)

Defined variables can be used in workflow files using the given syntax: `${{ secrets.VARIABLE }}`.

#### Publication

Configure workflow task that builds your project and then deploys it to your Reposilite instance with the following setup:

```yaml
name: Publish project to Maven repository
# Publish manually
on: workflow_dispatch
# OR, publish per each commit
on:
  push:
    branches: [ main ]
jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK
        uses: actions/setup-java@v1
        with:
          java-version: 18
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      - name: Publish with Gradle
        run: ./gradlew build publish
        env:
          MAVEN_NAME: ${{ secrets.MAVEN_NAME }}
          MAVEN_TOKEN: ${{ secrets.MAVEN_TOKEN }}
```

You can find full list of available events in GitHub docs:

* [GitHub Docs - Events that trigger workflows](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows#available-events)