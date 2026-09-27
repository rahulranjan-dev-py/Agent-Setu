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

   ```bash
   mkdir -p ~/AgentSetuKeys && cd ~/AgentSetuKeys
   keytool -genkeypair -v -storetype PKCS12 \
     -keystore agentsetu-release.jks \
     -alias agentsetu -keyalg RSA -keysize 4096 -validity 10000
   chmod 600 agentsetu-release.jks
   ```

   Enter a long password twice and **write it on paper**. Your own name is enough for the name
   questions; press Enter for the rest and type `yes`. It can take a minute on a slow phone.
4. Note the fingerprint (pin it in the WhatsApp group; users compare it in *Settings → About*):

   ```bash
   keytool -list -v -keystore ~/AgentSetuKeys/agentsetu-release.jks -alias agentsetu | grep SHA256
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
3. **Unzip and sign** (apksigner asks for the key password; never put it in a script):

   ```bash
   cd ~/storage/shared/Download
   unzip -o AgentSetu-release-unsigned.zip
   apksigner sign --ks ~/AgentSetuKeys/agentsetu-release.jks --ks-key-alias agentsetu \
     --out AgentSetu-vX.Y.Z.apk app-release-unsigned.apk
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

