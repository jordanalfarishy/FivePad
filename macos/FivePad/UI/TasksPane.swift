import AppKit
import SwiftUI

// Nilai persis dari TasksScreen.kt — bukan token bersama, karena Android
// sendiri menyimpannya sebagai konstanta privat per layar, bukan di Tokens.kt.
private let sectionPadH: CGFloat = 12
private let blockRadius: CGFloat = 12
private let rowPad: CGFloat = 13
private let rowGap: CGFloat = 10
private let sectionGap: CGFloat = 16
private let taskRowPadV: CGFloat = 12.25
private let handleSize: CGFloat = 20
private let sectionHeaderHeight: CGFloat = 42
private let emptySectionHeight: CGFloat = 26
private let footerActionHeight: CGFloat = 50
private let emptyButtonRadius: CGFloat = 35

private enum TaskSheetTarget: Identifiable {
    case newTask(groupId: String?)
    case editTask(Todo)
    case newGroup
    case renameGroup(TodoGroup)

    var id: String {
        switch self {
        case .newTask(let groupId): "new-\(groupId ?? ungroupedKey)"
        case .editTask(let todo): "edit-\(todo.id)"
        case .newGroup: "newGroup"
        case .renameGroup(let group): "rename-\(group.id)"
        }
    }
}

struct TasksPane: View {
    @Environment(\.fivePad) private var colors
    @Bindable var store: Store

    @State private var sheetTarget: TaskSheetTarget?
    @State private var hoveredRowID: String?

    var body: some View {
        ScrollView {
            LazyVStack(spacing: sectionGap) {
                ForEach(store.sections) { section in
                    sectionBlock(section)
                }
                footer
            }
            .padding(sectionPadH)
        }
        .background(colors.background)
        .overlay {
            if store.todos.isEmpty, store.groups.isEmpty {
                emptyState
            }
        }
        .overlay(alignment: .bottom) {
            if !store.clearedTodos.isEmpty {
                UndoBanner(
                    message: store.clearedTodos.count == 1 ? "Task deleted" : "\(store.clearedTodos.count) tasks cleared",
                    onUndo: store.undoClearedTodos,
                )
            }
        }
        .animation(.default, value: store.clearedTodos.isEmpty)
        .sheet(item: $sheetTarget) { target in sheetView(for: target) }
    }

    // MARK: - Bagian

    @ViewBuilder
    private func sectionBlock(_ section: TaskSection) -> some View {
        VStack(spacing: 0) {
            header(for: section)
            if section.todos.isEmpty {
                Color.clear.frame(height: emptySectionHeight)
            } else {
                VStack(spacing: 1) {
                    ForEach(Array(section.todos.enumerated()), id: \.element.id) { index, todo in
                        if index > 0 { Rectangle().fill(colors.hairline).frame(height: 1) }
                        row(todo, section: section)
                    }
                }
            }
        }
        .background(colors.row)
        .clipShape(RoundedRectangle(cornerRadius: blockRadius))
        .overlay(RoundedRectangle(cornerRadius: blockRadius).strokeBorder(colors.fieldBorder, lineWidth: 1))
        .dropDestination(for: String.self) { items, _ in
            guard let id = items.first, store.todos.contains(where: { $0.id == id }) else { return false }
            let lastPosition = section.todos.map(\.position).max()
            store.moveTodoToSection(id: id, groupId: section.group?.id, before: lastPosition, after: nil)
            return true
        }
    }

    private func header(for section: TaskSection) -> some View {
        HStack(spacing: Tokens.space2) {
            if let group = section.group {
                Menu {
                    Button("Rename") { sheetTarget = .renameGroup(group) }
                    Button("Move Up") { moveGroup(group, direction: -1) }
                    Button("Move Down") { moveGroup(group, direction: 1) }
                    Divider()
                    Button("Delete", role: .destructive) { store.deleteGroup(group.id) }
                } label: {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(colors.muted)
                        .frame(width: Tokens.space6, height: Tokens.space6)
                }
                .menuStyle(.borderlessButton)
                .frame(width: Tokens.space6)

                Text(group.name.uppercased())
                    .fivePadStyle(FivePadText.sectionTitle)
                    .foregroundStyle(colors.muted)
                    .lineLimit(1)
            }
            Spacer()
            Button { sheetTarget = .newTask(groupId: section.group?.id) } label: {
                Image(systemName: "plus")
                    .foregroundStyle(colors.accent)
                    .frame(width: Tokens.space6, height: Tokens.space6)
            }
            .buttonStyle(.plain)
            .help("Add task")
        }
        .padding(.horizontal, Tokens.space3)
        .frame(height: sectionHeaderHeight)
        .background(colors.fieldBorder)
    }

    // MARK: - Baris tugas

    private func row(_ todo: Todo, section: TaskSection) -> some View {
        HStack(spacing: rowGap) {
            Image(systemName: "line.3.horizontal")
                .font(.system(size: 12))
                .foregroundStyle(colors.dragHandle)
                .frame(width: handleSize)

            Image(systemName: todo.done ? "checkmark.circle.fill" : "circle")
                .font(.system(size: 20))
                .foregroundStyle(todo.done ? colors.accent : colors.checkboxStroke)

            VStack(alignment: .leading, spacing: 2) {
                Text(todo.text)
                    .fivePadStyle(FivePadText.body)
                    .strikethrough(todo.done)
                    .foregroundStyle(todo.done ? colors.muted : colors.ink)
                    .lineLimit(2)

                if let due = todo.dueAt {
                    let overdue = !todo.done && DueDates.isOverdue(due)
                    HStack(spacing: 4) {
                        if overdue {
                            Text("Overdue ·").foregroundStyle(.red)
                        }
                        Text(dueLabel(due: due, recurrence: todo.recurrence))
                            .foregroundStyle(colors.muted)
                    }
                    .fivePadStyle(FivePadText.description)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if hoveredRowID == todo.id {
                Button { store.deleteTodo(todo) } label: {
                    Image(systemName: "trash")
                        .foregroundStyle(.red)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, Tokens.space3)
        .padding(.vertical, taskRowPadV)
        .contentShape(Rectangle())
        .onTapGesture(count: 2) { sheetTarget = .editTask(todo) }
        .onTapGesture(count: 1) { store.toggleTodo(todo) }
        .onHover { hovering in hoveredRowID = hovering ? todo.id : (hoveredRowID == todo.id ? nil : hoveredRowID) }
        .contextMenu {
            Button("Edit") { sheetTarget = .editTask(todo) }
            Button("Delete", role: .destructive) { store.deleteTodo(todo) }
        }
        .draggable(todo.id)
        .dropDestination(for: String.self) { items, _ in
            guard let id = items.first, id != todo.id, store.todos.contains(where: { $0.id == id }) else { return false }
            let index = section.todos.firstIndex(where: { $0.id == todo.id }) ?? 0
            let before = index > 0 ? section.todos[index - 1].position : nil
            store.moveTodoToSection(id: id, groupId: section.group?.id, before: before, after: todo.position)
            return true
        }
    }

    private func dueLabel(due: Int64, recurrence: Recurrence) -> String {
        let formatted = DueDates.format(due)
        return recurrence == .none ? formatted : "\(formatted) · \(recurrence.label)"
    }

    // MARK: - Kaki daftar

    private var footer: some View {
        VStack(spacing: 0) {
            if !(store.todos.isEmpty && store.groups.isEmpty) {
                Button { sheetTarget = .newGroup } label: {
                    HStack(spacing: Tokens.space2) {
                        Image(systemName: "folder.badge.plus")
                        Text("New Group")
                    }
                    .fivePadStyle(FivePadText.button)
                    .foregroundStyle(colors.muted)
                    .frame(maxWidth: .infinity)
                    .frame(height: footerActionHeight)
                }
                .buttonStyle(.plain)

                if store.doneCount > 0 {
                    Button { store.clearCompleted() } label: {
                        HStack(spacing: Tokens.space2) {
                            Image(systemName: "trash")
                            Text("Clear completed (\(store.doneCount))")
                        }
                        .fivePadStyle(FivePadText.button)
                        .foregroundStyle(colors.muted)
                        .frame(maxWidth: .infinity)
                        .frame(height: footerActionHeight)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Layar kosong

    private var emptyState: some View {
        VStack(spacing: Tokens.space4) {
            Text("No tasks yet")
                .fivePadStyle(FivePadText.message)
                .foregroundStyle(colors.muted)
            Button { sheetTarget = .newTask(groupId: nil) } label: {
                HStack(spacing: Tokens.space2) {
                    Image(systemName: "plus")
                    Text("New Task")
                }
                .fivePadStyle(FivePadText.button)
                .foregroundStyle(.white)
                .padding(.horizontal, Tokens.space5)
                .padding(.vertical, Tokens.space3)
                .background(Capsule().fill(filledAccent))
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Lembar

    @ViewBuilder
    private func sheetView(for target: TaskSheetTarget) -> some View {
        switch target {
        case .newTask(let groupId):
            TaskEditorSheet(title: "New Task", confirmLabel: "Add", repeatable: true) { text, due, recurrence in
                store.addTodo(text: text, groupId: groupId, dueAt: due, recurrence: recurrence)
                requestReminderPermissionIfNeeded(due: due)
            }
        case .editTask(let todo):
            TaskEditorSheet(
                title: "Edit Task",
                initialText: todo.text,
                initialDue: todo.dueAt,
                initialRecurrence: todo.recurrence,
                confirmLabel: "Save",
            ) { text, due, recurrence in
                store.editTodo(todo, text: text, dueAt: due, recurrence: recurrence)
                requestReminderPermissionIfNeeded(due: due)
            }
        case .newGroup:
            TextPromptSheet(title: "New Group", placeholder: "Group name", maxLength: TodoGroup.maxNameLength, confirmLabel: "Add") { name in
                store.addGroup(name: name)
            }
        case .renameGroup(let group):
            TextPromptSheet(
                title: "Rename Group",
                placeholder: "Group name",
                initialValue: group.name,
                maxLength: TodoGroup.maxNameLength,
                confirmLabel: "Save",
            ) { name in
                store.renameGroup(group.id, name: name)
            }
        }
    }

    /// Diminta setelah tugas tersimpan, bukan saat tanggalnya dipilih (FR-2.11).
    private func requestReminderPermissionIfNeeded(due: Int64?) {
        guard due != nil else { return }
        Task { _ = await Reminders.ensureAuthorization() }
    }

    private func moveGroup(_ group: TodoGroup, direction: Int) {
        let groups = store.groups
        guard let index = groups.firstIndex(where: { $0.id == group.id }) else { return }
        let target = index + direction
        guard target >= 0, target < groups.count else { return }
        var others = groups
        others.remove(at: index)
        let insertAt = min(max(target, 0), others.count)
        others.insert(group, at: insertAt)
        guard let newIndex = others.firstIndex(where: { $0.id == group.id }) else { return }
        let before = newIndex > 0 ? others[newIndex - 1].position : nil
        let after = newIndex < others.count - 1 ? others[newIndex + 1].position : nil
        store.moveGroup(group.id, before: before, after: after)
    }
}
