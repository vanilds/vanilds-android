# vanilds-android

Android app for [vanilds.github.io](https://vanilds.github.io): Vani Santos' portfolio and the
CreditDesk demo, shown in a native `WebView`. The site itself lives in
[vanilds/vanilds.github.io](https://github.com/vanilds/vanilds.github.io); this repo has no web code.

## How it works

- `MainActivity` opens `https://vanilds.github.io/` in a full-screen `WebView`.
- Two hosts stay inside the app: `vanilds.github.io` (portfolio, shell) and
  `creditdesk.duckdns.org` (micro frontends, API, Keycloak). The Keycloak login
  (OIDC code + PKCE) is a normal navigation inside the WebView, and the shell keeps its
  tokens in `sessionStorage` (DOM storage on). Token renewal uses refresh tokens, not a
  hidden iframe, so third-party cookie rules do not break it.
- Every other link (LinkedIn, GitHub, S3 presigned downloads) opens in the phone's browser.
- `<input type="file">` (the demo's documents panel) uses the system file picker.
- HTTPS only (`network_security_config.xml`), no file access from the WebView.
- Back button goes back in the WebView history; a page-load failure shows a "Sem conexão"
  screen with a retry button. The demo cluster being off at night is handled by the site's
  own "Demo offline" page.

## Build

No Android Studio needed: GitHub Actions builds on every push (`.github/workflows/android.yml`)
and attaches the APKs to the run (artifact `apk`). Download it, copy `app-debug.apk` to the
phone and install it (allow "install unknown apps").

Locally (JDK 17 + Android SDK, e.g. in a Codespace):

```sh
./gradlew assembleDebug
```

## Before publishing on Google Play

- Create an upload key and sign the release build with it (today `release` uses the debug key).
- Google Play needs an Android App Bundle: `./gradlew bundleRelease`.
- Replace the placeholder icon (`app/src/main/res/drawable/ic_launcher_foreground.xml`).

## Known limits

- `window.print()` does nothing in an Android WebView, so the resume's "print to PDF" button
  does not work in the app.
