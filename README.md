# Sietch

A file manager for Android with iOS-style liquid glass, in the same minimal style as [Seekur](https://github.com/everettpenne/DuneDdit), [Spice](https://github.com/everettpenne/Spice) and [Thumper](https://github.com/everettpenne/Thumper). It is built for [GrapheneOS](https://grapheneos.org/) on the Google Pixel: a small, offline way to browse, organise and recover your files, with a recycle bin and nothing sent anywhere.

Sietch is the Fremen name for a hidden desert stronghold — a safe place to keep your things.

## What it does

- **Home:** how full the phone is, your files by category (Images, Videos, Audio, Documents, Downloads, Apps, Archives) with live counts, the usual folders, the recycle bin and the newest files.
- **Browse:** every folder on internal storage, as a list or a thumbnail grid, sorted by name, size, date or kind in either direction, with an optional hidden-file view and a tappable breadcrumb.
- **Recycle bin:** deleting moves a file aside instead of removing it. Restore it, delete it for good, or let it expire (7, 15 or 30 days, your choice). It can be switched off.
- **Player:** video and sound play in-app with the controls of a phone's own player: swipe to seek, brightness on the left and volume on the right, hold for 2×, speed, audio and subtitle tracks (and subtitle files beside the video), a preview picture while seeking, fit or fill, repeat, lock, picture-in-picture, next and previous through the folder, and it remembers where you stopped. Sound files (and videos, if you turn on "Keep playing when I leave") carry on in the background with a media notification.
- **Act on files:** open them with the right app, preview pictures, and read saved web pages in-app; rename, share, copy or move (with a folder picker), see details, make folders, and select several at once.
- **Recents and Search:** the newest files first, and a search by file name.
- **Settings:** access, sorting, the recycle bin, and an in-app update check.

## Permissions, and why

Sietch 1.0 is a full file manager, so unlike earlier versions it asks for wide access, but only when you turn it on and only for what a feature needs:

| Permission | What it unlocks | Without it |
|---|---|---|
| Photos, videos and audio (`READ_MEDIA_*`) | The Images, Videos and Audio categories, Recents, Search | Those screens say what is missing and offer the prompt |
| Notifications and a media service (`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) | The media notification while something plays in the background | Playback still carries on, with no notification |
| All files access (`MANAGE_EXTERNAL_STORAGE`) | Browsing every folder; the Documents, Downloads, Apps and Archives categories; moving, copying and deleting ordinary files | Home and the media categories still work; Browse asks for it |

Nothing else: no contacts, location, camera, microphone, accounts, analytics or tracking. See [PRIVACY.md](PRIVACY.md).

**Coming from 0.5?** Earlier versions worked only inside folders you granted through Android's folder picker, with no storage permission. 1.0 replaces that with All files access. On first launch it releases the old folder grants, so nothing keeps access the app can no longer show. If you want the old, permission-free behaviour, stay on [0.5.2](https://github.com/everettpenne/Sietch/releases/tag/v0.5.2).

## Install on your phone

Sietch is not on Google Play. Install the APK from the **[Releases page](https://github.com/everettpenne/Sietch/releases/latest)**. It needs **Android 13 or newer** (API 33).

1. On your phone, open the latest release and download **`Sietch-<version>.apk`** (and, if you want to check it, `SHA256SUMS.txt`).
2. Open the downloaded file. Android will say it can't install apps from this source: tap **Settings** and allow your browser (or the Files app) to **install unknown apps**, then go back and tap **Install**. On GrapheneOS this is the per-app "Install unknown apps" setting; turn it back off afterwards.
3. Open **Sietch** and, when it asks, allow media access and/or switch on **All files access** (the Home screen shows what is still off).

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

Requirements: JDK 21, Android SDK 36, Android 13 (API 33) or newer on the device.

Release signing comes from the environment (`SIETCH_RELEASE_STORE_FILE`, `_STORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`); without all four the release build is left unsigned.

## Layout

```
app/src/main/java/com/everettpenne/sietch/
  data/        LocalFs (file operations), TrashStore (recycle bin), MediaIndex (categories, recents, search), Access, Settings, sorting
  ui/          root shell, tab bar, routes
  ui/screens/  Home, Browse, Category, Recents, Search, Recycle bin, Settings
  ui/components/ rows, file icon, selection bar, sheets, file operations, opener, access prompt
  util/        formatting
```

## Licence

MIT. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
