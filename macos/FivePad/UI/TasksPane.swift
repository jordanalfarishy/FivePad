import SwiftUI

struct TasksPane: View {
    @Environment(\.fivePad) private var colors
    @Bindable var store: Store
    var showsComposer = true
    @State private var newTask = ""
    @State private var newGroup = ""
    @State private var showsNewGroup = false
    @State private var selectedGroupId: String?

    private var ungrouped: [Todo] { store.todos.filter { $0.groupId == nil } }

    var body: some View {
        VStack(spacing: 0) {
            if showsComposer { header }
            ScrollView {
                LazyVStack(spacing: Tokens.space3) {
                    if !ungrouped.isEmpty {
                        taskBlock(title: nil, todos: ungrouped)
                    }
                    ForEach(store.groups) { group in
                        taskBlock(title: group.name, todos: store.todos.filter { $0.groupId == group.id })
                    }

                    if store.todos.isEmpty && store.groups.isEmpty {
                        ContentUnavailableView(
                            "No tasks yet",
                            systemImage: "checkmark.circle",
                            description: Text("Add a task and keep the list intentionally short."),
                        )
                        .foregroundStyle(colors.muted)
                        .padding(.top, 64)
                    }
                }
                .padding(Tokens.space3)
            }
            .background(colors.background)
        }
    }

    private var header: some View {
        VStack(spacing: Tokens.space2) {
            HStack(spacing: Tokens.space2) {
                TextField("Add a task", text: $newTask)
                    .textFieldStyle(.plain)
                    .onSubmit(addTask)
                Menu {
                    Button("No group") { selectedGroupId = nil }
                    ForEach(store.groups) { group in
                        Button(group.name) { selectedGroupId = group.id }
                    }
                } label: {
                    Image(systemName: "folder")
                        .foregroundStyle(colors.muted)
                }
                .menuStyle(.borderlessButton)
                .frame(width: 24)
                .help(selectedGroupName)
                Button(action: addTask) {
                    Image(systemName: "plus.circle.fill")
                        .foregroundStyle(colors.accent)
                }
                .buttonStyle(.plain)
                .disabled(newTask.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(.horizontal, Tokens.space3)
            .frame(height: 38)
            .background(RoundedRectangle(cornerRadius: Tokens.radiusSm).fill(colors.row))

            HStack {
                if showsNewGroup {
                    TextField("Group name", text: $newGroup)
                        .textFieldStyle(.plain)
                        .onSubmit(addGroup)
                    Button("Add", action: addGroup).buttonStyle(.borderless)
                    Button("Cancel") {
                        newGroup = ""
                        showsNewGroup = false
                    }
                    .buttonStyle(.borderless)
                } else {
                    Button("New Group") { showsNewGroup = true }
                        .buttonStyle(.borderless)
                        .foregroundStyle(colors.muted)
                    Spacer()
                    if store.doneCount > 0 {
                        Button("Clear completed") { store.clearCompleted() }
                            .buttonStyle(.borderless)
                            .foregroundStyle(colors.muted)
                    }
                }
            }
            .font(.caption)
        }
        .padding(Tokens.space3)
        .background(colors.bar)
    }

    private func taskBlock(title: String?, todos: [Todo]) -> some View {
        VStack(spacing: Tokens.itemGap) {
            if let title {
                HStack {
                    Text(title)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(colors.muted)
                    Spacer()
                }
                .padding(.horizontal, Tokens.rowPad)
                .padding(.vertical, Tokens.space2)
            }

            ForEach(todos) { todo in
                HStack(spacing: Tokens.rowGap) {
                    Button { store.toggleTodo(todo) } label: {
                        Image(systemName: todo.done ? "checkmark.circle.fill" : "circle")
                            .font(.system(size: 17))
                            .foregroundStyle(todo.done ? colors.accent : colors.checkboxStroke)
                    }
                    .buttonStyle(.plain)

                    Text(todo.text)
                        .strikethrough(todo.done)
                        .foregroundStyle(todo.done ? colors.muted : colors.ink)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button { store.deleteTodo(todo) } label: {
                        Image(systemName: "trash")
                            .foregroundStyle(colors.muted)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Delete \(todo.text)")
                }
                .padding(Tokens.rowPad)
                .background(RoundedRectangle(cornerRadius: Tokens.rowRadius).fill(colors.row))
            }
        }
        .padding(Tokens.sectionPadV)
        .background(RoundedRectangle(cornerRadius: Tokens.blockRadius).fill(colors.bar))
    }

    private func addTask() {
        store.addTodo(text: newTask, groupId: selectedGroupId)
        newTask = ""
    }

    private var selectedGroupName: String {
        store.groups.first { $0.id == selectedGroupId }?.name ?? "No group"
    }

    private func addGroup() {
        store.addGroup(name: newGroup)
        newGroup = ""
        showsNewGroup = false
    }
}
