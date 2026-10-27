# Sietch privacy and security

Sietch is a file manager for GrapheneOS. A file manager needs wide access to be useful, so this is the full list of what it can see, where anything is kept, and what leaves the phone (nothing about your files does).

## Permissions

- **Photos, videos and audio** (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, and `READ_MEDIA_VISUAL_USER_SELECTED` so Android 14+ can offer partial access): read access to the media library, for the Images, Videos and Audio categories, Recents and Search. Asked in context, with Android's own prompt.
- **All files access** (`MANAGE_EXTERNAL_STORAGE`): every folder on internal storage. This is the broadest permission on the phone and the one to think about. It is **off until you switch it on**, through Android's own settings screen, and you can turn it off there at any time. Sietch works in a reduced way without it and tells you what is missing.
- The shared update library adds `INTERNET` (only to ask `https://api.github.com/repos/everettpenne/Sietch/releases` whether a newer release exists) and `REQUEST_INSTALL_PACKAGES` (so it can hand a downloaded APK to the system installer; you still confirm the install yourself).

There is no contacts, location, phone, camera, microphone or account access.

## What it can see

With All files access: every file and folder on internal storage that Android lets an app with that permission see (not other apps' private data in `Android/data` or `Android/obb`). It looks only when you open a screen. Categories, Recents and Search read the system's media index, so Sietch does not scan the disk. File contents are read only to show, copy or hand over a file you chose.

## Where data lives

- **Recycle bin:** deleted items are moved into `Android/data/com.everettpenne.sietch/files/.recycle` on the same storage, with a small index in the app's private storage. Uninstalling Sietch removes them, so empty or restore the bin first if you care about what is in it. Items are removed after your chosen retention period (default 30 days).
- **Settings:** SharedPreferences, private to the app.
- `allowBackup` and data extraction are off, so none of this is copied off the device by a backup or device transfer.
- **Sharing a file** (Share or Open with) hands one file to the app you pick as a one-off, read-only content address. Nothing is shared by default.

## What leaves the phone

- The update check above, and nothing else. No analytics, no crash reporting, no account, no server. Sietch never sends a file name, a path, or any content anywhere.

## Upgrading from 0.5

Versions before 1.0 kept a list of folders granted through Android's folder picker. On first launch 1.0 releases those grants and deletes that list, so the system does not keep access nothing in the app can show.

## Deleting

Deleting moves to the recycle bin by default (asked first). With the bin switched off, or when you choose "Delete forever" or "Empty", removal is permanent and cannot be undone.

## Builds

Releases are built and signed locally and published on this repository's Releases page. The only APKs to trust are the ones there, verified against the published SHA-256 and the signing certificate.
