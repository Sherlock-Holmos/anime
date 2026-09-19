import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeSubjectCommunityView: View {
    let subjectId: Int64
    let community: NativeSubjectCommunitySnapshot?
    let errorMessage: String?
    let totalEpisodes: Int?
    @ObservedObject var model: NativeAppModel
    let onReload: () -> Void

    @State private var selectedScore = 0
    @State private var selectedCollectionStatus: String?
    @State private var selectedCollectionProgress = 0
    @State private var commentText = ""
    @State private var commentSpoiler = false
    @State private var isSubmitting = false
    @State private var isSavingRating = false
    @State private var isSavingCollection = false
    @State private var localComments: [NativeCommentSnapshot] = []
    @State private var localReviews: [NativeReviewSnapshot] = []
    @State private var commentReactions: [String: NativeReactionSnapshot] = [:]
    @State private var reviewReactions: [String: NativeReactionSnapshot] = [:]
    @State private var pendingReactionKeys: Set<String> = []
    @State private var pendingReportIds: Set<String> = []
    @State private var message: String?
    @State private var showingReviewComposer = false
    @State private var showingAccount = false
    @State private var editingComment: NativeCommentSnapshot?
    @State private var deletingCommentId: String?
    @State private var replyTargetId: String?
    @State private var didLoadCommentDraft = false
    @FocusState private var commentEditorFocused: Bool

    private let collectionStatuses = ["Watching", "Wish", "Completed", "OnHold", "Dropped"]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(AnimeL10n.key(.communityTitle))
                        .font(.title2.weight(.bold))
                    Text(AnimeL10n.key(.communityDescription))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button(action: onReload) {
                    Image(systemName: "arrow.clockwise")
                }
                .accessibilityLabel(AnimeL10n.key(.communityRefresh))
            }

            if model.isRestoringSession {
                Label(AnimeL10n.key(.communityRestoring), systemImage: "arrow.triangle.2.circlepath")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.secondary)
            } else if !model.isAuthenticated {
                Label(AnimeL10n.key(.communityLoginDescription), systemImage: "lock.open")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(.secondary)
            }

            if let errorMessage {
                NativeInlineError(message: errorMessage, retry: onReload)
            } else if let community {
                ratingAndCollection(community.rating)
                reviewSection(mergedReviews(community.reviews))
                commentSection(community.comments)
            } else {
                ProgressView(AnimeL10n.key(.communityLoading))
                    .frame(maxWidth: .infinity, minHeight: 100)
            }

            if let message {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(message.contains(AnimeL10n.string(.errorFailureMarker)) ? .red : .secondary)
            }
        }
        .onAppear {
            restorePersonalState()
            loadCommentDraft()
        }
        .onChange(of: community?.comments.map(\.id) ?? []) { _, ids in
            // Keep an optimistic local insertion until the server returns it, then
            // reconcile by id instead of clearing all local content on every refresh.
            localComments.removeAll { ids.contains($0.id) }
        }
        .onChange(of: community?.reviews.map(\.id) ?? []) { _, ids in
            localReviews.removeAll { ids.contains($0.id) }
        }
        .onChange(of: community?.personal?.userRating) { _, _ in
            restorePersonalState()
        }
        .onChange(of: community?.personal?.collectionStatus) { _, _ in
            restorePersonalState()
        }
        .onChange(of: model.isSessionReady) { _, ready in
            if ready, model.isAuthenticated {
                restorePersonalState()
                onReload()
            }
        }
        .onChange(of: model.session.status) { _, status in
            if status != "authenticated" {
                selectedScore = 0
                selectedCollectionStatus = nil
                replyTargetId = nil
            }
        }
        .sheet(isPresented: $showingReviewComposer) {
            NativeReviewComposer(subjectId: subjectId, model: model) { review, error in
                if let review {
                    localReviews.insert(review, at: 0)
                    message = AnimeL10n.string(.reviewPublished)
                } else if let error {
                    message = AnimeL10n.string(.reviewPublishFailed, error)
                }
            }
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
        }
        .sheet(item: $editingComment) { comment in
            NativeCommentEditor(comment: comment, model: model) { updated, error in
                if let error {
                    message = AnimeL10n.string(.discussionUpdateFailed, error)
                } else {
                    if let updated, let index = localComments.firstIndex(where: { $0.id == updated.id }) {
                        localComments[index] = updated
                    }
                    message = AnimeL10n.string(.discussionUpdated)
                    onReload()
                }
            }
        }
        .confirmationDialog(AnimeL10n.key(.discussionDeleteConfirmation), isPresented: Binding(
            get: { deletingCommentId != nil },
            set: { if !$0 { deletingCommentId = nil } },
        ), titleVisibility: .visible) {
            Button(AnimeL10n.key(.actionDelete), role: .destructive) {
                guard let deletingCommentId else { return }
                model.deleteComment(id: deletingCommentId) { error in
                    message = error.map { AnimeL10n.string(.discussionDeleteFailed, $0) } ?? AnimeL10n.string(.discussionDeleted)
                    if error == nil {
                        localComments.removeAll { $0.id == deletingCommentId }
                        if replyTargetId == deletingCommentId { cancelReply() }
                    }
                    self.deletingCommentId = nil
                    if error == nil { onReload() }
                }
            }
        }
    }

    private func ratingAndCollection(_ rating: NativeRatingSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                Image(systemName: "star.fill")
                    .foregroundStyle(.orange)
                VStack(alignment: .leading, spacing: 2) {
                    Text(AnimeL10n.key(.communityAverageRating))
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                    Text(rating.score.map { String(format: "%.1f", $0) } ?? AnimeL10n.string(.noRating))
                        .font(.title3.weight(.bold))
                }
                Text(AnimeL10n.string(.ratingVotes, String(rating.votes)))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
            }

            HStack(spacing: 10) {
                Menu {
                    ForEach(1...10, id: \.self) { score in
                        Button(AnimeL10n.string(.myRatingScore, "\(score)", score == selectedScore ? AnimeL10n.string(.ratingSelectedMark) : "")) {
                            guard authorizeWrite() else { return }
                            let previousScore = selectedScore
                            selectedScore = score
                            saveRating(score, previousScore: previousScore)
                        }
                    }
                } label: {
                    Label(
                        selectedScore == 0 ? AnimeL10n.key(.myRating) : LocalizedStringKey(AnimeL10n.string(.myRatingScore, "\(selectedScore)/10", "")),
                        systemImage: selectedScore == 0 ? "star" : "star.fill",
                    )
                }
                .buttonStyle(.borderedProminent)
                .disabled(model.isRestoringSession || isSavingRating)

                Menu {
                    ForEach(collectionStatuses, id: \.self) { status in
                        Button(status.collectionDisplayName) {
                            guard authorizeWrite() else { return }
                            saveCollection(status)
                        }
                    }
                    Divider()
                    Button(AnimeL10n.key(.collectionRemove), role: .destructive) {
                        guard authorizeWrite() else { return }
                        saveCollection(nil)
                    }
                } label: {
                    Label(selectedCollectionStatus?.collectionDisplayName ?? AnimeL10n.string(.collectionStatus), systemImage: selectedCollectionStatus == nil ? "plus.circle" : "checkmark.circle.fill")
                }
                .buttonStyle(.bordered)
                .disabled(model.isRestoringSession || isSavingCollection)
            }

            if selectedCollectionStatus != nil {
                Stepper(value: Binding(
                    get: { selectedCollectionProgress },
                    set: { saveCollectionProgress($0) },
                ), in: 0...max(0, totalEpisodes ?? 999)) {
                    HStack {
                        Label(AnimeL10n.key(.watchProgress), systemImage: "play.rectangle")
                        Spacer()
                        Text(progressLabel)
                            .foregroundStyle(.secondary)
                    }
                }
                .disabled(model.isRestoringSession || isSavingCollection)
            }
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    @ViewBuilder
    private func reviewSection(_ reviews: [NativeReviewSnapshot]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(AnimeL10n.key(.profileReviewMetric))
                    .font(.title3.weight(.bold))
                Spacer()
                Button(AnimeL10n.key(.writeShortReview)) {
                    guard authorizeWrite() else { return }
                    showingReviewComposer = true
                }
                .disabled(model.isRestoringSession)
                    .font(.subheadline.weight(.semibold))
                Text("\(reviews.count)")
                    .foregroundStyle(.secondary)
            }

            if reviews.isEmpty {
                Text(AnimeL10n.key(.noReviewsPrompt))
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 8)
            } else {
                ForEach(Array(reviews.prefix(5))) { review in
                    reviewCard(review)
                }
            }
        }
    }

    private func reviewCard(_ review: NativeReviewSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .firstTextBaseline) {
                Text(review.title ?? AnimeL10n.string(.reviewTitleFallback))
                    .font(.headline)
                Spacer()
                Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            if review.spoiler {
                Label(AnimeL10n.key(.formSpoiler), systemImage: "eye.slash")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.orange)
            }
            Text(review.body)
                .font(.body)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 16) {
                reactionButton(
                    title: "\(reviewReaction(review.id, kind: "like")?.likeCount ?? review.likeCount)",
                    systemImage: reviewReaction(review.id, kind: "like")?.active == true ? "heart.fill" : "heart",
                    tint: reviewReaction(review.id, kind: "like")?.active == true ? .pink : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(review.id, kind: "like")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(review.id, kind: "like")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = reviewReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactReview(id: review.id, reaction: "like", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { reviewReactions[key] = reaction }
                        message = error.map { AnimeL10n.string(.activityOperationFailed, $0) }
                    }
                }
                reactionButton(
                    title: "\(reviewReaction(review.id, kind: "bookmark")?.bookmarkCount ?? review.bookmarkCount)",
                    systemImage: reviewReaction(review.id, kind: "bookmark")?.active == true ? "bookmark.fill" : "bookmark",
                    tint: reviewReaction(review.id, kind: "bookmark")?.active == true ? .blue : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(review.id, kind: "bookmark")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(review.id, kind: "bookmark")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = reviewReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactReview(id: review.id, reaction: "bookmark", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { reviewReactions[key] = reaction }
                        message = error.map { AnimeL10n.string(.activityOperationFailed, $0) }
                    }
                }
            }
            .font(.caption.weight(.medium))
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    @ViewBuilder
    private func commentSection(_ comments: [NativeCommentSnapshot]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(AnimeL10n.key(.commentTitle))
                    .font(.title3.weight(.bold))
                Spacer()
                Text("\(mergedComments(comments).count)")
                    .foregroundStyle(.secondary)
            }

            VStack(spacing: 10) {
                if let replyTarget {
                    HStack(spacing: 8) {
                        Image(systemName: "arrowshape.turn.up.left")
                        Text(AnimeL10n.string(.commentReply, replyTarget.authorName))
                            .font(.caption.weight(.medium))
                        Spacer()
                        Button(AnimeL10n.key(.actionCancel)) { cancelReply() }
                            .font(.caption.weight(.semibold))
                    }
                    .foregroundStyle(.secondary)
                }
                TextEditor(text: $commentText)
                    .frame(minHeight: 84)
                    .padding(8)
                    .scrollContentBackground(.hidden)
                    .focused($commentEditorFocused)
                    .disabled(model.isRestoringSession)
                    .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(alignment: .topLeading) {
                        if commentText.isEmpty {
                            Text(AnimeL10n.key(.commentPlaceholder))
                                .foregroundStyle(.tertiary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 16)
                                .allowsHitTesting(false)
                        }
                    }
                HStack {
                    Toggle(AnimeL10n.key(.commentSpoiler), isOn: $commentSpoiler)
                        .font(.caption)
                    Spacer()
                    Button(isSubmitting ? AnimeL10n.key(.commentPublishing) : AnimeL10n.key(.actionPublish)) { submitComment() }
                        .buttonStyle(.borderedProminent)
                        .disabled(model.isRestoringSession || isSubmitting || commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }

            let visibleComments = mergedComments(comments)
            if visibleComments.isEmpty {
                Text(AnimeL10n.key(.noCommentsPrompt))
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 8)
            } else {
                let commentIndex = Dictionary(uniqueKeysWithValues: visibleComments.map { ($0.id, $0) })
                let rootComments = visibleComments.filter { comment in
                    guard let parentId = comment.parentId else { return true }
                    // Only one reply level is rendered. Unexpected deeper replies
                    // are shown as standalone comments and cannot start another reply.
                    return commentIndex[parentId]?.parentId != nil || commentIndex[parentId] == nil
                }
                ForEach(rootComments) { comment in
                    commentCard(comment, isReply: false)
                    ForEach(visibleComments.filter { $0.parentId == comment.id }) { reply in
                        commentCard(reply, isReply: true)
                            .padding(.leading, 20)
                    }
                }
            }
        }
    }

    private func commentCard(_ comment: NativeCommentSnapshot, isReply: Bool) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(comment.authorName)
                    .font(.headline)
                if comment.owned {
                    Text(AnimeL10n.key(.currentUser))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tint)
                }
                Spacer()
                Text(comment.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Menu {
                    if comment.owned {
                        Button(AnimeL10n.key(.commentEdit)) {
                            guard authorizeWrite() else { return }
                            editingComment = comment
                        }
                        Button(AnimeL10n.key(.commentDelete), role: .destructive) {
                            guard authorizeWrite() else { return }
                            deletingCommentId = comment.id
                        }
                    } else {
                        Button(AnimeL10n.key(.commentReport)) {
                            guard authorizeWrite() else { return }
                            guard !pendingReportIds.contains(comment.id) else { return }
                            pendingReportIds.insert(comment.id)
                            model.reportComment(id: comment.id) { error in
                                pendingReportIds.remove(comment.id)
                                message = error.map { AnimeL10n.string(.commentReportFailed, $0) } ?? AnimeL10n.string(.commentReportSubmitted)
                            }
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(.secondary)
                }
            }
            Text(comment.body)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 16) {
                if !isReply, comment.parentId == nil {
                    Button(AnimeL10n.key(.commentReplyAction)) { beginReply(to: comment) }
                        .foregroundStyle(.tint)
                }
                reactionButton(
                    title: "\(commentReaction(comment.id, kind: "like")?.likeCount ?? comment.likeCount)",
                    systemImage: commentReaction(comment.id, kind: "like")?.active == true ? "heart.fill" : "heart",
                    tint: commentReaction(comment.id, kind: "like")?.active == true ? .pink : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(comment.id, kind: "like")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(comment.id, kind: "like")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = commentReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactComment(id: comment.id, reaction: "like", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { commentReactions[key] = reaction }
                        message = error.map { AnimeL10n.string(.activityOperationFailed, $0) }
                    }
                }
                reactionButton(
                    title: "\(commentReaction(comment.id, kind: "bookmark")?.bookmarkCount ?? comment.bookmarkCount)",
                    systemImage: commentReaction(comment.id, kind: "bookmark")?.active == true ? "bookmark.fill" : "bookmark",
                    tint: commentReaction(comment.id, kind: "bookmark")?.active == true ? .blue : .secondary,
                    disabled: model.isRestoringSession || pendingReactionKeys.contains(reactionKey(comment.id, kind: "bookmark")),
                ) {
                    guard authorizeWrite() else { return }
                    let key = reactionKey(comment.id, kind: "bookmark")
                    guard !pendingReactionKeys.contains(key) else { return }
                    let active = commentReactions[key]?.active != true
                    pendingReactionKeys.insert(key)
                    model.reactComment(id: comment.id, reaction: "bookmark", active: active) { reaction, error in
                        pendingReactionKeys.remove(key)
                        if let reaction { commentReactions[key] = reaction }
                        message = error.map { AnimeL10n.string(.activityOperationFailed, $0) }
                    }
                }
            }
            .font(.caption.weight(.medium))
        }
        .padding(14)
        .id("comment-\(comment.id)")
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var replyTarget: NativeCommentSnapshot? {
        guard let replyTargetId else { return nil }
        let comments = mergedComments(community?.comments ?? [])
        return comments.first { $0.id == replyTargetId && $0.parentId == nil }
    }

    private func mergedComments(_ comments: [NativeCommentSnapshot]) -> [NativeCommentSnapshot] {
        var result = comments
        let ids = Set(comments.map(\.id))
        result.append(contentsOf: localComments.filter { !ids.contains($0.id) })
        return result
    }

    private func mergedReviews(_ reviews: [NativeReviewSnapshot]) -> [NativeReviewSnapshot] {
        var result = reviews
        let ids = Set(reviews.map(\.id))
        result.append(contentsOf: localReviews.filter { !ids.contains($0.id) })
        return result
    }

    private func reactionKey(_ id: String, kind: String) -> String {
        "\(id):\(kind)"
    }

    private func reviewReaction(_ id: String, kind: String) -> NativeReactionSnapshot? {
        reviewReactions[reactionKey(id, kind: kind)]
    }

    private func commentReaction(_ id: String, kind: String) -> NativeReactionSnapshot? {
        commentReactions[reactionKey(id, kind: kind)]
    }

    private func reactionButton(
        title: String,
        systemImage: String,
        tint: Color,
        disabled: Bool = false,
        action: @escaping () -> Void,
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .foregroundStyle(tint)
        }
        .buttonStyle(.plain)
        .disabled(disabled)
    }

    private func authorizeWrite() -> Bool {
        if model.isRestoringSession {
            message = AnimeL10n.string(.loginRestoring)
            return false
        }
        guard model.isAuthenticated else {
            showingAccount = true
            return false
        }
        return true
    }

    private func beginReply(to comment: NativeCommentSnapshot) {
        guard comment.parentId == nil, authorizeWrite() else { return }
        replyTargetId = comment.id
        commentEditorFocused = true
        persistCommentDraft()
    }

    private func cancelReply() {
        replyTargetId = nil
        commentEditorFocused = false
        persistCommentDraft()
    }

    private func restorePersonalState() {
        guard model.isAuthenticated else {
            selectedScore = 0
            selectedCollectionStatus = nil
            selectedCollectionProgress = 0
            return
        }
        selectedScore = community?.personal?.userRating ?? model.cachedUserRating(subjectId: subjectId) ?? 0
        selectedCollectionStatus = community?.personal?.isCollected == false
            ? nil
            : community?.personal?.collectionStatus
        selectedCollectionProgress = max(0, community?.personal?.collectionEpisodeProgress ?? 0)
    }

    private var commentDraftKey: String {
        "anime.ios.comment-draft.\(subjectId)"
    }

    private func loadCommentDraft() {
        guard !didLoadCommentDraft else { return }
        didLoadCommentDraft = true
        guard
            let data = UserDefaults.standard.data(forKey: commentDraftKey),
            let draft = try? JSONDecoder().decode(NativeCommentDraft.self, from: data)
        else { return }
        commentText = draft.text
        commentSpoiler = draft.spoiler
        replyTargetId = draft.parentId
    }

    private func persistCommentDraft() {
        guard didLoadCommentDraft else { return }
        let text = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty || replyTargetId != nil else {
            UserDefaults.standard.removeObject(forKey: commentDraftKey)
            return
        }
        let draft = NativeCommentDraft(text: commentText, spoiler: commentSpoiler, parentId: replyTargetId)
        if let data = try? JSONEncoder().encode(draft) {
            UserDefaults.standard.set(data, forKey: commentDraftKey)
        }
    }

    private func saveRating(_ score: Int, previousScore: Int? = nil) {
        guard !isSavingRating else { return }
        isSavingRating = true
        message = nil
        model.saveRating(subjectId: subjectId, score: score) { error in
            isSavingRating = false
            if let error {
                selectedScore = previousScore ?? 0
                message = AnimeL10n.string(.ratingSaveFailed, error)
            } else {
                message = AnimeL10n.string(.ratingSaved)
            }
            if error == nil { onReload() }
        }
    }

    private func saveCollection(_ status: String?) {
        guard !isSavingCollection else { return }
        isSavingCollection = true
        message = nil
        model.setCollection(subjectId: subjectId, status: status, episodeProgress: status == nil ? nil : selectedCollectionProgress) { error in
            isSavingCollection = false
            if error == nil {
                selectedCollectionStatus = status
                message = status.map { AnimeL10n.string(.collectionAdded, $0.collectionDisplayName) } ?? AnimeL10n.string(.collectionRemoved)
                onReload()
            } else if let error {
                message = AnimeL10n.string(.collectionSaveFailed, error)
            }
        }
    }

    private func saveCollectionProgress(_ progress: Int) {
        guard let status = selectedCollectionStatus, !isSavingCollection else { return }
        let maximum = max(0, totalEpisodes ?? 999)
        let normalized = min(max(progress, 0), maximum)
        guard normalized != selectedCollectionProgress else { return }
        let previous = selectedCollectionProgress
        selectedCollectionProgress = normalized
        isSavingCollection = true
        model.setCollection(subjectId: subjectId, status: status, episodeProgress: normalized) { error in
            isSavingCollection = false
            if let error {
                selectedCollectionProgress = previous
                message = AnimeL10n.string(.progressSaveFailed, error)
            } else {
                message = AnimeL10n.string(.progressSaved)
                onReload()
            }
        }
    }

    private var progressLabel: String {
        if let totalEpisodes, totalEpisodes > 0 {
            return "\(selectedCollectionProgress)/\(totalEpisodes)"
        }
        return AnimeL10n.string(.episodeProgress, String(selectedCollectionProgress))
    }

    private func submitComment() {
        let body = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        guard authorizeWrite(), !isSubmitting else { return }
        let allComments = mergedComments(community?.comments ?? [])
        let parentId = replyTargetId.flatMap { targetId in
            allComments.first { $0.id == targetId && $0.parentId == nil }?.id
        }
        isSubmitting = true
        message = nil
        model.createCommentAdvanced(subjectId: subjectId, body: body, spoiler: commentSpoiler, parentId: parentId) { comment, error in
            isSubmitting = false
            if let comment {
                localComments.insert(comment, at: 0)
                commentText = ""
                commentSpoiler = false
                replyTargetId = nil
                commentEditorFocused = false
                persistCommentDraft()
                message = AnimeL10n.string(.discussionPublished)
            } else if let error {
                message = AnimeL10n.string(.discussionPublishFailed, error)
            }
        }
    }
}

private struct NativeCommentDraft: Codable {
    let text: String
    let spoiler: Bool
    let parentId: String?
}

private struct NativeCommentEditor: View {
    let comment: NativeCommentSnapshot
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeCommentSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var commentText: String
    @State private var spoiler: Bool
    @State private var isSaving = false

    init(comment: NativeCommentSnapshot, model: NativeAppModel, onComplete: @escaping (NativeCommentSnapshot?, String?) -> Void) {
        self.comment = comment
        self.model = model
        self.onComplete = onComplete
        _commentText = State(initialValue: comment.body)
        _spoiler = State(initialValue: comment.spoiler)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextEditor(text: $commentText).frame(minHeight: 150)
                Toggle(AnimeL10n.key(.formSpoiler), isOn: $spoiler)
                Button(isSaving ? AnimeL10n.key(.actionSaving) : AnimeL10n.key(.actionSaveChanges)) { save() }
                    .disabled(isSaving || commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle(AnimeL10n.key(.commentEdit))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(AnimeL10n.key(.actionCancel)) { dismiss() } }
            }
        }
    }

    private func save() {
        guard !isSaving else { return }
        let body = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        isSaving = true
        model.updateComment(id: comment.id, body: body, spoiler: spoiler) { value, error in
            isSaving = false
            onComplete(value, error)
            if value != nil { dismiss() }
        }
    }
}

