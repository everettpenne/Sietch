# Sietch privacy and security

Sietch is a file navigator for GrapheneOS, so its whole design is to expose as little as possible. This is the full list of what it can see, where it goes, and what leaves the phone.

## Permissions

Sietch's own manifest declares **no permissions at all** — no storage, no contacts, no location, no camera, no microphone.

The shared update library it links in adds exactly two:

- `INTERNET` — only to ask `https://api.github.com/repos/everettpenne/Sietch/releases` whether a newer release exists.
- `REQUEST_INSTALL_PACKAGES` — so the updater can hand a downloaded APK to the system installer. You must still confirm the install yourself.

That is the entire permission surface.

## What it can see

Nothing until you act. On the **Places** screen you tap "add a folder" and Android shows its own picker; Sietch receives a token for the folder you chose and its children, and nothing else. It has no way to enumerate the rest of the phone, no path to it, and no permission to ask for it.

Every file operation (list, create, rename, copy, move, delete) is a call against that token. Copying reads the source and writes the destination as ordinary streams through the token. There is no `MANAGE_EXTERNAL_STORAGE` and no `READ_MEDIA_*`/`WRITE_EXTERNAL_STORAGE` fallback.

## Where data lives

- **Folder grants**: stored as plain JSON in the app's private storage (`filesDir`), which no other app can read. `allowBackup` and data-extraction are both off, so they are not copied off the device by a backup or a device transfer.
- **Recents**: the same — a small private JSON file.
- **Settings**: SharedPreferences, private to the app.

## What leaves the phone

- The update check described above, and nothing else. No analytics, no crash reporting, no account, no server. Sietch never sends a file name, a path, or any content anywhere.

## Deleting access

Remove a folder on the Places screen and Sietch releases the token and forgets it. Your files are never touched by that action. Deleting a file or folder inside Sietch is permanent and cannot be undone, which is why it always asks first.

## Builds

Releases are built and signed in this repository's workflow and published here. The only APKs to trust are the ones on the Releases page, verified against the published SHA-256 and the signing certificate.
