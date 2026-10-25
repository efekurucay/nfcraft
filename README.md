<p align="center">
  <img src="android/docs/banner.svg" alt="NFCraft" width="560">
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-2E5BFF.svg" alt="MIT License"></a>
  <img src="https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-3B6BFF.svg" alt="Android">
  <img src="https://img.shields.io/badge/iOS-SwiftUI%20%2B%20CoreNFC-4285F4.svg" alt="iOS">
</p>

<p align="center">
  A modern, native <b>NFC toolkit</b> — one monorepo, two native apps.
</p>

---

## Repository layout

| Path | What |
|---|---|
| [`android/`](android/) | The full Android app (Kotlin + Jetpack Compose). Read, write, 220-task automation, Type 4 HCE emulation, dump/password/lock, profiles, raw APDU, widget, shortcuts. |
| [`ios/`](ios/) | The iOS app (SwiftUI + CoreNFC). Read & write NDEF and tag info — the subset iOS permits. |

Each app is independently buildable from its own folder. Start with the one you need:

- **Android** → [`android/README.md`](android/README.md)
- **iOS** → [`ios/README.md`](ios/README.md)

## Platform support

iOS deliberately exposes far less of the NFC stack than Android, so the apps are not feature-equal — this is an Apple platform limitation, not a design choice.

| Capability | Android | iOS |
|---|:---:|:---:|
| Read tag + chip identification | ✅ | ✅ (tag type + UID) |
| Read / write NDEF | ✅ | ✅ |
| Originality-signature verification | ✅ | ⚠️ limited |
| Raw APDU to any technology | ✅ | ❌ |
| MIFARE Classic Crypto1 / dump | ✅ | ❌ |
| Password / lock | ✅ | ⚠️ partial |
| Automation tasks (220) + run on scan | ✅ | ❌ |
| Tag emulation (HCE / Type 4) | ✅ | ❌ (not allowed for general apps) |

> **iOS NFC needs a paid Apple Developer account.** The CoreNFC entitlement
> (`com.apple.developer.nfc.readersession.formats`) is only granted to paid
> memberships, so a free-signed sideload build cannot use NFC at all. See
> [`ios/README.md`](ios/README.md) for installation and entitlement notes.

## License

[MIT](LICENSE) © Yahya Efe Kuruçay
