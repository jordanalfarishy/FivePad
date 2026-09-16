import SwiftUI

/// Nama tugas lebih dulu; jatuh tempo opsional dikonfigurasi di langkah yang
/// sama, bukan dua lembar terpisah — mengikuti pola Todoist Quick Add yang
/// dirujuk `docs/android-ui.md`. Padanan `TaskEditorSheet` di Android.
struct TaskEditorSheet: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    let title: String
    let confirmLabel: String
    /// Enter menyimpan lalu mengosongkan kolom untuk entri berikutnya (FR-2.2).
    var repeatable = false
    let onConfirm: (String, Int64?, Recurrence) -> Void

    @State private var text: String
    @State private var due: Int64?
    @State private var recurrence: Recurrence
    @State private var showReminderEditor = false
    @FocusState private var textFocused: Bool

    init(
        title: String,
        initialText: String = "",
        initialDue: Int64? = nil,
        initialRecurrence: Recurrence = .none,
        confirmLabel: String,
        repeatable: Bool = false,
        onConfirm: @escaping (String, Int64?, Recurrence) -> Void,
    ) {
        self.title = title
        self.confirmLabel = confirmLabel
        self.repeatable = repeatable
        self.onConfirm = onConfirm
        _text = State(initialValue: initialText)
        _due = State(initialValue: initialDue)
        _recurrence = State(initialValue: initialRecurrence)
    }

    var body: some View {
        if showReminderEditor {
            ReminderEditor(
                initialDue: due,
                initialRecurrence: recurrence,
                onCancel: { showReminderEditor = false },
                onConfirm: { newDue, newRecurrence in
                    due = newDue
                    recurrence = newRecurrence
                    showReminderEditor = false
                },
                onRemove: due != nil ? {
                    due = nil
                    recurrence = .none
                    showReminderEditor = false
                } : nil,
            )
        } else {
            VStack(alignment: .leading, spacing: Tokens.space3) {
                Text(title)
                    .fivePadStyle(FivePadText.headerName)
                    .foregroundStyle(colors.ink)

                TextField("Task", text: $text)
                    .textFieldStyle(.plain)
                    .focused($textFocused)
                    .onChange(of: text) { _, new in
                        if new.count > Todo.maxTextLength { text = String(new.prefix(Todo.maxTextLength)) }
                    }
                    .onSubmit(confirmAndMaybeReset)
                    .padding(Tokens.space3)
                    .background(RoundedRectangle(cornerRadius: Tokens.radiusSm).fill(colors.fieldSurface))
                    .overlay(RoundedRectangle(cornerRadius: Tokens.radiusSm).strokeBorder(colors.fieldBorder, lineWidth: 1))

                ReminderActionButton(
                    title: due.map { DueDates.format($0) } ?? "Add reminder",
                    description: (due != nil && recurrence != .none) ? recurrence.label : nil,
                ) {
                    showReminderEditor = true
                }

                HStack {
                    Spacer()
                    Button("Cancel") { dismiss() }
                        .buttonStyle(.plain)
                        .foregroundStyle(colors.muted)
                    Button(confirmLabel, action: confirmAndClose)
                        .buttonStyle(.borderedProminent)
                        .tint(colors.accent)
                        .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .padding(Tokens.space5)
            .frame(width: 380)
            .background(colors.background)
            .onAppear { if text.isEmpty { textFocused = true } }
        }
    }

    private func confirmAndMaybeReset() {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        onConfirm(trimmed, due, recurrence)
        if repeatable {
            text = ""
            due = nil
            recurrence = .none
            textFocused = true
        } else {
            dismiss()
        }
    }

    private func confirmAndClose() {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        onConfirm(trimmed, due, recurrence)
        dismiss()
    }
}
