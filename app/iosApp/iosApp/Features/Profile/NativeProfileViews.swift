import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeRatingsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?
    @State private var deletingRating: NativeProfileRatingSnapshot?
    @State private var message: String?

    var body: some View {
        ScrollView {
                LazyVStack(spacing: 12) {
                    if let page = model.myRatingsPage, !page.items.isEmpty {
                        ForEach(page.items) { rating in
                            NavigationLink {
                                NativeSubjectDetailView(
                                    summary: .placeholder(id: rating.subjectId, title: rating.title),
                                    model: model,
                                )
                            } label: {
                                NativeRatingRow(rating: rating)
                            }
                            .buttonStyle(.plain)
                            .contextMenu {
                                Button(AnimeL10n.key(.ratingsDelete), role: .destructive) { deletingRating = rating }
                            }
                        }
                        if let cursor = page.nextCursor {
                            Button(AnimeL10n.key(.actionLoadMore)) {
                                model.loadMyRatings(cursor: cursor, append: true) { _, error in errorMessage = error }
                            }
                            .buttonStyle(.bordered)
                        }
                    } else if let errorMessage {
                        NativeInlineError(message: errorMessage) { load() }
                    } else {
                        ContentUnavailableView(AnimeL10n.key(.ratingsEmpty), systemImage: "star", description: Text(AnimeL10n.key(.ratingsEmptyDescription)))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                    if let message { Text(message).font(.footnote).foregroundStyle(.secondary) }
                }
                .padding(16)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .navigationTitle(AnimeL10n.key(.ratingsTitle))
            .navigationBarTitleDisplayMode(.large)
            .task { load() }
            .refreshable { await refresh() }
            .confirmationDialog(AnimeL10n.string(.ratingsDeleteConfirmation), isPresented: Binding(
                get: { deletingRating != nil },
                set: { if !$0 { deletingRating = nil } },
            ), titleVisibility: .visible) {
                Button(AnimeL10n.key(.ratingsDelete), role: .destructive) {
                    guard let rating = deletingRating else { return }
                    model.deleteRating(subjectId: rating.subjectId) { error in
                        if let error { message = AnimeL10n.string(.ratingsDeleteFailed, error) }
                        else { message = AnimeL10n.string(.ratingsDeleted); load() }
                        deletingRating = nil
                    }
                }
        }
    }

    private func load() {
        errorMessage = nil
        if model.myRatingsPage == nil { model.loadMyRatings { _, error in errorMessage = error } }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadMyRatings { _, error in errorMessage = error; continuation.resume() }
        }
    }
}

struct NativeMyReviewsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 12) {
                    if let userId = model.session.userId, let page = model.userReviews[userId], !page.items.isEmpty {
                        ForEach(page.items) { review in
                            NavigationLink {
                                NativeReviewDetailView(reviewId: review.id, model: model)
                            } label: {
                                NativeReviewRow(review: review)
                            }
                            .buttonStyle(.plain)
                        }
                        if let cursor = page.nextCursor {
                            Button(AnimeL10n.key(.actionLoadMore)) {
                                model.loadUserReviews(id: userId, cursor: cursor) { _, error in errorMessage = error }
                            }
                            .buttonStyle(.bordered)
                        }
                    } else if let errorMessage {
                        NativeInlineError(message: errorMessage) { load() }
                    } else {
                        ContentUnavailableView(AnimeL10n.key(.reviewsEmpty), systemImage: "text.quote", description: Text(AnimeL10n.key(.reviewsEmptyDescription)))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                }
                .padding(16)
            }
            .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle(AnimeL10n.key(.reviewsTitle))
        .navigationBarTitleDisplayMode(.large)
        .task { load() }
        .refreshable { await refresh() }
    }
    }

    private func load() {
        guard let userId = model.session.userId else { return }
        errorMessage = nil
        model.loadUserReviews(id: userId) { _, error in errorMessage = error }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            guard let userId = model.session.userId else {
                continuation.resume()
                return
            }
            errorMessage = nil
            model.loadUserReviews(id: userId) { _, error in
                errorMessage = error
                continuation.resume()
            }
        }
    }
}


struct NativeProfileView: View {
    @ObservedObject var model: NativeAppModel
    @State private var profileTitleOpacity = 1.0
    @State private var showingAccount = false
    @State private var message: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    if model.session.status == "authenticated" {
                        authenticatedContent
                    } else if model.session.status == "restoring" {
                        ProgressView(AnimeL10n.key(.statusRestoring))
                            .frame(maxWidth: .infinity, minHeight: 180)
                    } else {
                        guestContent
                    }
                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(message.contains(AnimeL10n.string(.errorFailureMarker)) ? .red : .secondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .onScrollGeometryChange(for: CGFloat.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top
            } action: { _, offset in
                let fadeDistance: CGFloat = 56
                let nextOpacity = 1 - min(max(offset / fadeDistance, 0), 1)
                if abs(nextOpacity - profileTitleOpacity) > 0.01 {
                    profileTitleOpacity = nextOpacity
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text(AnimeL10n.key(.profile))
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                        .layoutPriority(1)
                        .opacity(profileTitleOpacity)
                        .accessibilityAddTraits(.isHeader)
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeSettingsView(model: model)
                    } label: {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel(AnimeL10n.key(.settings))
                }
            }
            .sheet(isPresented: $showingAccount) {
                NativeAccountSheet(model: model)
                    .presentationDetents([.medium, .large])
            }
        }
        .task {
            model.start()
            model.loadNetworkContext()
            if model.session.status == "authenticated" { model.loadProfile() }
        }
        .onChange(of: model.session.status) { _, status in
            if status == "authenticated" { model.loadProfile() }
        }
    }

    @ViewBuilder
    private var authenticatedContent: some View {
        HStack(spacing: 14) {
            NativeAvatar(url: model.profile?.avatarURL ?? model.session.avatarURL, name: model.profile?.displayName ?? model.session.displayName)
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(model.profile?.displayName ?? model.session.displayName ?? AnimeL10n.string(.userFallback))
                        .font(.title3.weight(.bold))
                    if let location = model.networkContext?.displayLocation,
                       !location.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                         Text(AnimeL10n.string(.profileIPAddress, location))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                }
                Text(AnimeL10n.string(.loggedInAs, model.profile?.provider ?? AnimeL10n.string(.appName)))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(18)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))

        if let profile = model.profile {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                NavigationLink {
                    NativeCollectionView(model: model)
                } label: {
                    NativeMetric(value: profile.watchingCount + profile.completedCount, title: AnimeL10n.string(.profileCollectionMetric))
                }
                .buttonStyle(.plain)
                NavigationLink {
                    NativeRatingsView(model: model)
                } label: {
                    NativeMetric(value: profile.ratingCount, title: AnimeL10n.string(.profileRatingMetric))
                }
                .buttonStyle(.plain)
                NavigationLink {
                    NativeMyReviewsView(model: model)
                } label: {
                    NativeMetric(value: profile.reviewCount, title: AnimeL10n.string(.profileReviewMetric))
                }
                .buttonStyle(.plain)
            }
        }

        NavigationLink {
            NativeCollectionView(model: model)
        } label: {
            NativeActionRow(title: AnimeL10n.string(.profileLibrary), subtitle: AnimeL10n.string(.profileLibrarySubtitle), systemImage: "books.vertical")
        }
        .buttonStyle(.plain)

        if model.profile?.role == "maintainer" {
            NavigationLink {
                NativeAdminView(model: model)
            } label: {
                NativeActionRow(title: AnimeL10n.string(.profileAdminConsole), subtitle: AnimeL10n.string(.profileAdminDescription), systemImage: "shield.lefthalf.filled")
            }
            .buttonStyle(.plain)
        }

        Button {
            model.logout { error in message = error ?? AnimeL10n.string(.profileLoggedOut) }
        } label: {
            NativeActionRow(title: AnimeL10n.string(.actionLogout), subtitle: AnimeL10n.string(.profileLogoutDescription), systemImage: "rectangle.portrait.and.arrow.right", tint: .red)
        }
        .buttonStyle(.plain)
    }

    private var guestContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 42))
                .foregroundStyle(.tint)
            Text(AnimeL10n.key(.profileLoginTitle))
                .font(.title2.weight(.bold))
            Text(AnimeL10n.key(.profileLoginMessage))
                .foregroundStyle(.secondary)
            Button(AnimeL10n.key(.actionLoginOrRegister)) { showingAccount = true }
                .buttonStyle(.borderedProminent)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct NativeReviewDetailView: View {
    let reviewId: String
    @ObservedObject var model: NativeAppModel
    @State private var review: NativeReviewSnapshot?
    @State private var errorMessage: String?
    @State private var isLoading = true
    @State private var isLiked = false
    @State private var isBookmarked = false
    @State private var message: String?
    @State private var showingEditor = false
    @State private var showingDeleteConfirmation = false
    @State private var showingAccount = false
    @State private var isReacting = false
    @State private var isDeleting = false

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                if isLoading && review == nil {
                    ProgressView(AnimeL10n.key(.reviewLoading))
                        .frame(maxWidth: .infinity, minHeight: 220)
                } else if let review {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 5) {
                                Text(review.title ?? AnimeL10n.string(.reviewTitleFallback))
                                    .font(.title2.weight(.bold))
                                Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(16))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if review.spoiler {
                                Label(AnimeL10n.key(.reviewSpoiler), systemImage: "eye.slash")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.orange)
                            }
                        }
                        Text(review.body)
                            .font(.body)
                            .fixedSize(horizontal: false, vertical: true)
                        HStack(spacing: 18) {
                            Button {
                                guard authorizeWrite() else { return }
                                react("like")
                            } label: {
                                Label("\(review.likeCount)", systemImage: isLiked ? "heart.fill" : "heart")
                            }
                            .disabled(model.isRestoringSession || isReacting)
                            Button {
                                guard authorizeWrite() else { return }
                                react("bookmark")
                            } label: {
                                Label("\(review.bookmarkCount)", systemImage: isBookmarked ? "bookmark.fill" : "bookmark")
                            }
                            .disabled(model.isRestoringSession || isReacting)
                        }
                        .foregroundStyle(.tint)
                    }
                    .padding(18)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))

                    NavigationLink {
                        NativeSubjectDetailView(summary: NativeSubjectSummary.placeholder(id: review.subjectId, title: AnimeL10n.string(.subjectTitleFallback)), model: model)
                    } label: {
                        NativeActionRow(title: AnimeL10n.string(.reviewSubjectAction), subtitle: AnimeL10n.string(.reviewSubjectActionDescription), systemImage: "film")
                    }
                    .buttonStyle(.plain)

                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(message.hasPrefix(AnimeL10n.string(.errorFailureMarker)) ? .red : .secondary)
                    }
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.reviewTitle))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let review, review.owned, review.kind == "short" {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button(AnimeL10n.key(.actionEdit)) {
                            guard authorizeWrite() else { return }
                            showingEditor = true
                        }
                        Button(AnimeL10n.key(.actionDelete), role: .destructive) {
                            guard authorizeWrite() else { return }
                            showingDeleteConfirmation = true
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .task { load() }
        .refreshable { await refresh() }
        .sheet(isPresented: $showingEditor) {
            if let review {
                NativeReviewEditor(review: review, model: model) { updated, error in
                    if let updated { self.review = updated; message = AnimeL10n.string(.reviewUpdated) }
                    if let error { message = AnimeL10n.string(.reviewUpdateFailed, error) }
                }
            }
        }
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
        }
        .confirmationDialog(AnimeL10n.key(.reviewDeleteConfirmation), isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button(AnimeL10n.key(.actionDelete), role: .destructive) {
                guard authorizeWrite(), !isDeleting else { return }
                isDeleting = true
                model.deleteReview(id: reviewId) { error in
                    isDeleting = false
                    if let error { message = AnimeL10n.string(.reviewDeleteFailed, error) }
                    else { message = AnimeL10n.string(.reviewDeleted); review = nil }
                }
            }
        }
    }

    private func load() {
        isLoading = true
        errorMessage = nil
        model.loadReview(id: reviewId) { value, error in
            review = value
            errorMessage = error
            isLiked = false
            isBookmarked = false
            isLoading = false
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadReview(id: reviewId) { value, error in
                review = value
                errorMessage = error
                continuation.resume()
            }
        }
    }

    private func react(_ reaction: String) {
        guard !isReacting else { return }
        let active = reaction == "like" ? !isLiked : !isBookmarked
        isReacting = true
        model.reactReview(id: reviewId, reaction: reaction, active: active) { value, error in
            isReacting = false
            if let value {
                if reaction == "like" { isLiked = value.active }
                if reaction == "bookmark" { isBookmarked = value.active }
                message = AnimeL10n.string(.operationCompleted)
            } else if let error {
                message = AnimeL10n.string(.activityOperationFailed, error)
            }
        }
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
}

private struct NativeReviewEditor: View {
    let review: NativeReviewSnapshot
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeReviewSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var reviewText: String
    @State private var spoiler: Bool
    @State private var visibility: String
    @State private var isSaving = false

    init(review: NativeReviewSnapshot, model: NativeAppModel, onComplete: @escaping (NativeReviewSnapshot?, String?) -> Void) {
        self.review = review
        self.model = model
        self.onComplete = onComplete
        _reviewText = State(initialValue: review.body)
        _spoiler = State(initialValue: review.spoiler)
        _visibility = State(initialValue: review.visibility)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextEditor(text: $reviewText).frame(minHeight: 170)
                Toggle(AnimeL10n.key(.formSpoiler), isOn: $spoiler)
                Picker(AnimeL10n.key(.visibility), selection: $visibility) {
                    Text(AnimeL10n.key(.visibilityPublic)).tag("public")
                    Text(AnimeL10n.key(.visibilityPrivate)).tag("private")
                }
                Button(isSaving ? AnimeL10n.key(.actionSaving) : AnimeL10n.key(.actionSaveChanges)) { save() }
                    .disabled(isSaving || reviewText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle(AnimeL10n.key(.actionEdit))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(AnimeL10n.key(.actionCancel)) { dismiss() } }
            }
        }
    }

    private func save() {
        guard !isSaving else { return }
        let body = reviewText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        isSaving = true
        model.updateReview(
            id: review.id,
            title: nil,
            body: body,
            spoiler: spoiler,
            visibility: visibility,
        ) { updated, error in
            isSaving = false
            onComplete(updated, error)
            if updated != nil { dismiss() }
        }
    }
}

struct NativeUserProfileView: View {
    let userId: String
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?
    @State private var isLoading = true
    @State private var isFollowing = false
    @State private var message: String?

    private var profile: NativeUserProfileSnapshot? { model.userProfiles[userId] }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                if let profile {
                    HStack(spacing: 14) {
                        NativeAvatar(url: profile.avatarURL, name: profile.displayName)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(profile.displayName).font(.title3.weight(.bold))
                            Text(AnimeL10n.string(.userJoinedAt, String(profile.createdAt.prefix(10))))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button(isFollowing ? AnimeL10n.key(.actionFollowing) : AnimeL10n.key(.actionFollow)) {
                            model.followUser(id: userId, following: !isFollowing) { value, error in
                                if let value { isFollowing = value }
                                message = error.map { AnimeL10n.string(.activityOperationFailed, $0) }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                        NativeMetric(value: Int(profile.reviewCount), title: AnimeL10n.string(.profileReviewMetric))
                        NativeMetric(value: Int(profile.ratingCount), title: AnimeL10n.string(.profileRatingMetric))
                        NativeMetric(value: Int(profile.followerCount), title: AnimeL10n.string(.userFollowers))
                    }

                    if let reviews = model.userReviews[userId]?.items, !reviews.isEmpty {
                        Text(AnimeL10n.key(.userReviews)).font(.title3.weight(.bold))
                        ForEach(reviews) { review in
                            NavigationLink {
                                NativeReviewDetailView(reviewId: review.id, model: model)
                            } label: {
                                NativeReviewRow(review: review)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    if let lists = model.userLists[userId], !lists.isEmpty {
                        Text(AnimeL10n.key(.userLists)).font(.title3.weight(.bold))
                        ForEach(lists) { list in
                            NavigationLink {
                                NativeListDetailView(listId: list.id, model: model)
                            } label: {
                                NativeListSummaryRow(list: list)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    if let message {
                        Text(message).font(.footnote).foregroundStyle(.secondary)
                    }
                } else if isLoading {
                    ProgressView(AnimeL10n.key(.userLoading))
                        .frame(maxWidth: .infinity, minHeight: 220)
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.userTitle))
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .refreshable { await refresh() }
    }

    private func load() {
        isLoading = true
        errorMessage = nil
        var remaining = 3
        func finish() {
            remaining -= 1
            if remaining == 0 { isLoading = false }
        }
        model.loadUserProfile(id: userId) { value, error in
            if let value { isFollowing = value.following }
            errorMessage = error
            finish()
        }
        model.loadUserReviews(id: userId) { _, error in
            if errorMessage == nil { errorMessage = error }
            finish()
        }
        model.loadUserLists(id: userId) { _, error in
            if errorMessage == nil { errorMessage = error }
            finish()
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadUserProfile(id: userId) { value, error in
                if let value { isFollowing = value.following }
                errorMessage = error
                continuation.resume()
            }
            model.loadUserReviews(id: userId)
            model.loadUserLists(id: userId)
        }
    }
}

private struct NativeReviewRow: View {
    let review: NativeReviewSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(review.title ?? AnimeL10n.string(.reviewTitleFallback)).font(.headline)
            Text(review.body).font(.subheadline).foregroundStyle(.secondary).lineLimit(3)
            Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

