`test.jks` signs **test builds only** (the `android-test` release), so each new test build installs over the last one and keeps your data.
It is intentionally public. The Google Play release must use a separate, private upload key kept in GitHub Secrets.
