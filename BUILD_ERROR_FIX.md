# Fix for `android.useAndroidX` GitHub Actions build error

The build error:

```text
Configuration `:app:debugRuntimeClasspath` contains AndroidX dependencies, but the `android.useAndroidX` property is not enabled.
```

means the GitHub runner did not see `android.useAndroidX=true` in the root `gradle.properties` file.

This project now fixes it in two places:

1. Root `gradle.properties` includes:

```properties
android.useAndroidX=true
android.enableJetifier=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

2. `.github/workflows/android-build.yml` has a defensive step named `Ensure AndroidX Gradle properties`, which creates/appends the required properties before Gradle runs.

Commit both files to GitHub, then rerun **Actions → Android CI**.
