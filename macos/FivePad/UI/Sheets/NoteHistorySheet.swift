import SwiftUI

/// Hingga sepuluh revisi per slot selama 30 hari — ketuk untuk pratinjau
/// penuh, lalu Pulihkan atau Batal. Padanan `NoteHistorySheet` di Android.
struct NoteHistorySheet: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    @Bindable var store: Store
    let slot: Int

    @State private var previewing: NoteRevision?
    @State private var confirmingDeleteAll = false

    var body: some View {
        let revisions = store.history(slot: slot)
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("History")
                    .fivePadStyle(FivePadText.headerName)
                    .foregroundStyle(colors.ink)
                Spacer()
                Button("Done") { dismiss() }
                    .buttonStyle(.plain)
                    .foregroundStyle(colors.accent)
            }
            .padding(Tokens.space4)

            if revisions.isEmpty {
                ContentUnavailableView(
                    "No history yet",
                    systemImage: "clock",
                    description: Text("Edits are saved here as you go."),
                )
                .padding(.vertical, Tokens.space6)
            } else {
                ScrollView {
                    VStack(spacing: Tokens.space2) {
                        ForEach(revisions) { revision in
                            Button { previewing = revision } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(DueDates.format(revision.createdAt))
                                        .fivePadStyle(FivePadText.description)
                                        .foregroundStyle(colors.muted)
                                    Text(String(revision.body.prefix(100)))
                                        .fivePadStyle(FivePadText.message)
                                        .foregroundStyle(colors.ink)
                                        .lineLimit(2)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(Tokens.space3)
                                .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, Tokens.space4)
                }

                Button("Delete History", role: .destructive) { confirmingDeleteAll = true }
                    .buttonStyle(.plain)
                    .foregroundStyle(.red)
                    .padding(Tokens.space4)
            }
        }
        .frame(width: 420, height: 480)
        .background(colors.background)
        .sheet(isPresented: Binding(get: { previewing != nil }, set: { if !$0 { previewing = nil } })) {
            if let revision = previewing {
                NoteRestorePreview(revision: revision) {
                    store.restoreRevision(revision.id)
                    previewing = nil
                    dismiss()
                }
            }
        }
        .alert("Delete all history for this slot?", isPresented: $confirmingDeleteAll) {
            Button("Delete", role: .destructive) {
                store.deleteHistory(slot: slot)
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        }
    }
}

private struct NoteRestorePreview: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    let revision: NoteRevision
    let onRestore: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.space3) {
            Text(DueDates.format(revision.createdAt))
                .fivePadStyle(FivePadText.headerName)
                .foregroundStyle(colors.ink)
            ScrollView {
                Text(revision.body)
                    .fivePadStyle(FivePadText.body)
                    .foregroundStyle(colors.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(maxHeight: 320)
            HStack {
                Spacer()
                Button("Cancel") { dismiss() }.buttonStyle(.plain).foregroundStyle(colors.muted)
                Button("Restore", action: onRestore).buttonStyle(.borderedProminent).tint(colors.accent)
            }
        }
        .padding(Tokens.space5)
        .frame(width: 420)
        .background(colors.background)
    }
}
