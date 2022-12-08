# Publishing new version of kotlin-fill-class

Update two files. You can use [this commit](https://github.com/suusan2go/kotlin-fill-class/commit/5f7fe9a384b63d1664be9a99760fa2b61378c172) as a reference.

- build.gradle
  - update the `version`
- src/main/resources/META-INF/plugin.xml
  - update `change-notes`

Create a PR and tag [@suusan2go](https://github.com/suusan2go) or [@oboenikui](https://github.com/oboenikui)

After merging it, publish the plugin to JetBrains by the command below.

```bash
 ./gradlew publishPlugin
```

Note that you need to set JetBrains token as an environment variable `TOKEN`. This token is only shared with maintainers of this plugin.
It would take 1-2 business days to get approval from JetBrains.

Finally, create a new release from GitHub. Create a new tag that is the same as the version you published.

