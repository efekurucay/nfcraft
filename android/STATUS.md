# NFCraft — status, gaps, and on-device verification

## Verified on a device (2026-10-25)

Tested on an Android phone with genuine tags, end to end:

- **Chip identification** — NTAG213 (via GET_VERSION; storage byte = byte[6]) and
  MIFARE Classic 1K (via ATQA/SAK) both identified correctly.
- **Originality signature** — secp128r1 ECDSA verified on a genuine NXP NTAG213
  ("verified (genuine NXP)"), pure-Kotlin, no crypto dependency.
- **Write → read loop** — wrote a URI record to a factory-blank NTAG213 (raw CC +
  NDEF TLV) and read it back decoded; `WRITE` ACK handling works on-device.
- **Dump** — full page hex of the NTAG213, confirming the written CC/TLV bytes.
- No crashes across the flow.

Still device-dependent: ICODE public key / SLI-vs-SLIX split, DESFire dump on
EV2, password/lock irreversibility, and HCE against an external reader.

---


The project compiles and builds a debug APK. Everything below is implemented
against typed contracts but, where noted, cannot be verified without NFC
hardware and real tags. Static analysis cannot observe tag execution.

## Contract state

`NfcIo` exposes `atqa`, `sak`, `historicalBytes` (added for chip ID). No other
contract changes were needed across the five engine modules. Known contract
limits, not yet addressed:

- **No Crypto1 auth path.** MIFARE Classic sector auth is a controller call
  (`MifareClassic.authenticateSectorWithKeyA/B`), not raw transceive, so it is
  absent from `NfcIo`. Classic `format`/`makeReadOnly`/`setPassword` are
  therefore unimplemented (return `NOT_WRITABLE`), and Classic `dump`/`writeDump`
  only cover sectors the controller already left open. A `MifareClassic`-aware
  `NfcIo` extension is the fix.
- `NdefRecordModel.Wifi.hidden` has no standard WSC attribute → never written,
  always decodes `false`. `Geo` (lat/lon) and `Bluetooth` (mac/name) drop richer
  payload fields.
- `TaskDescriptor` has no permission metadata, no structured condition markers,
  no enum/range param bounds; the task wire format cannot escape `|`.
- `DangerousTaskGuard.confirm` receives only the descriptor, not the args.

## Needs a device / real tag to confirm

**Chip identification (`core-chip`):**
- GET_VERSION storage-size byte index: code keys product-type = byte[2],
  storage = byte[6] (NXP datasheet). The task note said byte[4]; the wiki lists
  bytes [1][2][4][6]. byte[4] is the major version. Confirm against a real
  GET_VERSION dump; adjust `STORAGE_BYTE` in `DefaultChipIdentifier` if needed.
- ICODE public key bytes and SLI-vs-SLIX split heuristic — confirm against a
  genuine ICODE SLIX2 tag.
- ISO15693 UID byte order (Android may return it reversed).
- NTAG413 vs 424 DNA subtype split is best-effort; generic DESFire is solid.

**Writing / protection (`core-tag`):**
- `makeReadOnly` and `setPassword` are **irreversible on hardware** (lock bits
  are one-way; a wrong password can soft-lock writes).
- WRITE ACK semantics: `0xA2` page write may return a 4-bit ACK or an empty
  frame depending on the stack; `expectAck` handles both but wants on-device
  confirmation.
- DESFire dump is best-effort; EV2 may reject plain reads.
- NTAG215 CC size byte uses datasheet `0x3E` (not arithmetic `0x3F`).

**Signature (`core-chip`):** secp128r1 ECDSA verify is pure Kotlin
(`BigInteger`), no external crypto dependency. NTAG key is well-established;
re-check ICODE key on a reference tag.

**Tasks (`feature-tasks`):** 71 of 220 descriptors are catalog-only (no executor
yet). Permission-gated behavior (brightness, DND/ringer, Bluetooth/WiFi API
limits by SDK, flashlight, vibration), intent-handler availability, background
launch limits, and root shell execution are all device/permission-dependent.

**HCE (`feature-hce`):** end-to-end APDU exchange with an external reader,
MLe/MLc negotiation, multi-chunk reads — HCE-capable hardware only. Emulation is
read-only (no WRITE/UPDATE BINARY) by design.

## Security notes

- Root and other dangerous tasks (category w666) run through `RootShell`
  (`su` → `/system/xbin/su` → `/system/bin/su`) but are gated behind
  `DangerousTaskGuard`, which **denies by default**. No privileged command runs
  until a real guard (confirmation dialog + whitelist) is wired and the user
  approves. This mirrors the original suite's consent model.
- The HTTP task family applies a private-IP SSRF filter (10/8, 172.16–31,
  192.168/16, 127/8, 169.254/16, localhost).
