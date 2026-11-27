# Publishing on Google Play

What is in this repo, and the steps only the account owner can do.

## In the repo

| What | Where |
| --- | --- |
| Signed App Bundle (`.aab`) | GitHub Actions, job `release`, artifact `play-bundle` (main branch, once the secrets below exist) |
| Privacy policy | [`PRIVACY.md`](../PRIVACY.md), URL `https://github.com/vanilds/vanilds-android/blob/main/PRIVACY.md` |
| Store texts (pt-BR, en-US) | `store/listing/<lang>/` (title ≤ 30, short ≤ 80, full ≤ 4000 characters) |
| App icon 512×512, feature graphic 1024×500 | `store/graphics/` (rendered by `tools/store_graphics.py`) |
| Phone screenshots | Not here: take 2–8 from the emulator (portrait, e.g. 1080×1920) |

`versionCode` = the GitHub Actions run number, so every build can be uploaded.
The application id `io.github.vanilds.app` can never change after the first upload.

## 1. Upload key (once, on the Mac)

Play App Signing keeps the real app signing key at Google; this key only proves uploads
come from you, and Google can reset it if it is lost.

```sh
keytool -genkeypair -v -keystore upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Vani Santos"
```

It asks for a password (choose one, at least 6 characters). Save `upload.jks` and the password
in your password manager. **Never commit `upload.jks`** (`.gitignore` already blocks `*.jks`).

Then in GitHub → `vanilds-android` → Settings → Secrets and variables → Actions → New repository secret:

| Name | Value |
| --- | --- |
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | output of `base64 -i upload.jks \| pbcopy` (it is now in the clipboard) |
| `ANDROID_UPLOAD_STORE_PASSWORD` | the password you chose |

Re-run the latest `Android` workflow on main (Actions → Android → Run workflow). The `release`
job uploads the artifact `play-bundle` with `app-release.aab`.

## 2. Play Console account

1. https://play.google.com/console/signup → **Personal** account, US$ 25 once.
2. Identity verification (official ID) and a verified phone; it can take a few days.
3. Personal accounts created after Nov 2023 must run a **closed test with at least 12 testers
   opted in for 14 days in a row** before they can apply for production.

## 3. Create the app

Create app → name `Vani Santos – Portfólio`, default language Portuguese (Brazil), App, Free.

App content (Policy → App content):

- **Privacy policy**: URL above.
- **Ads**: no ads.
- **App access**: some functionality is restricted → explain the demo login: shared demo users
  are listed on the demo page; the demo runs weekdays 08:00–20:00 BRT (reviewers outside that
  window see "Demo offline").
- **Content rating**: questionnaire, category "Reference, News, or Educational"; answer No to all.
- **Target audience**: 18+.
- **Data safety** (suggested, review it): the app itself collects nothing. Data typed into the
  demo (fictional) is sent to the demo server: declare *App activity → Other actions* and
  *Files and docs* as **collected, not shared, optional, for App functionality**, encrypted in
  transit, and users can ask for deletion (issues link). No location, contacts, financial or
  health data. No data shared with third parties.
- **Government app / financial features / health**: No. (CreditDesk is a fictional demo; it
  offers no real financial service.)

Store listing: paste the texts from `store/listing`, upload the icon, the feature graphic and the
screenshots; category **Books & Reference** (or Productivity); contact e-mail is required and is
shown publicly.

## 4. Closed test, then production

1. Testing → Closed testing → create track → upload `app-release.aab` (accept Play App Signing).
2. Testers: an e-mail list (Google accounts) of at least 12 people; share the opt-in link.
3. Wait 14 days with ≥ 12 testers opted in, then Dashboard → **Apply for production** and answer
   the questions about the test.
4. Production → create release → same bundle (or a newer one) → review (usually a few days).

## Review risks

- Google Play rejects apps that are "just a website" when they add nothing (Webview spam /
  minimum functionality). Points in favour: you own the site; it is a real app experience
  (demo with login, file upload, offline screen, back navigation). If rejected, the usual fix is
  adding native value (e.g. offline resume, push notifications).
