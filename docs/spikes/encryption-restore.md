# Spike: encryption and restore on a real phone

Goal: prove ADR 0002 and ADR 0003 work on the PO's Samsung before sign-in is built in sprint 3. Debug builds only. Uses a fictional profile; no real birth data.

## Before you start
- Settings > Google > Backup: **Backup by Google One** is on.
- A screen lock (PIN, pattern or password) is set.

## Steps
1. Download the `app-debug` APK from the latest green CI run of this pull request (Actions > the run > Artifacts) and install it. A second icon, **Encryption check**, appears next to AstroLingo.
2. Open **Encryption check** and tap **1. Run**. Every line should show ✓. Take a screenshot.
3. Uninstall AstroLingo, reinstall the same APK, open **Encryption check** and tap **2. Restore**. "Data was created on this install" should say **no**, and "Profile decrypts with the key from Block Store" should show ✓. Take a screenshot.
4. Optional, closest to a new phone: in Settings > Google > Backup tap **Back up now**, factory-reset a spare phone or set up a second one from that backup, install the APK and tap **2. Restore**.
5. Tap **Clear** when done.
6. Send the screenshots in the project thread.

## What each line proves

| Line | Proves |
|---|---|
| Keystore key created; key is in secure hardware | The database key is wrapped by a key that can't leave the chip (StrongBox or TEE) |
| Database opens with SQLCipher | SQLCipher works on the phone with the wrapped 256-bit passphrase |
| Database file is unreadable without the key | No SQLite header and no profile text in the file on disk |
| Block Store end-to-end encryption available | Google will back up our sync key end to end; if not, the screen says what to turn on |
| Tink encrypts profile and keyset saved | The sync encryption works and the key is stored for backup |
| Profile decrypts after reinstall | A returning user gets their profile back without re-entering it |

If step 3 fails, decision 5 applies: the user re-enters their birth details. The spike then tells us how often that would happen.
