# Sietch

A file navigator for Android with iOS-style liquid glass, in the same minimal style as [Seekur](https://github.com/everettpenne/DuneDdit), [Spice](https://github.com/everettpenne/Spice) and [Thumper](https://github.com/everettpenne/Thumper). It is built for [GrapheneOS](https://grapheneos.org/) on the Google Pixel: a small, offline, permission-free way to look at and organise your files.

Sietch is the Fremen name for a hidden desert stronghold — a safe place to keep your things.

## What it does

- **Places:** the folders you grant Sietch through Android's own folder picker. It never asks for broad storage access, and it never scans your phone.
- **Browse:** list a folder, open files and folders, with folders-first sorting by name, size, date or kind, in either direction, and an optional hidden-file view.
- **Search:** find files and folders by name across every folder you have granted, at once.
- **Act on files:** open them with the right app, preview images in-app (pinch, swipe, dismiss), rename, share, copy or move to another folder, and delete — with a confirmation, because there is no recycle bin.
- **Selection:** long-press to select several, then share, copy, move or delete them together.
- **Recents:** the folders and files you opened, newest first, so the thing you were just looking at is one tap away.
- **Settings:** default sort, folders-first, hidden files, and an in-app update check.

## Privacy and security, the GrapheneOS way

Sietch is written for people who care about the surface an app exposes:

- **No storage permissions.** It declares none. It uses the Storage Access Framework: you hand it a folder in the system picker, and it can only touch that folder and its children.
- **No file-path guessing.** Every operation is a single provider call or a plain stream copy through the folder grant. There is no `MANAGE_EXTERNAL_STORAGE`, no `READ_MEDIA_*`, no access to anything you did not choose.
- **No analytics, no tracking, no accounts.** Nothing about your files ever leaves the phone.
- **Backups are off**, so the folder grants stay on this device.
- The one exception is the update check: like Seekur, Spice and Thumper, Sietch asks `github.com` for a newer release when you open it (or on demand in Settings). It sends nothing but a normal HTTPS request. If you prefer, update from the [Releases page](https://github.com/everettpenne/Sietch/releases) and ignore the check.

See [PRIVACY.md](PRIVACY.md) for the full model.

## Install on your phone

Sietch is not on Google Play. Install the APK from the **[Releases page](https://github.com/everettpenne/Sietch/releases/latest)**. It needs **Android 12 or newer** (API 31); Android 13 or newer for the full glass lens.

1. On your phone, open the latest release and download **`Sietch-<version>.apk`** (and, if you want to check it, `SHA256SUMS.txt`).
2. Open the downloaded file. Android will say it can't install apps from this source: tap **Settings** and allow your browser (or the Files app) to **install unknown apps**, then go back and tap **Install**. On GrapheneOS this is the per-app "Install unknown apps" setting; turn it back off afterwards.
3. Open **Sietch**, go to **Places**, and tap the **folder button** to grant it a folder.

To update, install a newer APK over the old one; it is signed with the same key, so your folders and settings stay.

**Check the download (optional but sensible):** the release lists the APK's SHA-256. Compare it with `sha256sum Sietch-<version>.apk`, and the signing certificate with `apksigner verify --print-certs Sietch-<version>.apk`. Only install an APK from this repository's Releases page.

**From a computer instead:** enable *Developer options › USB debugging*, then `adb install Sietch-<version>.apk`.

**Build it yourself:** see below. A debug build (`./gradlew :app:installDebug`) is signed with your own debug key, so it will not install over the release one; uninstall one before installing the other.

## Build

The liquid-glass UI kit (components, theme, type scale) is the separate `Glass` project, pulled in with a Gradle `includeBuild("../Glass")`, so it must be checked out next to this folder.

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:installDebug        # build and install on a connected device
```

Requirements: JDK 21, Android SDK 36, Android 12 (API 31) or newer on the device.

Release signing comes from the environment (`SIETCH_RELEASE_STORE_FILE`, `_STORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`); without all four the release build is left unsigned.

## Layout

```
app/src/main/java/com/everettpenne/sietch/
  data/        FileSystem (DocumentsContract), Locations, Recents, Settings, sorting
  ui/          root shell, tab bar, routes
  ui/screens/  Places, Browse, Search, Recents, Settings
  ui/components/ rows, file icon, search field, settings rows
  util/        formatting, the folder route codec
```

## Licence

MIT. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
