import Foundation

enum NoteBackupError: Error {
    case tooLarge
    case invalidFormat
    case invalidSlots
}

/// Format portabel hanya-catatan. Tugas sengaja di luar arsip ini. Padanan
/// `NoteBackupCodec` di Android — format `fivepad-notes`, versi 1.
enum NoteBackupCodec {
    static let maxBytes = 2_000_000

    static func encode(_ notes: [Note]) throws -> Data {
        try validate(notes)
        let sorted = notes.sorted { $0.slot < $1.slot }
        let payload: [String: Any] = [
            "format": "fivepad-notes",
            "version": 1,
            "notes": sorted.map { ["slot": $0.slot, "label": $0.label, "body": $0.body] },
        ]
        return try JSONSerialization.data(withJSONObject: payload, options: [.prettyPrinted, .sortedKeys])
    }

    static func decode(_ data: Data) throws -> [Note] {
        guard data.count <= maxBytes else { throw NoteBackupError.tooLarge }
        guard
            let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
            root["format"] as? String == "fivepad-notes",
            (root["version"] as? Int) == 1,
            let array = root["notes"] as? [[String: Any]]
        else {
            throw NoteBackupError.invalidFormat
        }
        let notes: [Note] = try array.map { item in
            guard
                let slot = item["slot"] as? Int,
                let label = item["label"] as? String,
                let body = item["body"] as? String
            else {
                throw NoteBackupError.invalidFormat
            }
            return Note(slot: slot, label: label, body: body)
        }
        try validate(notes)
        return notes.sorted { $0.slot < $1.slot }
    }

    private static func validate(_ notes: [Note]) throws {
        guard notes.map(\.slot).sorted() == Array(1...Note.slotCount) else { throw NoteBackupError.invalidSlots }
        guard notes.allSatisfy({ $0.label.count <= Note.maxLabelLength && $0.body.count <= Note.maxBodyLength }) else {
            throw NoteBackupError.invalidFormat
        }
    }
}

/// Satu cadangan atomik per hari aktif, menyimpan tujuh salinan terakhir
/// secara bergilir. Padanan `NoteBackupStore` di Android.
struct NoteBackupStore {
    let directory: URL

    private static let namePattern = try! NSRegularExpression(pattern: #"^\d{4}-\d{2}-\d{2}\.json$"#)

    func list() -> [URL] {
        let files = (try? FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil)) ?? []
        return files
            .filter { url in
                let name = url.lastPathComponent
                return Self.namePattern.firstMatch(in: name, range: NSRange(name.startIndex..., in: name)) != nil
            }
            .sorted { $0.lastPathComponent > $1.lastPathComponent }
    }

    func saveDaily(_ notes: [Note]) throws {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let data = try NoteBackupCodec.encode(notes)
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = .current
        let url = directory.appendingPathComponent("\(formatter.string(from: Date())).json")
        try data.write(to: url, options: .atomic)
        for stale in list().dropFirst(7) {
            try? FileManager.default.removeItem(at: stale)
        }
    }

    func read(_ url: URL) throws -> [Note] {
        try NoteBackupCodec.decode(Data(contentsOf: url))
    }

    func deleteAll() {
        for file in list() { try? FileManager.default.removeItem(at: file) }
    }
}
