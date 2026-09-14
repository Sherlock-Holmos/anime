import SwiftUI
import Foundation

struct NativeSearchDiscoverySnapshot: Codable {
    let trending: [String]
    let recommendations: [NativeSubjectSummary]
    let personalized: Bool
}

struct NativeSearchResultsSnapshot: Codable {
    let items: [NativeSubjectSummary]
    let nextCursor: String?
    let hasMore: Bool
}

struct NativeCollectionPageSnapshot: Codable {
    let items: [NativeCollectionItemSnapshot]
    let nextCursor: String?
}

struct NativeCollectionItemSnapshot: Codable, Identifiable {
    let subjectId: Int64
    let title: String
    let originalTitle: String
    let posterUrl: String?
    let airDate: String?
    let score: Double
    let status: String
    let userRating: Int
    let comment: String
    let episodeProgress: Int
    let totalEpisodes: Int
    let updatedAt: String

    var id: Int64 { subjectId }
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeActivityPageSnapshot: Codable {
    let items: [NativeActivityItemSnapshot]
    let nextCursor: String?
}

struct NativeActivityItemSnapshot: Codable, Identifiable {
    let id: String
    let actorId: String?
    let actorName: String
    let actorAvatarUrl: String?
    let kind: String
    let subjectId: Int64?
    let subjectTitle: String?
    let posterUrl: String?
    let reviewId: String?
    let listId: String?
    let summary: String
    let occurredAt: String

    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeNotificationSnapshot: Codable, Identifiable {
    let id: String
    let kind: String
    let actorId: String?
    let actorName: String?
    let subjectId: Int64?
    let commentId: String?
    let listId: String?
    let readAt: String?
    let createdAt: String
    let reviewId: String?
}

struct NativeProfileSnapshot: Codable {
    let userId: String
    let displayName: String
    let avatarUrl: String?
    let provider: String
    let reviewCount: Int
    let ratingCount: Int
    let listCount: Int
    let wishCount: Int
    let watchingCount: Int
    let completedCount: Int
    let onHoldCount: Int
    let droppedCount: Int
    let syncedAt: String?

    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

struct NativeDiagnosticSnapshot: Codable, Identifiable {
    let endpoint: String
    let statusCode: Int?
    let healthy: Bool
    let body: String

    var id: String { endpoint }
}

struct NativeSubjectCommunitySnapshot: Codable {
    let rating: NativeRatingSnapshot
    let reviews: [NativeReviewSnapshot]
    let comments: [NativeCommentSnapshot]
}

struct NativeCalendarSnapshot: Codable {
    let date: String
    let items: [NativeSubjectSummary]
    let generatedAtEpochSeconds: Int64
}

struct NativeSubjectSectionsSnapshot: Codable {
    let episodes: [NativeEpisodeSnapshot]
    let characters: [NativeCharacterSnapshot]
    let persons: [NativePersonSnapshot]
    let relations: [NativeRelationSnapshot]
}

struct NativeEpisodeSnapshot: Codable, Identifiable {
    let id: Int64
    let number: Double?
    let title: String?
    let originalTitle: String?
    let type: String
    let airDate: String?
    let airStatus: String
}

struct NativeCharacterSnapshot: Codable, Identifiable {
    let id: Int64
    let name: String
    let imageUrl: String?
    let relation: String
    let actors: [NativePersonSnapshot]

    var imageURL: URL? { imageUrl.flatMap(URL.init(string:)) }
}

struct NativePersonSnapshot: Codable, Identifiable {
    let id: Int64
    let name: String
    let imageUrl: String?
    let role: String?

    var imageURL: URL? { imageUrl.flatMap(URL.init(string:)) }
}

struct NativeRelationSnapshot: Codable, Identifiable {
    let subject: NativeSubjectSummary
    let kind: String
    let label: String

    var id: Int64 { subject.id }
}

struct NativeRatingSnapshot: Codable {
    let score: Double?
    let votes: Int64
}

struct NativeReviewSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64
    let authorId: String
    let kind: String
    let title: String?
    let body: String
    let spoiler: Bool
    let likeCount: Int64
    let createdAt: String
    let owned: Bool
    let bookmarkCount: Int64
    let editedAt: String?
    let visibility: String
}

struct NativeCommentSnapshot: Codable, Identifiable {
    let id: String
    let parentId: String?
    let authorId: String
    let authorName: String
    let body: String
    let spoiler: Bool
    let createdAt: String
    let owned: Bool
    let likeCount: Int64
    let bookmarkCount: Int64
}

struct NativeReviewPageSnapshot: Codable {
    let items: [NativeReviewSnapshot]
    let nextCursor: String?
}

struct NativeUserProfileSnapshot: Codable {
    let id: String
    let displayName: String
    let avatarUrl: String?
    let createdAt: String
    let reviewCount: Int64
    let ratingCount: Int64
    let listCount: Int64
    let followerCount: Int64
    let followingCount: Int64
    let following: Bool

    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

struct NativeListSummarySnapshot: Codable, Identifiable {
    let id: String
    let ownerId: String?
    let ownerName: String
    let title: String
    let description: String
    let itemCount: Int64
    let followerCount: Int64
    let updatedAt: String
    let owned: Bool
    let following: Bool
}

struct NativeListItemSnapshot: Codable, Identifiable {
    let subjectId: Int64
    let title: String
    let posterUrl: String?
    let note: String?
    let position: Int
    let score: Double?

    var id: Int64 { subjectId }
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeListDetailSnapshot: Codable {
    let summary: NativeListSummarySnapshot
    let items: [NativeListItemSnapshot]
}

struct NativeFollowSnapshot: Codable {
    let following: Bool
}

struct NativeSyncStatusSnapshot: Codable {
    let pendingCount: Int64
    let failedCount: Int64
    let conflictCount: Int64
    let lastSuccessfulAt: String?
    let bangumiLinked: Bool
}

struct NativeSyncConflictSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64
    let localVersion: Int64
    let fieldName: String
    let localValue: String
    let remoteValue: String
    let detectedAt: String
}

struct NativeReactionSnapshot: Codable {
    let reaction: String
    let active: Bool
    let likeCount: Int64
    let bookmarkCount: Int64
}

struct NativeLibraryView: View {
    @ObservedObject var model: NativeAppModel
    @State private var query = ""
    @State private var results: [NativeSubjectSummary] = []
    @State private var nextCursor: String?
    @State private var isSearching = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        recommendationContent
                    } else if isSearching && results.isEmpty {
                        ProgressView("正在搜索")
                            .frame(maxWidth: .infinity, minHeight: 180)
                    } else if results.isEmpty {
                        ContentUnavailableView.search(text: query)
                            .frame(maxWidth: .infinity, minHeight: 220)
                    } else {
                        resultGrid
                    }
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollEdgeEffectStyle(.soft, for: .top)
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "搜索作品")
            .onSubmit(of: .search, submitSearch)
            .refreshable { await refreshRecommendations() }
            .navigationTitle("资料库")
            .navigationBarTitleDisplayMode(.large)
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
        }
        .task {
            model.start()
            if model.searchDiscovery == nil { model.loadSearchDiscovery() }
        }
    }

    @ViewBuilder
    private var recommendationContent: some View {
        if let discovery = model.searchDiscovery {
            if !discovery.trending.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("大家正在搜")
                        .font(.title3.weight(.bold))
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(discovery.trending, id: \.self) { term in
                                Button(term) {
                                    query = term
                                    submitSearch()
                                }
                                .buttonStyle(.bordered)
                                .tint(.accentColor)
                            }
                        }
                    }
                }
            }
            if !discovery.recommendations.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(discovery.personalized ? "为你推荐" : "精选作品")
                        .font(.title3.weight(.bold))
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 145), spacing: 12)], spacing: 18) {
                        ForEach(discovery.recommendations) { subject in
                            NavigationLink(value: subject) {
                                NativeCatalogCard(subject: subject)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        } else {
            ProgressView()
                .frame(maxWidth: .infinity, minHeight: 180)
        }
    }

    private var resultGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("搜索结果")
                .font(.title3.weight(.bold))
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 145), spacing: 12)], spacing: 18) {
                ForEach(results) { subject in
                    NavigationLink(value: subject) {
                        NativeCatalogCard(subject: subject)
                    }
                    .buttonStyle(.plain)
                }
            }
            if let nextCursor {
                Button("加载更多") { loadMore(cursor: nextCursor) }
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.bordered)
            }
        }
    }

    private func submitSearch() {
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return }
        isSearching = true
        errorMessage = nil
        model.search(query: normalized) { snapshot, error in
            results = snapshot?.items ?? []
            nextCursor = snapshot?.nextCursor
            errorMessage = error
            isSearching = false
            if error == nil { model.saveSearchHistory(query: normalized) }
        }
    }

    private func loadMore(cursor: String) {
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return }
        isSearching = true
        model.search(query: normalized, cursor: cursor) { snapshot, error in
            if let snapshot {
                results.append(contentsOf: snapshot.items)
                nextCursor = snapshot.nextCursor
            }
            errorMessage = error
            isSearching = false
        }
    }

    private func refreshRecommendations() async {
        await withCheckedContinuation { continuation in
            model.loadSearchDiscovery { _ in continuation.resume() }
        }
    }
}

struct NativeCollectionView: View {
    @ObservedObject var model: NativeAppModel
    @State private var selectedStatus = "Watching"
    @State private var errorMessage: String?

    private let filters = ["Watching", "Wish", "Completed", "OnHold", "Dropped"]

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 20) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(filters, id: \.self) { filter in
                                Button(filter.displayName) {
                                    selectedStatus = filter
                                    loadCollection(filter)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(selectedStatus == filter ? .accentColor : .gray.opacity(0.35))
                            }
                        }
                    }
                    if let errorMessage {
                        NativeInlineError(message: errorMessage) { loadCollection(selectedStatus) }
                    }
                    if let page = model.collectionPage, !page.items.isEmpty {
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 155), spacing: 12)], spacing: 18) {
                            ForEach(page.items) { item in
                                NavigationLink {
                                    NativeSubjectDetailView(summary: item.subjectSummary, model: model)
                                } label: {
                                    NativeCollectionCard(item: item)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        if let cursor = page.nextCursor {
                            Button("加载更多") {
                                model.loadCollection(status: selectedStatus, cursor: cursor, append: true) { _, error in
                                    errorMessage = error
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .buttonStyle(.bordered)
                        }
                    } else if model.session.status == "restoring" {
                        ProgressView("正在恢复片库")
                            .frame(maxWidth: .infinity, minHeight: 220)
                    } else {
                        ContentUnavailableView("还没有作品", systemImage: "books.vertical", description: Text("在作品详情中收藏后，会显示在这里。"))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollEdgeEffectStyle(.soft, for: .top)
            .navigationTitle("片库")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await withCheckedContinuation { continuation in
                    model.loadCollection(status: selectedStatus) { _, error in
                        errorMessage = error
                        continuation.resume()
                    }
                }
            }
            .task {
                model.start()
                if model.collectionPage == nil { loadCollection(selectedStatus) }
            }
        }
    }

    private func loadCollection(_ status: String) {
        errorMessage = nil
        model.loadCollection(status: status) { _, error in
            errorMessage = error
        }
    }
}

struct NativeCalendarView: View {
    @ObservedObject var model: NativeAppModel
    @State private var selectedDate = Date()
    @State private var errorMessage: String?

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                DatePicker("播出日期", selection: $selectedDate, displayedComponents: .date)
                    .datePickerStyle(.compact)
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                } else if let calendar = model.calendar, calendar.date == dateString {
                    if calendar.items.isEmpty {
                        ContentUnavailableView("当天没有播出", systemImage: "calendar.badge.clock", description: Text("可以切换到其他日期查看。"))
                            .frame(maxWidth: .infinity, minHeight: 180)
                    } else {
                        ForEach(calendar.items) { subject in
                            NavigationLink {
                                NativeSubjectDetailView(summary: subject, model: model)
                            } label: {
                                NativeCalendarRow(subject: subject)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                } else {
                    ProgressView("正在加载日历")
                        .frame(maxWidth: .infinity, minHeight: 180)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("播出日历")
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .onChange(of: selectedDate) { _, _ in load() }
        .refreshable { await refresh() }
    }

    private var dateString: String { Self.dateFormatter.string(from: selectedDate) }

    private func load() {
        errorMessage = nil
        model.loadCalendar(date: dateString) { _, error in errorMessage = error }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadCalendar(date: dateString) { _, error in
                errorMessage = error
                continuation.resume()
            }
        }
    }
}

private struct NativeCalendarRow: View {
    let subject: NativeSubjectSummary

    var body: some View {
        HStack(spacing: 14) {
            NativeRemoteImage(url: subject.posterURL)
                .frame(width: 58, height: 78)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            VStack(alignment: .leading, spacing: 5) {
                Text(subject.title)
                    .font(.headline)
                    .lineLimit(2)
                Text(subject.originalTitle ?? subject.type.displayName)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                if let rating = subject.rating {
                    Label(String(format: "%.1f", rating), systemImage: "star.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.orange)
                }
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

struct NativeSubjectSectionsView: View {
    let subjectId: Int64
    @ObservedObject var model: NativeAppModel
    @State private var selectedSection = "episodes"
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                Picker("资料类型", selection: $selectedSection) {
                    Text("分集").tag("episodes")
                    Text("角色").tag("characters")
                    Text("关联").tag("relations")
                }
                .pickerStyle(.segmented)

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load(force: true) }
                } else if let sections = model.subjectSections[subjectId] {
                    sectionContent(sections)
                } else {
                    ProgressView("正在加载作品资料")
                        .frame(maxWidth: .infinity, minHeight: 220)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("作品资料")
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .refreshable { await refresh() }
    }

    @ViewBuilder
    private func sectionContent(_ sections: NativeSubjectSectionsSnapshot) -> some View {
        switch selectedSection {
        case "characters":
            if sections.characters.isEmpty {
                ContentUnavailableView("暂无角色资料", systemImage: "person.2")
            } else {
                ForEach(sections.characters) { character in
                    HStack(spacing: 12) {
                        NativeRemoteImage(url: character.imageURL)
                            .frame(width: 54, height: 54)
                            .clipShape(Circle())
                        VStack(alignment: .leading, spacing: 4) {
                            Text(character.name).font(.headline)
                            Text(character.relation)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            if !character.actors.isEmpty {
                                Text("配音：" + character.actors.map(\.name).joined(separator: "、"))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        Spacer()
                    }
                    .padding(14)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
            }
        case "relations":
            if sections.relations.isEmpty {
                ContentUnavailableView("暂无关联作品", systemImage: "link")
            } else {
                ForEach(sections.relations) { relation in
                    NavigationLink {
                        NativeSubjectDetailView(summary: relation.subject, model: model)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(relation.label.isEmpty ? relation.kind : relation.label)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.tint)
                                Text(relation.subject.title)
                                    .font(.headline)
                                    .foregroundStyle(.primary)
                                    .lineLimit(2)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(.tertiary)
                        }
                        .padding(14)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        default:
            if sections.episodes.isEmpty {
                ContentUnavailableView("暂无分集资料", systemImage: "list.number")
            } else {
                ForEach(sections.episodes) { episode in
                    HStack(spacing: 12) {
                        Text(episode.number.map { String(format: "%g", $0) } ?? "—")
                            .font(.headline.monospacedDigit())
                            .frame(width: 42)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(episode.title ?? "未命名分集")
                                .font(.headline)
                                .lineLimit(2)
                            Text([episode.airDate, episode.airStatus.displayName].compactMap { $0 }.joined(separator: " · "))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(14)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
            }
        }
    }

    private func load(force: Bool = false) {
        errorMessage = nil
        model.loadSubjectSections(id: subjectId, force: force) { _, error in errorMessage = error }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadSubjectSections(id: subjectId, force: true) { _, error in
                errorMessage = error
                continuation.resume()
            }
        }
    }
}

struct NativeActivityView: View {
    @ObservedObject var model: NativeAppModel
    @State private var selectedMode = "动态"
    @State private var selectedFeed = "public"
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    Picker("内容", selection: $selectedMode) {
                        Text("动态").tag("动态")
                        Text("通知").tag("通知")
                    }
                    .pickerStyle(.segmented)
                    if selectedMode == "动态" {
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
            .scrollEdgeEffectStyle(.soft, for: .top)
            .refreshable { await refresh() }
            .navigationTitle("动态")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeListsView(model: model)
                    } label: {
                        Image(systemName: "list.bullet.rectangle")
                    }
                    .accessibilityLabel("片单")
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
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
        if let errorMessage {
            NativeInlineError(message: errorMessage) { loadCurrentMode() }
        } else if let page = model.activityPage, !page.items.isEmpty {
            ForEach(page.items) { item in
                NativeActivityCard(item: item, destination: activityDestination(for: item))
            }
            if page.nextCursor != nil {
                Button("加载更多") {
                    model.loadActivity(feed: selectedFeed, cursor: page.nextCursor, append: true)
                }
                .frame(maxWidth: .infinity)
                .buttonStyle(.bordered)
            }
        } else {
            ContentUnavailableView("还没有动态", systemImage: "bubble.left.and.bubble.right", description: Text("完成一次评分、评价或讨论后，内容会显示在这里。"))
                .frame(maxWidth: .infinity, minHeight: 220)
        }
    }

    @ViewBuilder
    private var notificationContent: some View {
        if model.notifications.isEmpty {
            ContentUnavailableView("没有新通知", systemImage: "bell", description: Text("新的互动会在这里提醒你。"))
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
        if selectedMode == "通知" {
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

    private func notificationDestination(for notification: NativeNotificationSnapshot) -> AnyView? {
        if let reviewId = notification.reviewId {
            return AnyView(NativeReviewDetailView(reviewId: reviewId, model: model))
        }
        if let commentId = notification.commentId, let subjectId = notification.subjectId {
            return AnyView(NativeSubjectDetailView(summary: NativeSubjectSummary.placeholder(id: subjectId, title: "讨论"), model: model))
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
            if selectedMode == "通知" {
                model.loadNotifications { _, error in errorMessage = error; continuation.resume() }
            } else {
                model.loadActivity(feed: selectedFeed) { _, error in errorMessage = error; continuation.resume() }
            }
        }
    }
}

struct NativeProfileView: View {
    @ObservedObject var model: NativeAppModel
    @State private var showingAccount = false
    @State private var message: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    if model.session.status == "authenticated" {
                        authenticatedContent
                    } else if model.session.status == "restoring" {
                        ProgressView("正在恢复账号")
                            .frame(maxWidth: .infinity, minHeight: 180)
                    } else {
                        guestContent
                    }
                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(message.contains("失败") ? .red : .secondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollEdgeEffectStyle(.soft, for: .top)
            .navigationTitle("我的")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeSettingsView(model: model)
                    } label: {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel("设置")
                }
            }
            .sheet(isPresented: $showingAccount) {
                NativeAccountSheet(model: model)
                    .presentationDetents([.medium, .large])
            }
        }
        .task {
            model.start()
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
                Text(model.profile?.displayName ?? model.session.displayName ?? "Anime 用户")
                    .font(.title3.weight(.bold))
                Text("已登录 · \(model.profile?.provider ?? "Anime")")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(18)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))

        if let profile = model.profile {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                NativeMetric(value: profile.watchingCount + profile.completedCount, title: "收藏")
                NativeMetric(value: profile.ratingCount, title: "评分")
                NativeMetric(value: profile.reviewCount, title: "评价")
            }
        }

        NavigationLink {
            NativeCollectionView(model: model)
        } label: {
            NativeActionRow(title: "我的片库", subtitle: "浏览正在追、想看和已完成的作品", systemImage: "books.vertical")
        }
        .buttonStyle(.plain)

        Button {
            model.logout { error in message = error ?? "已退出登录" }
        } label: {
            NativeActionRow(title: "退出登录", subtitle: "清除本机凭据并返回访客状态", systemImage: "rectangle.portrait.and.arrow.right", tint: .red)
        }
        .buttonStyle(.plain)
    }

    private var guestContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 42))
                .foregroundStyle(.tint)
            Text("登录 Anime")
                .font(.title2.weight(.bold))
            Text("登录后可同步片库、评分、评价和社区互动。")
                .foregroundStyle(.secondary)
            Button("登录或注册") { showingAccount = true }
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

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                if isLoading && review == nil {
                    ProgressView("正在加载评价")
                        .frame(maxWidth: .infinity, minHeight: 220)
                } else if let review {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 5) {
                                Text(review.title ?? "作品评价")
                                    .font(.title2.weight(.bold))
                                Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(16))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if review.spoiler {
                                Label("剧透", systemImage: "eye.slash")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.orange)
                            }
                        }
                        Text(review.body)
                            .font(.body)
                            .fixedSize(horizontal: false, vertical: true)
                        HStack(spacing: 18) {
                            Button {
                                react("like")
                            } label: {
                                Label("\(review.likeCount)", systemImage: isLiked ? "heart.fill" : "heart")
                            }
                            Button {
                                react("bookmark")
                            } label: {
                                Label("\(review.bookmarkCount)", systemImage: isBookmarked ? "bookmark.fill" : "bookmark")
                            }
                        }
                        .foregroundStyle(.tint)
                    }
                    .padding(18)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))

                    NavigationLink {
                        NativeSubjectDetailView(summary: NativeSubjectSummary.placeholder(id: review.subjectId, title: "作品"), model: model)
                    } label: {
                        NativeActionRow(title: "查看所属作品", subtitle: "打开作品详情和社区讨论", systemImage: "film")
                    }
                    .buttonStyle(.plain)

                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(message.hasPrefix("操作失败") ? .red : .secondary)
                    }
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("评价")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let review, review.owned {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("编辑") { showingEditor = true }
                        Button("删除评价", role: .destructive) { showingDeleteConfirmation = true }
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
                    if let updated { self.review = updated; message = "评价已更新" }
                    if let error { message = "更新失败：\(error)" }
                }
            }
        }
        .confirmationDialog("确定删除这条评价吗？", isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button("删除评价", role: .destructive) {
                model.deleteReview(id: reviewId) { error in
                    if let error { message = "删除失败：\(error)" }
                    else { message = "评价已删除"; review = nil }
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
        let active = reaction == "like" ? !isLiked : !isBookmarked
        model.reactReview(id: reviewId, reaction: reaction, active: active) { value, error in
            if let value {
                if reaction == "like" { isLiked = value.active }
                if reaction == "bookmark" { isBookmarked = value.active }
                message = "操作已完成"
            } else if let error {
                message = "操作失败：\(error)"
            }
        }
    }
}

private struct NativeReviewEditor: View {
    let review: NativeReviewSnapshot
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeReviewSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var reviewText: String
    @State private var spoiler: Bool
    @State private var visibility: String
    @State private var isSaving = false

    init(review: NativeReviewSnapshot, model: NativeAppModel, onComplete: @escaping (NativeReviewSnapshot?, String?) -> Void) {
        self.review = review
        self.model = model
        self.onComplete = onComplete
        _title = State(initialValue: review.title ?? "")
        _reviewText = State(initialValue: review.body)
        _spoiler = State(initialValue: review.spoiler)
        _visibility = State(initialValue: review.visibility)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("标题", text: $title)
                TextEditor(text: $reviewText).frame(minHeight: 170)
                Toggle("包含剧透", isOn: $spoiler)
                Picker("可见性", selection: $visibility) {
                    Text("公开").tag("public")
                    Text("仅自己").tag("private")
                }
                Button(isSaving ? "保存中…" : "保存修改") { save() }
                    .disabled(isSaving || reviewText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle("编辑评价")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
        }
    }

    private func save() {
        isSaving = true
        model.updateReview(
            id: review.id,
            title: title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : title,
            body: reviewText.trimmingCharacters(in: .whitespacesAndNewlines),
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
                            Text("加入于 \(profile.createdAt.prefix(10))")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button(isFollowing ? "已关注" : "关注") {
                            model.followUser(id: userId, following: !isFollowing) { value, error in
                                if let value { isFollowing = value }
                                message = error.map { "操作失败：\($0)" }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                        NativeMetric(value: Int(profile.reviewCount), title: "评价")
                        NativeMetric(value: Int(profile.ratingCount), title: "评分")
                        NativeMetric(value: Int(profile.followerCount), title: "粉丝")
                    }

                    if let reviews = model.userReviews[userId]?.items, !reviews.isEmpty {
                        Text("评价").font(.title3.weight(.bold))
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
                        Text("片单").font(.title3.weight(.bold))
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
                    ProgressView("正在加载用户")
                        .frame(maxWidth: .infinity, minHeight: 220)
                } else if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("用户")
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .refreshable { await refresh() }
    }

    private func load() {
        isLoading = true
        errorMessage = nil
        var remaining = 2
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
            Text(review.title ?? "作品评价").font(.headline)
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

private struct NativeListSummaryRow: View {
    let list: NativeListSummarySnapshot

    var body: some View {
        HStack {
            Image(systemName: "list.bullet.rectangle.portrait")
                .font(.title2)
                .foregroundStyle(.tint)
            VStack(alignment: .leading, spacing: 4) {
                Text(list.title).font(.headline)
                Text("\(list.itemCount) 部作品 · \(list.followerCount) 人关注")
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
                ContentUnavailableView("还没有片单", systemImage: "list.bullet.rectangle", description: Text("创建片单来整理喜欢的作品。"))
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
        .navigationTitle("片单")
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
                        Text(detail.summary.description.isEmpty ? "暂无描述" : detail.summary.description)
                            .foregroundStyle(.secondary)
                        Text("创建者：\(detail.summary.ownerName) · \(detail.summary.itemCount) 部作品")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                        HStack {
                            Button(isFollowing ? "已关注" : "关注片单") {
                                model.followList(id: listId, following: !isFollowing) { value, error in
                                    if let value { isFollowing = value }
                                    message = error.map { "操作失败：\($0)" }
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            if let ownerId = detail.summary.ownerId {
                                NavigationLink("查看创建者") {
                                    NativeUserProfileView(userId: ownerId, model: model)
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    }
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

                    if detail.items.isEmpty {
                        ContentUnavailableView("片单暂无作品", systemImage: "books.vertical")
                    } else {
                        ForEach(detail.items) { item in
                            NavigationLink {
                                NativeSubjectDetailView(summary: NativeSubjectSummary.placeholder(id: item.subjectId, title: item.title), model: model)
                            } label: {
                                HStack(spacing: 12) {
                                    NativeRemoteImage(url: item.posterURL)
                                        .frame(width: 54, height: 74)
                                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
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
                    ProgressView("正在加载片单").frame(maxWidth: .infinity, minHeight: 220)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle("片单")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if detail?.summary.owned == true {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("编辑") { showingEditor = true }
                        Button("删除片单", role: .destructive) { showingDeleteConfirmation = true }
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
        .confirmationDialog("确定删除这个片单吗？", isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button("删除片单", role: .destructive) {
                model.deleteList(id: listId) { error in
                    message = error.map { "删除失败：\($0)" } ?? "片单已删除"
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
                TextField("片单名称", text: $title)
                TextField("描述（可选）", text: $description, axis: .vertical)
                    .lineLimit(2...5)
                Picker("可见性", selection: $visibility) {
                    Text("公开").tag("public")
                    Text("仅自己").tag("private")
                }
                Section("作品") {
                    TextField("作品 ID，逗号分隔", text: $subjectIdsText, axis: .vertical)
                        .keyboardType(.numbersAndPunctuation)
                    Text("可以在作品详情或 Bangumi 中查看作品 ID。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Button(isSaving ? "保存中…" : (existing == nil ? "创建片单" : "保存修改")) { save() }
                    .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle(existing == nil ? "新建片单" : "编辑片单")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
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

struct NativeSubjectCommunityView: View {
    let subjectId: Int64
    let community: NativeSubjectCommunitySnapshot?
    let errorMessage: String?
    @ObservedObject var model: NativeAppModel
    let onReload: () -> Void

    @State private var selectedScore = 0
    @State private var selectedCollectionStatus: String?
    @State private var commentText = ""
    @State private var commentSpoiler = false
    @State private var isSubmitting = false
    @State private var isSavingRating = false
    @State private var isSavingCollection = false
    @State private var localComments: [NativeCommentSnapshot] = []
    @State private var localReviews: [NativeReviewSnapshot] = []
    @State private var commentReactions: [String: NativeReactionSnapshot] = [:]
    @State private var reviewReactions: [String: NativeReactionSnapshot] = [:]
    @State private var message: String?
    @State private var showingReviewComposer = false
    @State private var editingComment: NativeCommentSnapshot?
    @State private var deletingCommentId: String?

    private let collectionStatuses = [
        ("Watching", "在看"),
        ("Wish", "想看"),
        ("Completed", "看过"),
        ("OnHold", "搁置"),
        ("Dropped", "抛弃"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("社区")
                        .font(.title2.weight(.bold))
                    Text("评分、评价和讨论")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button(action: onReload) {
                    Image(systemName: "arrow.clockwise")
                }
                .accessibilityLabel("刷新社区内容")
            }

            if let errorMessage {
                NativeInlineError(message: errorMessage, retry: onReload)
            } else if let community {
                ratingAndCollection(community.rating)
                reviewSection(community.reviews + localReviews)
                commentSection(community.comments)
            } else {
                ProgressView("正在加载社区内容")
                    .frame(maxWidth: .infinity, minHeight: 100)
            }

            if let message {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(message.contains("失败") ? .red : .secondary)
            }
        }
        .onChange(of: community?.comments.count ?? 0) { _, _ in
            localComments.removeAll()
        }
        .onChange(of: community?.reviews.count ?? 0) { _, _ in
            localReviews.removeAll()
        }
        .sheet(isPresented: $showingReviewComposer) {
            NativeReviewComposer(subjectId: subjectId, model: model) { review, error in
                if let review {
                    localReviews.insert(review, at: 0)
                    message = "评价已发布"
                } else if let error {
                    message = "发布失败：\(error)"
                }
            }
            .presentationDetents([.medium, .large])
        }
        .sheet(item: $editingComment) { comment in
            NativeCommentEditor(comment: comment, model: model) { _, error in
                if let error { message = "更新失败：\(error)" } else { message = "讨论已更新"; onReload() }
            }
        }
        .confirmationDialog("确定删除这条讨论吗？", isPresented: Binding(
            get: { deletingCommentId != nil },
            set: { if !$0 { deletingCommentId = nil } },
        ), titleVisibility: .visible) {
            Button("删除讨论", role: .destructive) {
                guard let deletingCommentId else { return }
                model.deleteComment(id: deletingCommentId) { error in
                    message = error.map { "删除失败：\($0)" } ?? "讨论已删除"
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
                Text(rating.score.map { String(format: "%.1f", $0) } ?? "暂无评分")
                    .font(.title3.weight(.bold))
                Text("· \(rating.votes) 人评分")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
            }

            HStack(spacing: 10) {
                Menu {
                    ForEach(1...10, id: \.self) { score in
                        Button("评分 \(score)") {
                            selectedScore = score
                            saveRating(score)
                        }
                    }
                } label: {
                    Label(
                        selectedScore == 0 ? "给作品评分" : "我的评分 \(selectedScore)",
                        systemImage: "star",
                    )
                }
                .buttonStyle(.borderedProminent)
                .disabled(isSavingRating)

                Menu {
                    ForEach(collectionStatuses, id: \.0) { status in
                        Button(status.1) { saveCollection(status.0) }
                    }
                    Divider()
                    Button("移出片库", role: .destructive) { saveCollection(nil) }
                } label: {
                    Label(selectedCollectionStatus?.displayName ?? "收藏", systemImage: "plus.circle")
                }
                .buttonStyle(.bordered)
                .disabled(isSavingCollection)
            }
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    @ViewBuilder
    private func reviewSection(_ reviews: [NativeReviewSnapshot]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("评价")
                    .font(.title3.weight(.bold))
                Spacer()
                Button("写评价") { showingReviewComposer = true }
                    .font(.subheadline.weight(.semibold))
                Text("\(reviews.count)")
                    .foregroundStyle(.secondary)
            }

            if reviews.isEmpty {
                Text("还没有评价，成为第一个分享感受的人吧。")
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
                Text(review.title ?? "作品评价")
                    .font(.headline)
                Spacer()
                Text(review.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            if review.spoiler {
                Label("包含剧透", systemImage: "eye.slash")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.orange)
            }
            Text(review.body)
                .font(.body)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: 16) {
                reactionButton(
                    title: "\(reviewReactions[review.id]?.likeCount ?? review.likeCount)",
                    systemImage: reviewReactions[review.id]?.active == true ? "heart.fill" : "heart",
                    tint: reviewReactions[review.id]?.active == true ? .pink : .secondary,
                ) {
                    let active = reviewReactions[review.id]?.active != true
                    model.reactReview(id: review.id, reaction: "like", active: active) { reaction, error in
                        if let reaction { reviewReactions[review.id] = reaction }
                        message = error.map { "操作失败：\($0)" }
                    }
                }
                reactionButton(
                    title: "\(reviewReactions[review.id]?.bookmarkCount ?? review.bookmarkCount)",
                    systemImage: "bookmark",
                    tint: .secondary,
                ) {
                    let active = reviewReactions[review.id]?.active != true
                    model.reactReview(id: review.id, reaction: "bookmark", active: active) { reaction, error in
                        if let reaction { reviewReactions[review.id] = reaction }
                        message = error.map { "操作失败：\($0)" }
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
                Text("讨论")
                    .font(.title3.weight(.bold))
                Spacer()
                Text("\(comments.count + localComments.count)")
                    .foregroundStyle(.secondary)
            }

            VStack(spacing: 10) {
                TextEditor(text: $commentText)
                    .frame(minHeight: 84)
                    .padding(8)
                    .scrollContentBackground(.hidden)
                    .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(alignment: .topLeading) {
                        if commentText.isEmpty {
                            Text("说点什么…")
                                .foregroundStyle(.tertiary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 16)
                                .allowsHitTesting(false)
                        }
                    }
                HStack {
                    Toggle("剧透", isOn: $commentSpoiler)
                        .font(.caption)
                    Spacer()
                    Button(isSubmitting ? "发布中…" : "发布") { submitComment() }
                        .buttonStyle(.borderedProminent)
                        .disabled(isSubmitting || commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }

            let visibleComments = comments + localComments
            if visibleComments.isEmpty {
                Text("还没有讨论，欢迎开启话题。")
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 8)
            } else {
                ForEach(visibleComments) { comment in
                    commentCard(comment)
                }
            }
        }
    }

    private func commentCard(_ comment: NativeCommentSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(comment.authorName)
                    .font(.headline)
                if comment.owned {
                    Text("我")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tint)
                }
                Spacer()
                Text(comment.createdAt.replacingOccurrences(of: "T", with: " ").prefix(10))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Menu {
                    if comment.owned {
                        Button("编辑") { editingComment = comment }
                        Button("删除", role: .destructive) { deletingCommentId = comment.id }
                    } else {
                        Button("举报") {
                            model.reportComment(id: comment.id) { error in
                                message = error.map { "举报失败：\($0)" } ?? "举报已提交"
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
                reactionButton(
                    title: "\(commentReactions[comment.id]?.likeCount ?? comment.likeCount)",
                    systemImage: commentReactions[comment.id]?.active == true ? "heart.fill" : "heart",
                    tint: commentReactions[comment.id]?.active == true ? .pink : .secondary,
                ) {
                    let active = commentReactions[comment.id]?.active != true
                    model.reactComment(id: comment.id, reaction: "like", active: active) { reaction, error in
                        if let reaction { commentReactions[comment.id] = reaction }
                        message = error.map { "操作失败：\($0)" }
                    }
                }
                reactionButton(
                    title: "\(commentReactions[comment.id]?.bookmarkCount ?? comment.bookmarkCount)",
                    systemImage: "bookmark",
                    tint: .secondary,
                ) {
                    let active = commentReactions[comment.id]?.active != true
                    model.reactComment(id: comment.id, reaction: "bookmark", active: active) { reaction, error in
                        if let reaction { commentReactions[comment.id] = reaction }
                        message = error.map { "操作失败：\($0)" }
                    }
                }
            }
            .font(.caption.weight(.medium))
        }
        .padding(14)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func reactionButton(
        title: String,
        systemImage: String,
        tint: Color,
        action: @escaping () -> Void,
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .foregroundStyle(tint)
        }
        .buttonStyle(.plain)
    }

    private func saveRating(_ score: Int) {
        isSavingRating = true
        message = nil
        model.saveRating(subjectId: subjectId, score: score) { error in
            isSavingRating = false
            message = error.map { "评分失败：\($0)" } ?? "评分已保存"
            if error == nil { onReload() }
        }
    }

    private func saveCollection(_ status: String?) {
        isSavingCollection = true
        message = nil
        model.setCollection(subjectId: subjectId, status: status) { error in
            isSavingCollection = false
            if error == nil {
                selectedCollectionStatus = status
                message = status.map { "已加入\($0.displayName)" } ?? "已移出片库"
            } else if let error {
                message = "收藏失败：\(error)"
            }
        }
    }

    private func submitComment() {
        let body = commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty else { return }
        isSubmitting = true
        message = nil
        model.createCommentAdvanced(subjectId: subjectId, body: body, spoiler: commentSpoiler, parentId: nil) { comment, error in
            isSubmitting = false
            if let comment {
                localComments.insert(comment, at: 0)
                commentText = ""
                commentSpoiler = false
                message = "讨论已发布"
            } else if let error {
                message = "发布失败：\(error)"
            }
        }
    }
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
                Toggle("包含剧透", isOn: $spoiler)
                Button(isSaving ? "保存中…" : "保存修改") { save() }
                    .disabled(isSaving || commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .navigationTitle("编辑讨论")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            }
        }
    }

    private func save() {
        isSaving = true
        model.updateComment(id: comment.id, body: commentText.trimmingCharacters(in: .whitespacesAndNewlines), spoiler: spoiler) { value, error in
            isSaving = false
            onComplete(value, error)
            if value != nil { dismiss() }
        }
    }
}

struct NativeSettingsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var showingAccount = false

    var body: some View {
        Form {
            Section("账号") {
                if model.session.status == "authenticated" {
                    LabeledContent("当前账号", value: model.session.displayName ?? "Anime 用户")
                    NavigationLink {
                        NativeAccountManagementView(model: model)
                    } label: {
                        Label("账号与数据", systemImage: "person.crop.circle.badge.checkmark")
                    }
                    Button("切换账号") { showingAccount = true }
                } else {
                    Button("登录或注册") { showingAccount = true }
                }
            }

            Section("服务") {
                NavigationLink {
                    NativeDiagnosticsView(model: model)
                } label: {
                    Label("服务诊断", systemImage: "waveform.path.ecg")
                }
            }

            Section("关于") {
                LabeledContent("版本", value: appVersion)
                LabeledContent("界面", value: "SwiftUI 原生")
            }
        }
        .navigationTitle("设置")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
                .presentationDetents([.medium, .large])
        }
    }

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "未知"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String
        return build.map { "\(version) (\($0))" } ?? version
    }
}

struct NativeAccountManagementView: View {
    @ObservedObject var model: NativeAppModel
    @State private var displayName = ""
    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var message: String?
    @State private var exportText: String?
    @State private var showingDeleteConfirmation = false

    var body: some View {
        Form {
            Section("个人资料") {
                TextField("显示名称", text: $displayName)
                Button("保存显示名称") {
                    model.updateProfile(displayName: displayName) { error in
                        message = error.map { "保存失败：\($0)" } ?? "资料已更新"
                        if error == nil { model.loadProfile() }
                    }
                }
                .disabled(displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section("修改密码") {
                SecureField("当前密码", text: $currentPassword)
                SecureField("新密码", text: $newPassword)
                Button("修改密码") {
                    model.changePassword(currentPassword: currentPassword, newPassword: newPassword) { error in
                        message = error.map { "修改失败：\($0)" } ?? "密码已修改"
                        if error == nil { currentPassword = ""; newPassword = "" }
                    }
                }
                .disabled(currentPassword.isEmpty || newPassword.count < 8)
            }

            Section("数据与同步") {
                NavigationLink {
                    NativeSyncView(model: model)
                } label: {
                    Label("Bangumi 同步", systemImage: "arrow.triangle.2.circlepath")
                }
                Button("导出我的数据") {
                    model.exportMyData { data, error in
                        if let data { exportText = "导出成功，共 \(data.utf8.count) 字节" }
                        else { exportText = "导出失败：\(error ?? "未知错误")" }
                    }
                }
            }

            Section {
                Button("注销账号", role: .destructive) { showingDeleteConfirmation = true }
            } footer: {
                Text("注销会删除服务器账号及其社区数据，且无法恢复。")
            }

            if let message { Section { Text(message).foregroundStyle(message.contains("失败") ? .red : .secondary) } }
            if let exportText { Section("导出结果") { Text(exportText).font(.footnote) } }
        }
        .navigationTitle("账号与数据")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            displayName = model.profile?.displayName ?? model.session.displayName ?? ""
            if model.profile == nil { model.loadProfile() }
        }
        .confirmationDialog("确定注销账号吗？", isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button("永久注销", role: .destructive) {
                model.deleteAccount { error in message = error.map { "注销失败：\($0)" } ?? "账号已注销" }
            }
        }
    }
}

struct NativeSyncView: View {
    @ObservedObject var model: NativeAppModel
    @State private var message: String?

    var body: some View {
        List {
            Section("同步状态") {
                if let status = model.syncStatus {
                    LabeledContent("待同步", value: String(status.pendingCount))
                    LabeledContent("失败", value: String(status.failedCount))
                    LabeledContent("冲突", value: String(status.conflictCount))
                    LabeledContent("Bangumi", value: status.bangumiLinked ? "已连接" : "未连接")
                    if let last = status.lastSuccessfulAt {
                        LabeledContent("上次成功", value: last.prefix(16).description)
                    }
                } else {
                    ProgressView("正在加载同步状态")
                }
                Button("立即同步") {
                    model.startSync { error in
                        message = error.map { "同步失败：\($0)" } ?? "同步请求已提交"
                        model.loadSyncStatus()
                        model.loadSyncConflicts()
                    }
                }
                if let message { Text(message).font(.footnote).foregroundStyle(.secondary) }
            }

            Section("冲突处理") {
                if model.syncConflicts.isEmpty {
                    Text("没有待处理冲突")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(model.syncConflicts) { conflict in
                        VStack(alignment: .leading, spacing: 8) {
                            Text("作品 \(conflict.subjectId) · \(conflict.fieldName)").font(.headline)
                            Text("本地：\(conflict.localValue)").font(.caption).foregroundStyle(.secondary)
                            Text("远端：\(conflict.remoteValue)").font(.caption).foregroundStyle(.secondary)
                            HStack {
                                Button("保留本地") { resolve(conflict, choice: "keep_local") }
                                Button("使用远端") { resolve(conflict, choice: "use_remote") }
                                Button("稍后") { resolve(conflict, choice: "later") }
                            }
                            .font(.caption.weight(.semibold))
                        }
                        .padding(.vertical, 5)
                    }
                }
            }
        }
        .navigationTitle("Bangumi 同步")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            model.loadSyncStatus()
            model.loadSyncConflicts()
        }
        .refreshable {
            await withCheckedContinuation { continuation in
                model.loadSyncStatus { _, _ in
                    model.loadSyncConflicts { _, _ in continuation.resume() }
                }
            }
        }
    }

    private func resolve(_ conflict: NativeSyncConflictSnapshot, choice: String) {
        model.resolveSyncConflict(id: conflict.id, expectedVersion: conflict.localVersion, choice: choice) { error in
            message = error.map { "处理失败：\($0)" } ?? "冲突已处理"
            model.loadSyncStatus()
            model.loadSyncConflicts()
        }
    }
}

struct NativeDiagnosticsView: View {
    @ObservedObject var model: NativeAppModel

    var body: some View {
        List {
            Section {
                Button("重新检查") { model.loadDiagnostics() }
            }
            Section("服务") {
                ForEach(model.diagnostics) { item in
                    HStack {
                        Image(systemName: item.healthy ? "checkmark.circle.fill" : "xmark.octagon.fill")
                            .foregroundStyle(item.healthy ? .green : .red)
                        VStack(alignment: .leading) {
                            Text(item.endpoint).font(.headline)
                            Text(item.statusCode.map(String.init) ?? "无响应")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .navigationTitle("服务诊断")
        .task { if model.diagnostics.isEmpty { model.loadDiagnostics() } }
    }
}

private struct NativeReviewComposer: View {
    let subjectId: Int64
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeReviewSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var reviewBody = ""
    @State private var spoiler = false
    @State private var visibility = "public"
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("评价标题") {
                    TextField("例如：节奏很舒服的一季", text: $title)
                }
                Section("你的感受") {
                    TextEditor(text: $reviewBody)
                        .frame(minHeight: 160)
                    Toggle("包含剧透", isOn: $spoiler)
                    Picker("可见性", selection: $visibility) {
                        Text("公开").tag("public")
                        Text("仅自己").tag("private")
                    }
                }
                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                    }
                }
                Section {
                    Button(isSubmitting ? "发布中…" : "发布评价") {
                        submit()
                    }
                        .disabled(isSubmitting || reviewBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .navigationTitle("写评价")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
        }
    }

    private func submit() {
        let normalizedBody = reviewBody.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedBody.isEmpty else { return }
        isSubmitting = true
        errorMessage = nil
        let normalizedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        model.createReviewAdvanced(
            subjectId: subjectId,
            kind: "review",
            title: normalizedTitle.isEmpty ? nil : normalizedTitle,
            body: normalizedBody,
            spoiler: spoiler,
            visibility: visibility,
        ) { review, error in
            isSubmitting = false
            if let review {
                onComplete(review, nil)
                dismiss()
            } else {
                errorMessage = error ?? "评价发布失败"
                onComplete(nil, errorMessage)
            }
        }
    }
}

private struct NativeAccountSheet: View {
    @ObservedObject var model: NativeAppModel
    @Environment(\.dismiss) private var dismiss
    @State private var username = ""
    @State private var password = ""
    @State private var displayName = ""
    @State private var isRegistering = false
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Anime 账号") {
                    TextField("用户名", text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField("密码", text: $password)
                    if isRegistering { TextField("显示名称", text: $displayName) }
                }
                Section {
                    Button(isLoading ? "提交中…" : (isRegistering ? "注册并登录" : "登录")) {
                        submit()
                    }
                    .disabled(isLoading || username.isEmpty || password.isEmpty || (isRegistering && displayName.isEmpty))
                    Button(isRegistering ? "已有账号，直接登录" : "没有账号，注册") {
                        isRegistering.toggle()
                    }
                }
                Section {
                    Button("使用 Bangumi 登录") {
                        model.beginBangumiLogin { errorMessage = $0 }
                    }
                }
                if let errorMessage {
                    Section { Text(errorMessage).foregroundStyle(.red) }
                }
            }
            .navigationTitle("账号")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } }
            }
        }
    }

    private func submit() {
        isLoading = true
        errorMessage = nil
        let finish: (String?) -> Void = { error in
            isLoading = false
            if let error { errorMessage = error } else { dismiss() }
        }
        if isRegistering {
            model.registerAnime(username: username, password: password, displayName: displayName, completion: finish)
        } else {
            model.loginWithAnime(username: username, password: password, completion: finish)
        }
    }
}

private struct NativeCatalogCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativeRemoteImage(url: subject.posterURL)
                .frame(height: 218)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
            HStack(spacing: 5) {
                Image(systemName: "star.fill")
                    .foregroundStyle(.orange)
                Text(subject.rating.map { String(format: "%.1f", $0) } ?? "暂无评分")
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
    }
}

private struct NativeCollectionCard: View {
    let item: NativeCollectionItemSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativeRemoteImage(url: item.posterURL)
                .frame(height: 218)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            Text(item.title)
                .font(.headline)
                .lineLimit(2)
            HStack {
                Text(item.status.displayName)
                Spacer()
                Text("\(item.episodeProgress)/\(item.totalEpisodes)")
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
    }
}

private struct NativeActivityCard: View {
    let item: NativeActivityItemSnapshot
    let destination: AnyView?

    var body: some View {
        Group {
            if let destination {
                NavigationLink { destination } label: { content(subject: item.subjectSummary) }
            } else {
                content(subject: item.subjectSummary)
            }
        }
        .buttonStyle(.plain)
    }

    private func content(subject: NativeSubjectSummary?) -> some View {
        HStack(alignment: .top, spacing: 12) {
            NativeAvatar(url: item.actorAvatarUrl.flatMap(URL.init(string:)), name: item.actorName)
            VStack(alignment: .leading, spacing: 7) {
                Text(item.actorName).font(.headline)
                Text(item.summary).foregroundStyle(.primary)
                Text(item.occurredAt.replacingOccurrences(of: "T", with: " ").prefix(16))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if let subject {
                    Label(subject.title, systemImage: "film")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.tint)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct NativeNotificationRow: View {
    let notification: NativeNotificationSnapshot
    let destination: AnyView?
    let onRead: () -> Void

    var body: some View {
        Group {
            if let destination {
                NavigationLink {
                    destination
                } label: {
                    content
                }
                .simultaneousGesture(TapGesture().onEnded { _ in onRead() })
            } else {
                Button(action: onRead) {
                    content
                }
            }
        }
        .buttonStyle(.plain)
    }

    private var content: some View {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: notification.readAt == nil ? "bell.badge.fill" : "bell")
                    .foregroundStyle(notification.readAt == nil ? Color.accentColor : Color.secondary)
                VStack(alignment: .leading, spacing: 5) {
                    Text(notification.title).font(.headline)
                    Text(notification.createdAt.replacingOccurrences(of: "T", with: " ").prefix(16))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .padding(16)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct NativeInlineError: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("加载失败", systemImage: "exclamationmark.triangle")
                .font(.headline)
            Text(message).font(.subheadline).foregroundStyle(.secondary)
            Button("重试", action: retry).buttonStyle(.borderedProminent)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct NativeRemoteImage: View {
    let url: URL?

    var body: some View {
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image): image.resizable().scaledToFill()
            case .failure:
                ZStack {
                    Color.secondary.opacity(0.14)
                    Image(systemName: "photo").foregroundStyle(.secondary)
                }
            default: Color.secondary.opacity(0.12)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

private struct NativeAvatar: View {
    let url: URL?
    let name: String?

    var body: some View {
        NativeRemoteImage(url: url)
            .overlay {
                if url == nil {
                    Text(String(name?.first ?? "A"))
                        .font(.headline)
                        .foregroundStyle(.white)
                }
            }
            .frame(width: 48, height: 48)
            .background(Color.accentColor.gradient)
            .clipShape(Circle())
    }
}

private struct NativeMetric: View {
    let value: Int
    let title: String

    var body: some View {
        VStack(spacing: 4) {
            Text(String(value)).font(.title3.weight(.bold))
            Text(title).font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct NativeActionRow: View {
    let title: String
    let subtitle: String
    let systemImage: String
    var tint: Color = .accentColor

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(tint)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.headline)
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(.tertiary)
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private extension NativeSubjectSummary {
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }

    static func placeholder(id: Int64, title: String) -> NativeSubjectSummary {
        NativeSubjectSummary(
            id: id,
            title: title,
            originalTitle: nil,
            posterUrl: nil,
            year: nil,
            type: "Other",
            airingStatus: "Unknown",
            rating: nil,
            ratingVotes: 0,
        )
    }
}

private extension NativeCollectionItemSnapshot {
    var subjectSummary: NativeSubjectSummary {
        NativeSubjectSummary(
            id: subjectId,
            title: title,
            originalTitle: originalTitle.isEmpty ? nil : originalTitle,
            posterUrl: posterUrl,
            year: nil,
            type: "Other",
            airingStatus: status == "Completed" ? "Finished" : "Unknown",
            rating: score > 0 ? score : nil,
            ratingVotes: 0,
        )
    }
}

private extension NativeActivityItemSnapshot {
    var subjectSummary: NativeSubjectSummary? {
        guard let subjectId, let subjectTitle else { return nil }
        return NativeSubjectSummary(
            id: subjectId,
            title: subjectTitle,
            originalTitle: nil,
            posterUrl: posterUrl,
            year: nil,
            type: "Other",
            airingStatus: "Unknown",
            rating: nil,
            ratingVotes: 0,
        )
    }
}

private extension NativeNotificationSnapshot {
    var title: String {
        let actor = actorName ?? "有人"
        switch kind {
        case "review_liked": return "\(actor) 赞了你的评价"
        case "comment_liked": return "\(actor) 赞了你的评论"
        case "comment_replied": return "\(actor) 回复了你的评论"
        case "followed": return "\(actor) 关注了你"
        default: return "\(actor) 与你产生了互动"
        }
    }
}

private extension NativeSessionSnapshot {
    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

private extension String {
    var displayName: String {
        switch self {
        case "Wish": return "想看"
        case "Watching": return "在看"
        case "Completed": return "看过"
        case "OnHold": return "搁置"
        case "Dropped": return "抛弃"
        default: return self
        }
    }

    var feedDisplayName: String {
        switch self {
        case "following": return "关注"
        case "popular": return "热门"
        case "public": return "全站"
        default: return self
        }
    }
}
