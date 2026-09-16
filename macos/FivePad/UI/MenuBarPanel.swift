import SwiftUI

/// Konten panel cepat. Bingkai, posisi, resize, klik-di-luar, dan pintasan
/// global dimiliki `MenuBarController`; isi tetap SwiftUI agar memakai Store
/// dan komponen visual yang sama dengan jendela utama.
struct MenuBarPanel: View {
    @Environment(\.fivePad) private var colors
    @AppStorage("selectedSlot") private var slot = 1
    @Bindable var store: Store
    let openMainWindow: () -> Void
    let closePanel: () -> Void

    @State private var tab = Tab.notes
    @State private var quickTask = ""
    @FocusState private var quickTaskFocused: Bool

    private enum Tab { case notes, tasks }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            SlotStripe(slot: slot, colour: colors.slotAccents[slot - 1])
            quickTaskBar

            Group {
                if tab == .notes {
                    NotesPane(store: store, slot: slot, autofocus: true)
                } else {
                    TasksPane(store: store, showsComposer: false)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            bottomBar
        }
        .frame(minWidth: 360, minHeight: 420)
        .background(colors.background)
        .onAppear { slot = min(max(slot, 1), Note.slotCount) }
        .onExitCommand {
            store.saveDraft(slot: slot)
            closePanel()
        }
    }

    private var topBar: some View {
        HStack(spacing: Tokens.space3) {
            SlotDots(active: slot, onSelect: selectSlot)
            Spacer(minLength: Tokens.space2)
            Button(action: openMainWindow) {
                Image(systemName: "macwindow")
            }
            .buttonStyle(.plain)
            .help("Open FivePad window")
        }
        .padding(.horizontal, Tokens.space4)
        .frame(height: Tokens.topBarHeight)
        .background(colors.bar)
    }

    private var quickTaskBar: some View {
        HStack(spacing: Tokens.space2) {
            Image(systemName: "plus.circle.fill")
                .foregroundStyle(colors.accent)
            TextField("Quick task", text: $quickTask)
                .textFieldStyle(.plain)
                .focused($quickTaskFocused)
                .onSubmit(addQuickTask)
            Text("↩")
                .fivePadStyle(FivePadText.meta)
                .foregroundStyle(colors.muted)
        }
        .padding(.horizontal, Tokens.space3)
        .frame(height: 40)
        .background(colors.bar)
        .overlay(alignment: .bottom) {
            Rectangle().fill(colors.hairline).frame(height: 1)
        }
    }

    private var bottomBar: some View {
        HStack(spacing: Tokens.space2) {
            panelTab(.notes, title: "Notes", symbol: "note.text", accent: colors.slotAccents[slot - 1])
            panelTab(.tasks, title: "Tasks", symbol: "checklist", accent: colors.accent)
            Spacer()
            Text("\(store.doneCount)/\(store.totalCount)")
                .fivePadStyle(FivePadText.meta)
                .monospacedDigit()
                .foregroundStyle(colors.muted)
        }
        .padding(.horizontal, Tokens.space3)
        .frame(height: 48)
        .background(colors.bar)
        .overlay(alignment: .top) {
            Rectangle().fill(colors.hairline).frame(height: 1)
        }
    }

    private func panelTab(_ target: Tab, title: String, symbol: String, accent: Color) -> some View {
        let selected = tab == target
        return Button {
            tab = target
            quickTaskFocused = false
        } label: {
            Label(title, systemImage: symbol)
                .fivePadStyle(FivePadText.tab)
                .foregroundStyle(selected ? accent : colors.muted)
                .padding(.horizontal, Tokens.space3)
                .frame(height: 30)
                .background(
                    RoundedRectangle(cornerRadius: Tokens.radiusPill)
                        .fill(.clear),
                )
        }
        .buttonStyle(.plain)
    }

    private func selectSlot(_ newSlot: Int) {
        store.saveDraft(slot: slot)
        slot = newSlot
        tab = .notes
        quickTaskFocused = false
    }

    private func addQuickTask() {
        guard !quickTask.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        store.addTodo(text: quickTask)
        quickTask = ""
        quickTaskFocused = true
    }
}
