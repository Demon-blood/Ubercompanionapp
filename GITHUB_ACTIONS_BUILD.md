# GitHub Actions APK Build

This project includes a GitHub Actions workflow at:

```text
.github/workflows/android-build.yml
```

## What it does

On every push to `main`/`master`, pull request, or manual run, GitHub Actions will:

1. Check out the repository.
2. Install JDK 17.
3. Set up the Android SDK.
4. Install Android API 35 and Build Tools 35.0.0.
5. Set up Gradle 8.10.2.
6. Run:

```bash
gradle --no-daemon --stacktrace assembleDebug
```

7. Upload the generated debug APK as a workflow artifact.

## Where to find the APK

After the workflow finishes:

1. Open your GitHub repository.
2. Go to **Actions**.
3. Open the latest **Android CI** run.
4. Scroll to **Artifacts**.
5. Download `uber-eats-companion-debug-apk`.

The APK is generated from:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Important

This project does not include a committed Gradle wrapper. The workflow uses `gradle/actions/setup-gradle` to install Gradle 8.10.2 directly on GitHub Actions.

If you later build locally on a computer, either install Gradle or generate a wrapper with:

```bash
gradle wrapper --gradle-version 8.10.2
```
