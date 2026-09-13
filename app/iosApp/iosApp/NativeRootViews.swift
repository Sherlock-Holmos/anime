import SwiftUI

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
    @State private var isSearching = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    libraryIntro
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

    private var libraryIntro: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text("Anime")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tint)
            Text("搜索作品，打开完整资料与社区讨论。")
                .foregroundStyle(.secondary)
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
        }
    }

    private func submitSearch() {
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return }
        isSearching = true
        errorMessage = nil
        model.search(query: normalized) { snapshot, error in
            results = snapshot?.items ?? []
            errorMessage = error
            isSearching = false
            if error == nil { model.saveSearchHistory(query: normalized) }
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
                    VStack(alignment: .leading, spacing: 5) {
                        Text("个人空间")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.tint)
                        Text("把想看、正在追和已经看过的作品收在一处。")
                            .foregroundStyle(.secondary)
                    }
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

struct NativeActivityView: View {
    @ObservedObject var model: NativeAppModel
    @State private var selectedMode = "动态"
    @State private var selectedFeed = "public"
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("社区")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.tint)
                        Text("评分、评价、片单与讨论都在这里汇聚。")
                            .foregroundStyle(.secondary)
                    }
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
                NativeActivityCard(item: item) { subject in
                    NativeSubjectDetailView(summary: subject, model: model)
                }
            }
            if page.nextCursor != nil {
                Button("加载更多") {
                    model.loadActivity(feed: selectedFeed, cursor: page.nextCursor)
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
                NativeNotificationRow(notification: notification) {
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
    @State private var showingDiagnostics = false
    @State private var message: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    profileHeader
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
                    Button { showingAccount = true } label: { Image(systemName: "person.crop.circle") }
                        .accessibilityLabel("账号")
                }
            }
            .sheet(isPresented: $showingAccount) {
                NativeAccountSheet(model: model)
                    .presentationDetents([.medium, .large])
            }
            .sheet(isPresented: $showingDiagnostics) {
                NavigationStack { NativeDiagnosticsView(model: model) }
                    .presentationDetents([.large])
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

    private var profileHeader: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text("Anime")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tint)
            Text("管理账号、片库和同步状态。")
                .foregroundStyle(.secondary)
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

        Button { showingDiagnostics = true } label: {
            NativeActionRow(title: "服务诊断", subtitle: "检查 API、健康检查和当前连接状态", systemImage: "waveform.path.ecg")
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

struct NativeSubjectCommunityView: View {
    let subjectId: Int64
    let community: NativeSubjectCommunitySnapshot?
    let errorMessage: String?
    @ObservedObject var model: NativeAppModel
    let onReload: () -> Void

    @State private var selectedScore = 0
    @State private var selectedCollectionStatus: String?
    @State private var commentText = ""
    @State private var isSubmitting = false
    @State private var isSavingRating = false
    @State private var isSavingCollection = false
    @State private var localComments: [NativeCommentSnapshot] = []
    @State private var localReviews: [NativeReviewSnapshot] = []
    @State private var commentReactions: [String: NativeReactionSnapshot] = [:]
    @State private var reviewReactions: [String: NativeReactionSnapshot] = [:]
    @State private var message: String?
    @State private var showingReviewComposer = false

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
                    Text("公开讨论")
                        .font(.caption)
                        .foregroundStyle(.secondary)
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
            }
            Text(comment.body)
                .fixedSize(horizontal: false, vertical: true)
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
        model.createComment(subjectId: subjectId, body: body) { comment, error in
            isSubmitting = false
            if let comment {
                localComments.insert(comment, at: 0)
                commentText = ""
                message = "讨论已发布"
            } else if let error {
                message = "发布失败：\(error)"
            }
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
        model.createReview(
            subjectId: subjectId,
            title: normalizedTitle.isEmpty ? nil : normalizedTitle,
            body: normalizedBody,
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

private struct NativeActivityCard<Destination: View>: View {
    let item: NativeActivityItemSnapshot
    let destination: (NativeSubjectSummary) -> Destination

    var body: some View {
        Group {
            if let subject = item.subjectSummary {
                NavigationLink { destination(subject) } label: { content(subject: subject) }
            } else {
                content(subject: nil)
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
    let onRead: () -> Void

    var body: some View {
        Button(action: onRead) {
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
        .buttonStyle(.plain)
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
