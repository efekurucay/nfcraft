import Foundation
import CoreNFC

/// Drives CoreNFC tag sessions for reading and writing NDEF. iOS exposes only
/// NDEF + basic tag identity (type + UID); there is no raw transceive, no HCE,
/// and no MIFARE Classic authentication available to third-party apps.
final class NFCService: NSObject, ObservableObject {
    enum Phase: Equatable {
        case idle
        case scanning
        case done(String)
        case error(String)
    }

    @Published var phase: Phase = .idle
    @Published var lastTag: ScannedTag?

    private var session: NFCTagReaderSession?
    private enum Mode { case read, write([NDEFRecordModel]) }
    private var mode: Mode = .read

    var isAvailable: Bool { NFCTagReaderSession.readingAvailable }

    func read() {
        start(mode: .read, prompt: "Hold your iPhone near a tag to read it.")
    }

    func write(_ records: [NDEFRecordModel]) {
        guard !records.isEmpty else { return }
        start(mode: .write(records), prompt: "Hold your iPhone near a tag to write it.")
    }

    private func start(mode: Mode, prompt: String) {
        guard isAvailable else {
            publish(.error("NFC is not available on this device."))
            return
        }
        self.mode = mode
        DispatchQueue.main.async { self.lastTag = nil; self.phase = .scanning }
        session = NFCTagReaderSession(pollingOption: [.iso14443, .iso15693, .iso18092], delegate: self, queue: nil)
        session?.alertMessage = prompt
        session?.begin()
    }

    private func publish(_ phase: Phase) {
        DispatchQueue.main.async { self.phase = phase }
    }
}

extension NFCService: NFCTagReaderSessionDelegate {
    func tagReaderSessionDidBecomeActive(_ session: NFCTagReaderSession) {}

    func tagReaderSession(_ session: NFCTagReaderSession, didInvalidateWithError error: Error) {
        let nfcError = error as? NFCReaderError
        if nfcError?.code == .readerSessionInvalidationErrorUserCanceled {
            publish(.idle)
        } else {
            publish(.error(error.localizedDescription))
        }
    }

    func tagReaderSession(_ session: NFCTagReaderSession, didDetect tags: [NFCTag]) {
        guard let tag = tags.first else { return }
        session.connect(to: tag) { [weak self] error in
            guard let self else { return }
            if let error {
                session.invalidate(errorMessage: error.localizedDescription)
                self.publish(.error(error.localizedDescription))
                return
            }
            switch self.mode {
            case .read: self.handleRead(session, tag)
            case .write(let records): self.handleWrite(session, tag, records)
            }
        }
    }

    private func handleRead(_ session: NFCTagReaderSession, _ tag: NFCTag) {
        guard let (typeName, id, ndefTag) = Self.describe(tag) else {
            session.invalidate(errorMessage: "Unsupported tag type.")
            return
        }
        ndefTag.queryNDEFStatus { [weak self] status, capacity, _ in
            guard let self else { return }
            let writable = status == .readWrite
            ndefTag.readNDEF { message, _ in
                let records = (message?.records ?? []).map(Self.decode)
                let scanned = ScannedTag(
                    typeName: typeName,
                    identifier: Self.hex(id),
                    records: records,
                    ndefWritable: writable,
                    capacity: capacity
                )
                session.alertMessage = "Read \(typeName)."
                session.invalidate()
                DispatchQueue.main.async {
                    self.lastTag = scanned
                    self.phase = .done("Read \(typeName)")
                }
            }
        }
    }

    private func handleWrite(_ session: NFCTagReaderSession, _ tag: NFCTag, _ records: [NDEFRecordModel]) {
        guard let (_, _, ndefTag) = Self.describe(tag) else {
            session.invalidate(errorMessage: "Unsupported tag type.")
            return
        }
        ndefTag.queryNDEFStatus { [weak self] status, _, _ in
            guard let self else { return }
            guard status == .readWrite else {
                session.invalidate(errorMessage: "This tag is not writable.")
                self.publish(.error("Tag is not writable."))
                return
            }
            guard let message = Self.message(from: records) else {
                session.invalidate(errorMessage: "Nothing to write.")
                return
            }
            ndefTag.writeNDEF(message) { error in
                if let error {
                    session.invalidate(errorMessage: error.localizedDescription)
                    self.publish(.error(error.localizedDescription))
                } else {
                    session.alertMessage = "Tag written."
                    session.invalidate()
                    self.publish(.done("Wrote \(records.count) record(s)"))
                }
            }
        }
    }

    // MARK: - Mapping

    private static func describe(_ tag: NFCTag) -> (String, Data, NFCNDEFTag)? {
        switch tag {
        case .miFare(let t):
            let family: String
            switch t.mifareFamily {
            case .ultralight: family = "MIFARE Ultralight / NTAG"
            case .plus: family = "MIFARE Plus"
            case .desfire: family = "MIFARE DESFire"
            case .unknown: family = "MIFARE Classic"
            @unknown default: family = "MIFARE"
            }
            return (family, t.identifier, t)
        case .iso7816(let t):
            return ("ISO 7816 (Type 4 / DESFire)", t.identifier, t)
        case .iso15693(let t):
            return ("ISO 15693 (ICODE)", t.identifier, t)
        case .feliCa(let t):
            return ("FeliCa", t.currentIDm, t)
        @unknown default:
            return nil
        }
    }

    private static func decode(_ payload: NFCNDEFPayload) -> NDEFRecordModel {
        switch payload.typeNameFormat {
        case .nfcWellKnown:
            if let url = payload.wellKnownTypeURIPayload() {
                return .uri(url.absoluteString)
            }
            let text = payload.wellKnownTypeTextPayload()
            if let value = text.0 {
                return .text(value, lang: text.1?.identifier ?? "en")
            }
            return .other(description: "Well-known record")
        case .media:
            return .mime(type: String(data: payload.type, encoding: .utf8) ?? "application/octet-stream", data: payload.payload)
        case .absoluteURI:
            return .uri(String(data: payload.payload, encoding: .utf8) ?? "")
        case .nfcExternal:
            return .external(type: String(data: payload.type, encoding: .utf8) ?? "", data: payload.payload)
        case .empty:
            return .empty
        default:
            return .other(description: "Unknown record")
        }
    }

    private static func message(from records: [NDEFRecordModel]) -> NFCNDEFMessage? {
        let payloads: [NFCNDEFPayload] = records.compactMap { record in
            switch record {
            case .uri(let value):
                guard let url = URL(string: value) else { return nil }
                return NFCNDEFPayload.wellKnownTypeURIPayload(url: url)
            case .text(let value, let lang):
                return NFCNDEFPayload.wellKnownTypeTextPayload(string: value, locale: Locale(identifier: lang))
            case .mime(let type, let data):
                return NFCNDEFPayload(format: .media, type: Data(type.utf8), identifier: Data(), payload: data)
            default:
                return nil
            }
        }
        guard !payloads.isEmpty else { return nil }
        return NFCNDEFMessage(records: payloads)
    }

    private static func hex(_ data: Data) -> String {
        data.map { String(format: "%02X", $0) }.joined(separator: ":")
    }
}
