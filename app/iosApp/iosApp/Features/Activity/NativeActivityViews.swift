import SwiftUI
import Foundation
import PhotosUI
import UIKit

struct NativeActivityView: View {
    @ObservedObject var model: NativeAppModel
    @State private var activityTitleOpacity = 1.0
    @State private var selectedMode = "feed"
    @State private var selectedFeed = "public"
    @State private var errorMessage: String?
    @State private var activityToDelete: NativeActivityItemSnapshot?
    @State private var activityMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    Picker(AnimeL10n.key(.activityContent), selection: $selectedMode) {
                        Text(AnimeL10n.key(.activityFeedMode)).tag("feed")
                        Text(AnimeL10n.key(.activityNotificationsMode)).tag("notifications")
                    }
                    .pickerStyle(.segmented)
                    if selectedMode == "feed" {
                        feedPicker
                        activityContent
                    } else {
                        notificationContent
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
                if abs(nextOpacity - activityTitleOpacity) > 0.01 {
                    activityTitleOpacity = nextOpacity
                }
            }
            .refreshable { await refresh() }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text(AnimeL10n.key(.communityTitle))
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                        .layoutPriority(1)
                        .opacity(activityTitleOpacity)
                        .accessibilityAddTraits(.isHeader)
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeListsView(model: model)
                    } label: {
                        Image(systemName: "list.bullet.rectangle")
                    }
                    .accessibilityLabel(AnimeL10n.string(.activityLists))
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
            .confirmationDialog(AnimeL10n.string(.activityRetractConfirmation), isPresented: Binding(
                get: { activityToDelete != nil },
                set: { if !$0 { activityToDelete = nil } },
            ), titleVisibility: .visible) {
                Button(AnimeL10n.key(.activityRetract), role: .destructive) { deleteActivity() }
            }
        }
        .task {
            model.start()
            loadCurrentMode()
        }
        .onChange(of: selectedMode) { _, _ in loadCurrentMode() }
        .onChange(of: selectedFeed) { _, _ in loadCurrentMode() }
    }

    private var feedPicker: some View {
        HStack(spacing: 8) {
            ForEach(["following", "popular", "public"], id: \.self) { feed in
                Button(feed.feedDisplayName) { selectedFeed = feed }
                    .buttonStyle(.borderedProminent)
                    .tint(selectedFeed == feed ? .accentColor : .gray.opacity(0.35))
            }
        }
    }

    @ViewBuilder
    private var activityContent: some View {
        if let activityMessage {
            Text(activityMessage).font(.footnote).foregroundStyle(.secondary)
        }
        if let errorMessage {
            NativeInlineError(message: errorMessage) { loadCurrentMode() }
        } else if let page = model.activityPage, !page.items.isEmpty {
            ForEach(page.items) { item in
                NativeActivityCard(
                    item: item,
                    destination: activityDestination(for: item),
                    onDelete: item.owned && isDeletable(item) ? { activityToDelete = item } : nil,
                )
            }
            if page.nextCursor != nil {
                Button(AnimeL10n.key(.actionLoadMore)) {
                    model.loadActivity(feed: selectedFeed, cursor: page.nextCursor, append: true)
                }
                .frame(maxWidth: .infinity)
                .buttonStyle(.bordered)
            }
        } else {
            ContentUnavailableView(AnimeL10n.key(.activityEmptyTitle), systemImage: "bubble.left.and.bubble.right", description: Text(AnimeL10n.key(.activityEmptyMessage)))
                .frame(maxWidth: .infinity, minHeight: 220)
        }
    }

    @ViewBuilder
    private var notificationContent: some View {
        if model.notifications.isEmpty {
            ContentUnavailableView(AnimeL10n.key(.notificationEmptyTitle), systemImage: "bell", description: Text(AnimeL10n.key(.notificationEmptyMessage)))
                .frame(maxWidth: .infinity, minHeight: 220)
        } else {
            ForEach(model.notifications) { notification in
                NativeNotificationRow(notification: notification, destination: notificationDestination(for: notification)) {
                    model.markNotificationRead(id: notification.id)
                }
            }
        }
    }

    private func loadCurrentMode() {
        errorMessage = nil
        if selectedMode == "notifications" {
            model.loadNotifications { _, error in errorMessage = error }
        } else {
            model.loadActivity(feed: selectedFeed) { _, error in errorMessage = error }
        }
    }

    private func activityDestination(for item: NativeActivityItemSnapshot) -> AnyView? {
        if let subject = item.subjectSummary {
            return AnyView(NativeSubjectDetailView(summary: subject, model: model))
        }
        if let reviewId = item.reviewId {
            return AnyView(NativeReviewDetailView(reviewId: reviewId, model: model))
        }
        if let listId = item.listId {
            return AnyView(NativeListDetailView(listId: listId, model: model))
        }
        if let actorId = item.actorId {
            return AnyView(NativeUserProfileView(userId: actorId, model: model))
        }
        return nil
    }

    private func isDeletable(_ item: NativeActivityItemSnapshot) -> Bool {
        item.owned
    }

    private func deleteActivity() {
        guard let item = activityToDelete else { return }
        let finish: (String?) -> Void = { error in
            if let error { activityMessage = "\(AnimeL10n.string(.errorActionFailed))：\(error)" }
            else { activityMessage = AnimeL10n.string(.activityRetract); loadCurrentMode() }
            activityToDelete = nil
        }
        model.withdrawActivity(id: item.id, completion: finish)
    }

    private func notificationDestination(for notification: NativeNotificationSnapshot) -> AnyView? {
        if let reviewId = notification.reviewId {
            return AnyView(NativeReviewDetailView(reviewId: reviewId, model: model))
        }
        if let commentId = notification.commentId, let subjectId = notification.subjectId {
            return AnyView(
                NativeSubjectDetailView(
                    summary: NativeSubjectSummary.placeholder(id: subjectId, title: AnimeL10n.string(.discussion)),
                    model: model,
                    focusCommentId: commentId,
                )
            )
        }
        if let listId = notification.listId {
            return AnyView(NativeListDetailView(listId: listId, model: model))
        }
        if let actorId = notification.actorId {
            return AnyView(NativeUserProfileView(userId: actorId, model: model))
        }
        return nil
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            if selectedMode == "notifications" {
                model.loadNotifications { _, error in errorMessage = error; continuation.resume() }
            } else {
                model.loadActivity(feed: selectedFeed) { _, error in errorMessage = error; continuation.resume() }
            }
        }
    }
}

