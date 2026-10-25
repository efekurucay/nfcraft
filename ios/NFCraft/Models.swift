import Foundation

/// A decoded NDEF record, mirroring the common families the app can read/write.
enum NDEFRecordModel: Identifiable {
    case uri(String)
    case text(String, lang: String)
    case mime(type: String, data: Data)
    case external(type: String, data: Data)
    case empty
    case other(description: String)

    var id: String { summary }

    var summary: String {
        switch self {
        case .uri(let u): return "URI · \(u)"
        case .text(let t, let lang): return "Text (\(lang)) · \(t)"
        case .mime(let type, let data): return "MIME \(type) · \(data.count) bytes"
        case .external(let type, let data): return "External \(type) · \(data.count) bytes"
        case .empty: return "Empty"
        case .other(let d): return d
        }
    }
}

/// Result of a successful read: tag type, identifier, and any NDEF content.
struct ScannedTag {
    let typeName: String
    let identifier: String
    let records: [NDEFRecordModel]
    let ndefWritable: Bool
    let capacity: Int
}

/// What the Write screen is composing.
enum DraftRecord: Identifiable {
    case uri(String)
    case text(String)

    var id: UUID { UUID() }
}
