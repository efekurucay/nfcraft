# NFCraft — iOS

The iOS companion app (SwiftUI + CoreNFC). It covers the subset of NFC that
Apple exposes to third-party apps: **reading tag type + UID, and reading /
writing NDEF**. Raw transceive, MIFARE Classic authentication, tag emulation
(HCE) and the automation task engine are **not available on iOS** — those live
in the [Android app](../android/).

- **SwiftUI**, iOS 16+.
- **CoreNFC** via `NFCTagReaderSession` (tag identity + NDEF) and NDEF write.
- No third-party dependencies.

## Build

The Xcode project is generated from `project.yml` with
[XcodeGen](https://github.com/yonaskolb/XcodeGen) (keeps the repo free of a
churning `.xcodeproj`):

```bash
brew install xcodegen      # once
cd ios
xcodegen generate          # creates NFCraft.xcodeproj
open NFCraft.xcodeproj      # then build & run from Xcode
```

Compile-check from the command line (no device needed):

```bash
xcodebuild -project NFCraft.xcodeproj -scheme NFCraft \
  -sdk iphonesimulator -configuration Debug \
  CODE_SIGNING_ALLOWED=NO -destination 'generic/platform=iOS Simulator' build
```

> NFC does not work in the simulator. To exercise it you need a real iPhone
> (iPhone 7 or newer).

## Installing without the App Store — and the NFC catch

You *can* sideload an iOS app (AltStore / SideStore with a free Apple ID,
Xcode-to-device, an Ad Hoc build with a paid account, or TestFlight). **But NFC
has a hard requirement:** the CoreNFC entitlement
`com.apple.developer.nfc.readersession.formats` is only granted to a **paid
Apple Developer account ($99/yr)**. A build signed with a free Apple ID will
install, but every NFC session fails immediately — there is no way around this.

So in practice: to actually use NFCraft on iOS you need a paid developer
account to sign it (then run it on your own device, distribute Ad Hoc to
registered devices, or ship via TestFlight / the App Store).

## Scope on iOS

| Capability | iOS |
|---|:---:|
| Read tag type + UID (MIFARE / ISO 7816 / ISO 15693 / FeliCa) | ✅ |
| Read / write NDEF | ✅ |
| Raw APDU, Crypto1, dump, HCE, tasks | ❌ (platform-restricted) |
