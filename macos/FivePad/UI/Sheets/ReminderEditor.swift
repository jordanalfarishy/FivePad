import SwiftUI

extension Recurrence {
    var label: String {
        switch self {
        case .none: "None"
        case .daily: "Daily"
        case .weekly: "Weekly"
        case .monthly: "Monthly"
        }
    }
}

/// Tanggal, jam, dan pengulangan sekaligus — langkah tersendiri di dalam
/// `TaskEditorSheet`, bukan lembar berbeda, dan perubahannya tetap lokal
/// sampai Simpan ditekan. Padanan `ReminderEditor` di Android, dibangun di
/// atas `DatePicker` bawaan macOS alih-alih meniru kuirk UTC-tengah-malam
/// `DatePickerState` milik Compose Material3 — `DatePicker` SwiftUI sudah
/// mengembalikan `Date` setempat langsung.
struct ReminderEditor: View {
    @Environment(\.fivePad) private var colors
    let initialDue: Int64?
    let initialRecurrence: Recurrence
    let onCancel: () -> Void
    let onConfirm: (Int64, Recurrence) -> Void
    var onRemove: (() -> Void)?

    @State private var date: Date
    @State private var recurrence: Recurrence

    init(
        initialDue: Int64?,
        initialRecurrence: Recurrence,
        onCancel: @escaping () -> Void,
        onConfirm: @escaping (Int64, Recurrence) -> Void,
        onRemove: (() -> Void)? = nil,
    ) {
        self.initialDue = initialDue
        self.initialRecurrence = initialRecurrence
        self.onCancel = onCancel
        self.onConfirm = onConfirm
        self.onRemove = onRemove
        let fallback = Calendar.current.date(byAdding: .hour, value: 1, to: Date()) ?? Date()
        _date = State(initialValue: initialDue.map { Date(millis: $0) } ?? fallback)
        _recurrence = State(initialValue: initialRecurrence)
    }

    var body: some View {
        VStack(spacing: 0) {
            // Batal dan Simpan tetap terjangkau meski kalendernya perlu digulir.
            HStack {
                Button("Cancel", action: onCancel)
                    .buttonStyle(.plain)
                    .foregroundStyle(colors.muted)
                Spacer()
                Text("Reminder")
                    .fivePadStyle(FivePadText.headerName)
                    .foregroundStyle(colors.ink)
                Spacer()
                Button("Save") { onConfirm(date.millis, recurrence) }
                    .buttonStyle(.borderedProminent)
                    .tint(colors.accent)
            }
            .padding(Tokens.space3)

            Rectangle().fill(colors.hairline).frame(height: 1)

            ScrollView {
                VStack(alignment: .leading, spacing: Tokens.space4) {
                    DatePicker("", selection: $date, displayedComponents: .date)
                        .datePickerStyle(.graphical)
                        .labelsHidden()
                        .tint(colors.accent)

                    HStack {
                        Text("Time").fivePadStyle(FivePadText.message).foregroundStyle(colors.ink)
                        Spacer()
                        DatePicker("", selection: $date, displayedComponents: .hourAndMinute)
                            .labelsHidden()
                    }
                    .padding(Tokens.space3)
                    .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))

                    VStack(alignment: .leading, spacing: Tokens.space2) {
                        Text("Repeat")
                            .fivePadStyle(FivePadText.button)
                            .foregroundStyle(colors.ink)
                        HStack(spacing: Tokens.space2) {
                            ForEach(Recurrence.allCases, id: \.self) { option in
                                RecurrenceChip(option: option, selected: recurrence == option, accent: colors.accent) {
                                    recurrence = option
                                }
                            }
                        }
                        if recurrence != .none {
                            Text("Repeats on this day and time, counted from today.")
                                .fivePadStyle(FivePadText.description)
                                .foregroundStyle(colors.muted)
                        }
                    }
                }
                .padding(Tokens.space4)
            }

            if let onRemove {
                Rectangle().fill(colors.hairline).frame(height: 1)
                Button("Remove Reminder", action: onRemove)
                    .buttonStyle(.plain)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, Tokens.space3)
            }
        }
        .frame(width: 360, height: 540)
        .background(colors.background)
    }
}

private struct RecurrenceChip: View {
    @Environment(\.fivePad) private var colors
    let option: Recurrence
    let selected: Bool
    let accent: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(option.label)
                .fivePadStyle(FivePadText.description)
                .foregroundStyle(selected ? Color.white : colors.ink)
                .padding(.horizontal, Tokens.space3)
                .padding(.vertical, Tokens.space2)
                .background(
                    RoundedRectangle(cornerRadius: Tokens.radiusPill)
                        .fill(selected ? accent : accent.opacity(0.12)),
                )
        }
        .buttonStyle(.plain)
    }
}
