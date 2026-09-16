import AppKit
import SwiftUI
import UniformTypeIdentifiers

/// Ekspor/impor gabungan lima catatan, plus jelajah cadangan harian —
/// padanan `NoteBackupsSheet` di Android.
struct NoteBackupsSheet: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    @Bindable var store: Store

    @State private var showDailyBackups = false
    @State private var pendingImportData: Data?
    @State private var pendingImportNotes: [Note] = []
    @State private var errorMessage: String?

    var body: some View {
        if showDailyBackups {
            DailyBackupsList(store: store, onBack: { showDailyBackups = false })
        } else {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("Notes Backups")
                        .fivePadStyle(FivePadText.headerName)
                        .foregroundStyle(colors.ink)
                    Spacer()
                    Button("Done") { dismiss() }
                        .buttonStyle(.plain)
                        .foregroundStyle(colors.accent)
                }
                .padding(Tokens.space4)

                VStack(spacing: Tokens.space2) {
                    SheetActionRow(icon: "square.and.arrow.up", title: "Export All", subtitle: "Save all five notes as one JSON file", action: exportAll)
                    SheetActionRow(icon: "square.and.arrow.down", title: "Import", subtitle: "Replace all five notes from a JSON file", action: importFile)
                    SheetActionRow(icon: "clock.arrow.circlepath", title: "Daily Backups", subtitle: "Browse the last seven days") {
                        showDailyBackups = true
                    }
                }
                .padding(.horizontal, Tokens.space4)

                Spacer()
            }
            .frame(width: 420, height: 340)
            .background(colors.background)
            .sheet(isPresented: Binding(get: { pendingImportData != nil }, set: { if !$0 { pendingImportData = nil } })) {
                ImportPreview(notes: pendingImportNotes) {
                    guard let data = pendingImportData else { return }
                    do {
                        try store.importNotes(data)
                    } catch {
                        errorMessage = "Couldn't import that file."
                    }
                    pendingImportData = nil
                }
            }
            .alert("Couldn't read that file", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    private func exportAll() {
        let panel = NSSavePanel()
        panel.nameFieldStringValue = "FivePad Notes.json"
        panel.allowedContentTypes = [.json]
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            let data = try store.exportNotes()
            try data.write(to: url)
        } catch {
            errorMessage = "Couldn't save the export."
        }
    }

    private func importFile() {
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.json]
        panel.allowsMultipleSelection = false
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            let data = try Data(contentsOf: url)
            pendingImportNotes = try NoteBackupCodec.decode(data)
            pendingImportData = data
        } catch {
            errorMessage = "This file isn't a valid FivePad notes backup."
        }
    }
}

private struct DailyBackupsList: View {
    @Environment(\.fivePad) private var colors
    @Bindable var store: Store
    let onBack: () -> Void

    @State private var previewingURL: URL?
    @State private var previewNotes: [Note] = []
    @State private var confirmingDeleteAll = false

    var body: some View {
        let files = store.backupList()
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Button(action: onBack) { Image(systemName: "chevron.left") }
                    .buttonStyle(.plain)
                    .foregroundStyle(colors.ink)
                Text("Daily Backups")
                    .fivePadStyle(FivePadText.headerName)
                    .foregroundStyle(colors.ink)
                Spacer()
            }
            .padding(Tokens.space4)

            if files.isEmpty {
                ContentUnavailableView("No backups yet", systemImage: "clock.arrow.circlepath")
                    .padding(.vertical, Tokens.space6)
            } else {
                ScrollView {
                    VStack(spacing: Tokens.space2) {
                        ForEach(files, id: \.self) { url in
                            Button {
                                previewNotes = (try? store.readBackup(url)) ?? []
                                previewingURL = url
                            } label: {
                                Text(url.deletingPathExtension().lastPathComponent)
                                    .fivePadStyle(FivePadText.message)
                                    .foregroundStyle(colors.ink)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(Tokens.space3)
                                    .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, Tokens.space4)
                }
                Button("Delete All Backups", role: .destructive) { confirmingDeleteAll = true }
                    .buttonStyle(.plain)
                    .foregroundStyle(.red)
                    .padding(Tokens.space4)
            }
        }
        .frame(width: 420, height: 420)
        .background(colors.background)
        .sheet(isPresented: Binding(get: { previewingURL != nil }, set: { if !$0 { previewingURL = nil } })) {
            ImportPreview(notes: previewNotes) {
                if let url = previewingURL, let data = try? Data(contentsOf: url) {
                    try? store.importNotes(data)
                }
                previewingURL = nil
            }
        }
        .alert("Delete all daily backups?", isPresented: $confirmingDeleteAll) {
            Button("Delete", role: .destructive) { store.deleteAllBackups() }
            Button("Cancel", role: .cancel) {}
        }
    }
}

/// Pratinjau kelima slot tujuan sebelum menimpa — dipakai baik oleh impor
/// JSON maupun pemulihan dari cadangan harian.
private struct ImportPreview: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    let notes: [Note]
    let onConfirm: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.space3) {
            Text("Replace all five notes?")
                .fivePadStyle(FivePadText.headerName)
                .foregroundStyle(colors.ink)
            Text("The current text in every slot will be replaced. Displaced text stays in each slot's history.")
                .fivePadStyle(FivePadText.description)
                .foregroundStyle(colors.muted)

            ScrollView {
                VStack(alignment: .leading, spacing: Tokens.space3) {
                    ForEach(notes.sorted(by: { $0.slot < $1.slot }), id: \.slot) { note in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(note.label.isEmpty ? "Note \(note.slot)" : note.label)
                                .fivePadStyle(FivePadText.message)
                                .foregroundStyle(colors.ink)
                            Text(String(note.body.prefix(80)))
                                .fivePadStyle(FivePadText.description)
                                .foregroundStyle(colors.muted)
                                .lineLimit(2)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Tokens.space3)
                        .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
                    }
                }
            }
            .frame(maxHeight: 280)

            HStack {
                Spacer()
                Button("Cancel") { dismiss() }.buttonStyle(.plain).foregroundStyle(colors.muted)
                Button("Replace") { onConfirm(); dismiss() }
                    .buttonStyle(.borderedProminent)
                    .tint(colors.accent)
            }
        }
        .padding(Tokens.space5)
        .frame(width: 420)
        .background(colors.background)
    }
}

private struct SheetActionRow: View {
    @Environment(\.fivePad) private var colors
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: Tokens.space3) {
                Image(systemName: icon)
                    .foregroundStyle(colors.muted)
                    .frame(width: 20)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).fivePadStyle(FivePadText.message).foregroundStyle(colors.ink)
                    Text(subtitle).fivePadStyle(FivePadText.description).foregroundStyle(colors.muted)
                }
                Spacer()
                Image(systemName: "chevron.right").font(.caption).foregroundStyle(colors.muted)
            }
            .padding(Tokens.space3)
            .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
        }
        .buttonStyle(.plain)
    }
}
