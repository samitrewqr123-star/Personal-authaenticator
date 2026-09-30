# Personal Authenticator

A simple personal-use TOTP authenticator app.

Features:
- 6-digit TOTP codes
- 30-second refresh
- Manual Base32 secret entry
- QR scan for `otpauth://totp/...` codes
- Copy code
- Delete account
- Offline code generation

## Build on GitHub (no AndroidIDE required)
1. Create a GitHub repository.
2. Upload all files/folders from this project.
3. Open **Actions** and run **Build APK**.
4. When it finishes, open the workflow run and download the `PersonalAuthenticator-debug-apk` artifact.

The included workflow installs Android SDK packages and uses Gradle 8.7 + JDK 17.
