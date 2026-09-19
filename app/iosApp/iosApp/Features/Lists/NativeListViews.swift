import SwiftUI
import Foundation
import PhotosUI
import UIKit

private struct NativeListSummaryRow: View {
    let list: NativeListSummarySnapshot

    var body: some View {
        HStack {
            Image(systemName: "list.bullet.rectangle.portrait")
                .font(.title2)
                .foregroundStyle(.tint)
            VStack(alignment: .leading, spacing: 4) {
                Text(list.title).font(.headline)
                Text(AnimeL10n.string(.listSummary, String(list.itemCount), String(list.followerCount)))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct NativeListsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var showingEditor = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            if let errorMessage {
                Section { Text(errorMessage).foregroundStyle(.red) }
            }
            if model.lists.isEmpty {
                ContentUnavailableView(AnimeL10n.key(.listEmpty), systemImage: "list.bullet.rectangle", description: Text(AnimeL10n.key(.listEmptyDescription)))
            } else {
                ForEach(model.lists) { list in
                    NavigationLink {
                        NativeListDetailView(listId: list.id, model: model)
                    } label: {
                        NativeListSummaryRow(list: list)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollIndicators(.hidden)
        .navigationTitle(AnimeL10n.key(.listTitle))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showingEditor = true } label: { Image(systemName: "plus") }
            }
        }
        .task { load() }
        .refreshable { await refresh() }
        .sheet(isPresented: $showingEditor) {
            NativeListEditorView(model: model) { _, error in errorMessage = error }
        }
    }

    private func load() {
        model.loadLists { _, error in errorMessage = error }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadLists { _, error in errorMessage = error; continuation.resume() }
        }
    }
}

struct NativeListDetailView: View {
    let listId: String
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?
    @State private var isFollowing = false
    @State private var showingEditor = false
    @State private var showingDeleteConfirmation = false
    @State private var message: String?

    private var detail: NativeListDetailSnapshot? { model.listDetails[listId] }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                if let detail {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(detail.summary.title).font(.title2.weight(.bold))
                        Text(detail.summary.description.isEmpty ? AnimeL10n.string(.listDescription) : detail.summary.description)
                            .foregroundStyle(.secondary)
                        Text(AnimeL10n.string(.listCreator, detail.summary.ownerName, String(detail.summary.itemCount)))
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                        HStack {
                            Button(isFollowing ? AnimeL10n.key(.actionFollowing) : AnimeL10n.key(.listFollow)) {
                                model.followList(id: listId, following: !isFollowing) { value, error in
                                    if let value { isFollowing = value }
                                    message = error.map { AnimeL10n.string(.activityOperationFailed, $0) }
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            if let ownerId = detail.summary.ownerId {
                                NavigationLink(AnimeL10n.key(.listCreatorAction)) {
                                    NativeUserProfileView(userId: ownerId, model: model)
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    }
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                    if detail.items.isEmpty {
                        ContentUnavailableView(AnimeL10n.key(.listNoItems), systemImage: "books.vertical")
                    } else {
                        ForEach(detail.items) { item in
                            NavigationLink {
                                NativeSubjectDetailView(summary: NativeSubjectSummary.placeholder(id: item.subjectId, title: item.title), model: model)
                            } label: {
                                HStack(spacing: 12) {
                                    NativePosterImage(url: item.posterURL, width: 56, height: 75, cornerRadius: 10)
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(item.title).font(.headline).lineLimit(2)
                                        if let note = item.note, !note.isEmpty {
                                            Text(note).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                                        }
                                    }
                                    Spacer()
                                    if let score = item.score { Text(String(format: "%.1f", score)).font(.caption.weight(.semibold)) }
                                }
                                .padding(12)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    if let message { Text(message).font(.footnote).foregroundStyle(.secondary) }
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                } else {
                    ProgressView(AnimeL10n.key(.listLoading)).frame(maxWidth: .infinity, minHeight: 220)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.listTitle))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if detail?.summary.owned == true {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button(AnimeL10n.key(.actionEdit)) { showingEditor = true }
                        Button(AnimeL10n.key(.actionDelete), role: .destructive) { showingDeleteConfirmation = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .task { load() }
        .refreshable { await refresh() }
        .sheet(isPresented: $showingEditor) {
            if let detail {
                NativeListEditorView(model: model, existing: detail) { _, error in
                    errorMessage = error
                    if error == nil { load() }
                }
            }
        }
        .confirmationDialog(AnimeL10n.key(.listDeleteConfirmation), isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button(AnimeL10n.key(.actionDelete), role: .destructive) {
                model.deleteList(id: listId) { error in
                    message = error.map { AnimeL10n.string(.listDeleteFailed, $0) } ?? AnimeL10n.string(.listDeleted)
                }
            }
        }
    }

    private func load() {
        errorMessage = nil
        model.loadList(id: listId) { value, error in
            if let value { isFollowing = value.summary.following }
            errorMessage = error
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadList(id: listId) { value, error in
                if let value { isFollowing = value.summary.following }
                errorMessage = error
                continuation.resume()
            }
        }
    }
}

private struct NativeListEditorView: View {
    @ObservedObject var model: NativeAppModel
    let existing: NativeListDetailSnapshot?
    let onComplete: (NativeListSummarySnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var description: String
    @State private var visibility: String
    @State private var subjectIdsText: String
    @State private var isSaving = false

    init(model: NativeAppModel, existing: NativeListDetailSnapshot? = nil, onComplete: @escaping (NativeListSummarySnapshot?, String?) -> Void) {
        self.model = model
        self.existing = existing
        self.onComplete = onComplete
        _title = State(initialValue: existing?.summary.title ?? "")
        _description = State(initialValue: existing?.summary.description ?? "")
        _visibility = State(initialValue: "public")
        _subjectIdsText = State(initialValue: existing?.items.map { String($0.subjectId) }.joined(separator: ", ") ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField(AnimeL10n.key(.listName), text: $title)
                TextField(AnimeL10n.key(.listDescription), text: $description, axis: .vertical)
                    .lineLimit(2...5)
                Picker(AnimeL10n.key(.visibility), selection: $visibility) {
                    Text(AnimeL10n.key(.visibilityPublic)).tag("public")
                    Text(AnimeL10n.key(.visibilityPrivate)).tag("private")
                }
                Section(AnimeL10n.key(.listItems)) {
                    TextField(AnimeL10n.key(.listSubjectIds), text: $subjectIdsText, axis: .vertical)
                        .keyboardType(.numbersAndPunctuation)
                    Text(AnimeL10n.key(.listSubjectIdsDescription))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Button(isSaving ? AnimeL10n.key(.listSaving) : (existing == nil ? AnimeL10n.key(.listCreate) : AnimeL10n.key(.actionSaveChanges))) { save() }
                    .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle(existing == nil ? AnimeL10n.key(.listNew) : AnimeL10n.key(.actionEdit))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(AnimeL10n.key(.actionCancel)) { dismiss() } }
            }
        }
    }

    private func save() {
        let ids = subjectIdsText.split { $0 == "," || $0 == "，" || $0 == " " || $0 == "\n" }.compactMap { Int64($0) }
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)
        isSaving = true
        let finish: (NativeListSummarySnapshot?, String?) -> Void = { value, error in
            isSaving = false
            onComplete(value, error)
            if value != nil { dismiss() }
        }
        if let existing {
            model.updateList(id: existing.summary.id, title: cleanTitle, description: cleanDescription, visibility: visibility, subjectIds: ids, completion: finish)
        } else {
            model.createList(title: cleanTitle, description: cleanDescription, visibility: visibility, subjectIds: ids, completion: finish)
        }
    }
}

