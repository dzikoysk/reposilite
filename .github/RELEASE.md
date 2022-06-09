# Release Guide
Reposilite 3.x uses [axios-release-plugin](https://github.com/allegro/axion-release-plugin) to simplify release process.
The general order of commands required to mark sources as next-version:

```bash
# Check current version
$ gradle cV
# Release current sources as <version>
$ gradle release -Prelease.version=<version> -Prelease.customKeyPassword=<passwd>
# Push stable artifact to the Maven repository
$ gradle publish
# Check current version
$ gradle cV
# Push changes + tags
git push --tags && git push
```

It is recommended to perform release on a standalone branch to avoid complications in case of any error during the release process.

### Notes

#### Signed commits 
In case of any issues with signing, we can disable this in `.gitconfig` by changing `gpgsign` to false:

```.git
[commit]
gpgsign = false
```

#### GitHub Authentication
Reposilite uses `localMode` of `axios-release-plugin`, so there is no need to configure GitHub authentication.