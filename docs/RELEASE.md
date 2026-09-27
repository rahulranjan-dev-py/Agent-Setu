# Release checklist

How to publish a version of Agent Setu (package `in.agentsetu.app`) as a signed APK through GitHub
Releases and the WhatsApp group. Follow every step, every time. Only the owner publishes releases.

> Do all of this on **your own computer or your own phone**, never an office machine (roadmap §7.1).
> The signing key and its passwords must **never** go into this repository, GitHub, WhatsApp, email
> or any cloud chat. This repository is public.
>
> Two ways to release, same key either way:
> - **Computer:** build and sign locally (sections 1–3).
> - **Phone with Termux:** GitHub builds the unsigned release; you sign it on the phone (section 6).

## 1. One time only: create the signing key

Android only lets a new version replace an old one if both are signed with the **same key**. Lose
the key and users must uninstall (and lose data unless backed up) to move to a new one.

1. Make a folder outside the repository, e.g. `~/AgentSetuKeys/` (Windows: `C:\AgentSetuKeys\`).
2. Create the key (JDK `keytool`; Android Studio's bundled JDK has it):

   ```bash
   keytool -genkeypair -v -storetype PKCS12 \
     -keystore ~/AgentSetuKeys/agentsetu-release.jks \
     -alias agentsetu -keyalg RSA -keysize 4096 -validity 10000
   ```

   Use a long password; the name/organisation questions can be your own name. 10,000 days is about
   27 years.
3. Note the key's SHA-256 fingerprint:

   ```bash
   keytool -list -v -keystore ~/AgentSetuKeys/agentsetu-release.jks -alias agentsetu | grep SHA256
   ```

   This is the value users compare on *Settings → About*. Pin it in the WhatsApp group description.
4. **Back it up twice, offline:** copy the `.jks` file to a pen drive and to a second encrypted
   copy (e.g. a password-protected zip on another drive). Write the passwords on paper and keep
   them with the pen drive. Test once that the backup copy opens (`keytool -list` on it).
5. Register as a verified developer with Google's Android Developer Console before sideloading
   enforcement reaches India (planned 2027), using this key (roadmap §7.3).

## 2. One time per computer: tell Gradle where the key is

Create `keystore.properties` in the repository root (it is git-ignored; check with `git status`
that it never shows up):

```properties
storeFile=/home/you/AgentSetuKeys/agentsetu-release.jks
storePassword=...
keyAlias=agentsetu
keyPassword=...
```

On Windows use forward slashes: `storeFile=C:/AgentSetuKeys/agentsetu-release.jks`.

## 3. Every release

| # | Step | How |
|---|---|---|
| 1 | Bump the version | In `app/build.gradle.kts`: `versionCode` +1 (whole number, always increasing) and `versionName` (e.g. `1.0.0` → `1.0.1`). |
| 2 | Refresh sample rates if needed | If a new quarter's interest rates or a new incentive order came out, update `data/seed/*.sample.json` (new `updatedAt`) so new installs get them. |
| 3 | Check CI is green | The GitHub Actions run for the commit must pass (tests, debug APK, release build check). |
| 4 | Build the signed APK | `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (minified; aim under 15 MB). |
| 5 | Name, checksum, verify | `./scripts/prepare-release.sh` → `release/out/AgentSetu-vX.Y.Z.apk`, its SHA-256, and the signing certificate. **The certificate must match the pinned fingerprint.** Windows without Git Bash: `Get-FileHash .\AgentSetu-vX.Y.Z.apk -Algorithm SHA256`. |
| 6 | Test the update on two phones | Install the new APK **over the previous version** on at least two phones (one Xiaomi/Vivo/Oppo if possible). Data must survive; PIN, reminders and backup must still work. See the pilot checklist for a full pass. |
| 7 | Publish on GitHub | Create a Release with tag `vX.Y.Z`, attach `AgentSetu-vX.Y.Z.apk`, paste the SHA-256 and the Hindi + English changelog. This is the master copy. |
| 8 | Update `release/version.json` | `latestVersionCode`, `latestVersionName`, `sha256`, `releasedOn`, both changelogs; `downloadUrl` = the Releases page. Raise `minSupportedVersionCode` only to push everyone off a version with a serious bug. Commit to `main`: the in-app update check reads it from there. |
| 9 | Post in WhatsApp | In the admin-only announcement group: the APK file, version, SHA-256, short Hindi changelog and the disclaimer (template below). |
| 10 | Keep the old APK | Keep the previous release attached on GitHub, in case someone needs to reinstall it. |

### Creating the GitHub Release (in the browser, works on a phone)

1. Write the notes first: copy `release/notes/v1.0.0.md` to `release/notes/vX.Y.Z.md`, update it and
   `CHANGELOG.md`, and commit to `main`.
2. Open **github.com/rahulranjan-dev-py/Agent-Setu/releases** → **Draft a new release** (on a phone,
   Chrome in "Desktop site" mode is easier).
3. **Choose a tag** → type `vX.Y.Z` → *Create new tag on publish*; target `main`.
4. **Release title:** `Agent Setu X.Y.Z` (for the pilot: `Agent Setu 1.0.0 (pilot)`).
5. **Description:** paste the part of `release/notes/vX.Y.Z.md` below the line, with the file's
   SHA-256 and the signing-key fingerprint filled in.
6. **Attach binaries:** upload the signed `AgentSetu-vX.Y.Z.apk`. Never attach the unsigned or debug
   APK, and never the keystore.
7. For the pilot, tick **Set as a pre-release**; for general releases, **Set as the latest release**.
8. **Publish release.** Then update `release/version.json` on `main` (step 8 above) so the app tells
   users about it.

## 4. WhatsApp release post (template)

```
📲 एजेंट सेतु (Agent Setu) — नया वर्शन X.Y.Z

क्या नया है:
• ...
• ...

फ़ाइल: AgentSetu-vX.Y.Z.apk
SHA-256: <checksum>
डाउनलोड: <GitHub Releases link>

⚠️ केवल इसी ग्रुप या ऊपर दिए GitHub लिंक से इंस्टॉल करें। किसी और से मिली APK न लें।
ऐप में सेटिंग्स → ऐप के बारे में जाकर "साइनिंग की फ़िंगरप्रिंट" ग्रुप विवरण में पिन की गई फ़िंगरप्रिंट से मिलाएँ।
अपडेट से पहले सेटिंग्स → बैकअप और रीस्टोर से बैकअप ले लें।

एजेंट सेतु एक स्वतंत्र, अनौपचारिक ऐप है - यह इंडिया पोस्ट / डाक विभाग / IPPB का उत्पाद नहीं है।
Agent Setu is an independent, unofficial app - not an India Post / DoP / IPPB product.
```

## 5. If something goes wrong

| Problem | What to do |
|---|---|
| A release has a serious bug | Publish a fixed version, then set `minSupportedVersionCode` in `version.json` to the fixed `versionCode`; the app shows "Please update" to everyone below it. |
| "App not installed" when updating | The APK was signed with a different key, or `versionCode` did not go up. Never sign with a different key. |
| Signing key lost | No more updates are possible for existing installs. Users must back up, uninstall and install a new build signed with a new key (and restore). This is why step 1.4 matters. |
| Someone circulates a modified APK | Remind the group: official source only, check the fingerprint. Report it in the group. |

## 6. Releasing from a phone with Termux

The Android build tools do not run in Termux, so GitHub builds the app and the phone only holds the
key and signs. The key never leaves the phone except as your offline backups.

### 6.1 One time: set up Termux and create the key

1. Install **Termux from F-Droid or its GitHub releases page**, not the Play Store (that version is
   outdated and its packages fail).
2. Install Java (for `keytool`) and the signing tool:

   ```bash
   pkg update && pkg upgrade
   pkg install openjdk-17 apksigner unzip
   ```

3. Create the key inside Termux's private folder, which other apps cannot read:

   Termux hides passwords while you type them (nothing appears, not even `*`). That is normal. If
   you prefer to see the password, write it in the command itself, in single quotes, as below.
   Replace `MyLongPassword123` with your own password (letters and digits are safest; avoid `'` and
   `\`), and your name after `CN=`:

   ```bash
   mkdir -p ~/AgentSetuKeys && cd ~/AgentSetuKeys
   keytool -genkeypair -v -storetype PKCS12 \
     -keystore agentsetu-release.jks \
     -alias agentsetu -keyalg RSA -keysize 4096 -validity 10000 \
     -storepass 'MyLongPassword123' -keypass 'MyLongPassword123' \
     -dname "CN=Your Name, O=Agent Setu, C=IN"
   chmod 600 agentsetu-release.jks
   history -c && rm -f ~/.bash_history
   ```

   With this key type (PKCS12) the key password is always the same as the store password. **Write
   the password on paper.** The last line wipes Termux's command history, which would otherwise keep
   the password. It can take a minute on a slow phone.
4. Note the fingerprint (pin it in the WhatsApp group; users compare it in *Settings → About*):

   ```bash
   keytool -list -v -keystore ~/AgentSetuKeys/agentsetu-release.jks -alias agentsetu \
     -storepass 'MyLongPassword123' | grep SHA256
   history -c && rm -f ~/.bash_history
   ```

5. **Back it up twice, offline:**

   ```bash
   termux-setup-storage      # allow storage access, once
   cp ~/AgentSetuKeys/agentsetu-release.jks ~/storage/shared/Download/
   ```

   With the Files app, move the file from *Download* to a **pen drive (OTG)**, and make a second copy
   on another pen drive or a home computer. **Then delete it from Download**: any app with storage
   access can read that folder. Keep the password paper with the pen drive, not on the phone.

   If the phone is lost, reset, or Termux is uninstalled, `~/AgentSetuKeys` is gone. Without these
   backups, no update can ever be released for existing users.

### 6.2 Every release

1. **Bump the version** on GitHub: open `app/build.gradle.kts` in the browser, tap the pencil,
   raise `versionCode` by 1 and set `versionName`, then *Commit changes*. This starts a build.
   (To rebuild without changes: *Actions → Build → Run workflow*, or *Re-run jobs* on an old run.)
2. **Wait for the green tick** on *Actions → Build*, open the run, and download the artifact
   **AgentSetu-release-unsigned** (use Chrome, logged in; the GitHub app cannot download
   artifacts). It is kept for 30 days.
3. **Unzip and sign.** The password is written in the command (`pass:` in front of it) so you can
   see it; the last line clears the history again. Never save these commands in a script file.

   ```bash
   cd ~/storage/shared/Download
   unzip -o AgentSetu-release-unsigned.zip
   apksigner sign --ks ~/AgentSetuKeys/agentsetu-release.jks --ks-key-alias agentsetu \
     --ks-pass 'pass:MyLongPassword123' --key-pass 'pass:MyLongPassword123' \
     --out AgentSetu-vX.Y.Z.apk app-release-unsigned.apk
   history -c && rm -f ~/.bash_history
   ```

4. **Check the signature and get the checksum:**

   ```bash
   apksigner verify --print-certs AgentSetu-vX.Y.Z.apk | grep -i sha-256
   sha256sum AgentSetu-vX.Y.Z.apk
   ```

   The certificate SHA-256 must match the fingerprint from 6.1 step 4. The `sha256sum` value goes
   in the GitHub Release, `version.json` and the WhatsApp post.
5. **Clean up** the unsigned files: `rm app-release-unsigned.apk AgentSetu-release-unsigned.zip`.
6. Continue with section 3 from step 6 (test the update on two phones, GitHub Release, update
   `release/version.json` on `main`, WhatsApp post).

The unsigned artifact is safe to leave on GitHub: Android refuses to install an unsigned APK, and
only your key can produce a copy with your fingerprint.

## 7. Signing on GitHub with secrets (optional)

The owner may store the release key as **encrypted GitHub secrets**. The build then signs the
release itself, and you download `AgentSetu-vX.Y.Z.apk` ready to publish, with nothing to sign on
the phone.

**Trade-off.** It is convenient, but anyone who gets into your GitHub account (or anyone you give
write access to the repository) could change the build to copy the key or sign a fake version.
Before you do this:

- turn on **two-factor authentication** on your GitHub account (*Settings → Password and
  authentication*);
- give nobody else write access to this repository;
- still keep the two offline backups from 6.1: GitHub secrets cannot be read back, so they are not a
  backup.

What the build does with the secrets: only for builds of `main` and the *Run workflow* button (never
pull requests), it writes the key to a temporary file on GitHub's machine, builds and signs, deletes
the file, and uploads **AgentSetu-release-signed** (the APK and its `.sha256` file). The run's
summary page shows the checksum and the certificate fingerprint. GitHub hides secret values in logs.
Without the secrets, builds stay exactly as before (unsigned release).

### 7.1 One time: put the key into GitHub

1. Create the key in Termux as in 6.1 (steps 1–5, including both offline backups).
2. Turn the key file into text (base64) and put it where you can copy it:

   ```bash
   termux-setup-storage     # once, if not done yet
   base64 -w0 ~/AgentSetuKeys/agentsetu-release.jks > ~/storage/shared/Download/key-base64.txt
   ```

   Open `key-base64.txt` with a text editor app, select all and copy. (With the **Termux:API** app
   and `pkg install termux-api` you can copy directly instead:
   `base64 -w0 ~/AgentSetuKeys/agentsetu-release.jks | termux-clipboard-set`.)
3. In Chrome, open **github.com/rahulranjan-dev-py/Agent-Setu → Settings → Secrets and variables →
   Actions → New repository secret** and add four secrets, each exactly as named:

   | Name | Value |
   |---|---|
   | `RELEASE_KEYSTORE_BASE64` | the whole text you copied in step 2 |
   | `RELEASE_KEYSTORE_PASSWORD` | your key password |
   | `RELEASE_KEY_ALIAS` | `agentsetu` |
   | `RELEASE_KEY_PASSWORD` | the same password again |

4. **Delete the text copy at once** — it is the key in another form:

   ```bash
   rm ~/storage/shared/Download/key-base64.txt
   ```

   If you copied it through the clipboard, copy some other text afterwards so the key is not left in
   the clipboard.

### 7.2 Every release

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts` on `main` (section 3, step 1).
   Committing to `main` starts a signed build; or use *Actions → Build → Run workflow* on `main`.
2. When the run is green, open it: the **summary** shows the SHA-256 checksum and the certificate
   fingerprint. The fingerprint must match the one pinned in the WhatsApp group.
3. Download **AgentSetu-release-signed**, unzip it: `AgentSetu-vX.Y.Z.apk` and its `.sha256`.
4. Optional double check in Termux:
   `apksigner verify --print-certs AgentSetu-vX.Y.Z.apk | grep -i sha-256`.
5. Continue with section 3 from step 6 (test on two phones, GitHub Release, `version.json`,
   WhatsApp post).

### 7.3 To stop signing on GitHub

Delete the four secrets (*Settings → Secrets and variables → Actions*). Builds go back to producing
the unsigned release for signing in Termux.

## 8. Test builds and the test key

Test builds (`AgentSetu-debug-apk`, app id `in.agentsetu.app.debug`) are signed with a fixed test key
that is committed to this public repository (`app/debug.keystore`), so each test build installs over
the previous one without losing data. Because anyone can use that key, test builds are **for testing
only**: install them only from this repository's Actions page and never keep real customer data in
them. The test key cannot sign or update the real app.

