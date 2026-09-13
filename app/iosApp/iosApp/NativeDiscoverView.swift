import Foundation
import SwiftUI
import AnimeShared

@MainActor
final class NativeAppModel: ObservableObject {
    @Published private(set) var discovery: NativeDiscoverySnapshot?
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var session = NativeSessionSnapshot(status: "restoring")
    @Published private(set) var searchDiscovery: NativeSearchDiscoverySnapshot?
    @Published private(set) var collectionPage: NativeCollectionPageSnapshot?
    @Published private(set) var activityPage: NativeActivityPageSnapshot?
    @Published private(set) var notifications: [NativeNotificationSnapshot] = []
    @Published private(set) var profile: NativeProfileSnapshot?
    @Published private(set) var subjectCommunity: [Int64: NativeSubjectCommunitySnapshot] = [:]
    @Published private(set) var diagnostics: [NativeDiagnosticSnapshot] = []

    private var facade: IosNativeAppFacade?
    private var hasStarted = false

    init() {}

    func start() {
        guard !hasStarted else { return }
        hasStarted = true
        NSLog("[Anime iOS] native model startup begin")

        let facade = IosBridge.shared.nativeAppFacade(
            openExternalUrl: { rawUrl in
                IosAuthSessionCoordinator.shared.start(rawUrl: rawUrl)
            },
            readSecret: { account in
                IosKeychain.shared.read(account: account)
            },
            writeSecret: { account, value in
                IosKeychain.shared.write(account: account, value: value)
            },
            removeSecret: { account in
                IosKeychain.shared.remove(account: account)
            },
        )
        self.facade = facade
        NSLog("[Anime iOS] native facade created")
        facade.startSessionObservation { [weak self] rawSnapshot in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.session = Self.decode(rawSnapshot, as: NativeSessionSnapshot.self) ?? NativeSessionSnapshot(status: "failed")
            }
        }
        NSLog("[Anime iOS] session observation installed")
        facade.start()
        NSLog("[Anime iOS] native facade start requested")
    }

    func refresh(force: Bool = false) {
        start()
        guard let facade else {
            errorMessage = "iOS 原生页面尚未准备完成"
            return
        }
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        facade.loadDiscovery(force: force) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isLoading = false
                if let rawSnapshot, let snapshot = Self.decode(rawSnapshot, as: NativeDiscoverySnapshot.self) {
                    self.discovery = snapshot
                }
                self.errorMessage = error
            }
        }
    }

    func loadSubject(
        id: Int64,
        force: Bool = false,
        completion: @escaping (NativeSubjectDetailSnapshot?, String?) -> Void,
    ) {
        start()
        guard let facade else {
            completion(nil, "iOS 原生页面尚未准备完成")
            return
        }
        facade.loadSubject(subjectId: id, force: force) { rawSnapshot, error in
            Task { @MainActor in
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSubjectDetailSnapshot.self) }
                completion(snapshot, error)
            }
        }
    }

    func loadSearchDiscovery(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.loadSearchDiscovery { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let rawSnapshot {
                    self.searchDiscovery = Self.decode(rawSnapshot, as: NativeSearchDiscoverySnapshot.self)
                }
                completion?(error)
            }
        }
    }

    func search(
        query: String,
        cursor: String? = nil,
        completion: ((NativeSearchResultsSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        facade?.searchSubjects(query: query, cursor: cursor) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSearchResultsSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func saveSearchHistory(query: String) {
        start()
        facade?.saveSearchHistory(query: query)
    }

    func loadCollection(status: String?, completion: ((NativeCollectionPageSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadCollection(status: status) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeCollectionPageSnapshot.self) }
                self.collectionPage = snapshot
                completion?(snapshot, error)
            }
        }
    }

    func loadActivity(
        feed: String,
        cursor: String? = nil,
        completion: ((NativeActivityPageSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        facade?.loadActivity(feed: feed, cursor: cursor) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeActivityPageSnapshot.self) }
                self.activityPage = snapshot
                completion?(snapshot, error)
            }
        }
    }

    func loadNotifications(completion: (([NativeNotificationSnapshot]?, String?) -> Void)? = nil) {
        start()
        facade?.loadNotifications { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: [NativeNotificationSnapshot].self) }
                self.notifications = snapshot ?? []
                completion?(snapshot, error)
            }
        }
    }

    func markNotificationRead(id: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.markNotificationRead(id: id) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil { self.loadNotifications() }
                completion?(error)
            }
        }
    }

    func loadProfile(completion: ((NativeProfileSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadProfile { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeProfileSnapshot.self) }
                self.profile = snapshot
                completion?(snapshot, error)
            }
        }
    }

    func loadSubjectCommunity(subjectId: Int64, completion: ((NativeSubjectCommunitySnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.loadSubjectCommunity(subjectId: subjectId) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSubjectCommunitySnapshot.self) }
                if let snapshot { self.subjectCommunity[subjectId] = snapshot }
                completion?(snapshot, error)
            }
        }
    }

    func saveRating(subjectId: Int64, score: Int, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.saveRating(subjectId: subjectId, score: Int32(score)) { error in
            Task { @MainActor in completion?(error) }
        }
    }

    func setCollection(subjectId: Int64, status: String?, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.setCollection(subjectId: subjectId, status: status) { error in
            Task { @MainActor in completion?(error) }
        }
    }

    func createComment(subjectId: Int64, body: String, completion: ((NativeCommentSnapshot?, String?) -> Void)? = nil) {
        start()
        facade?.createComment(subjectId: subjectId, body: body) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeCommentSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func createReview(
        subjectId: Int64,
        title: String?,
        body: String,
        completion: ((NativeReviewSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        facade?.createReview(subjectId: subjectId, title: title, body: body) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeReviewSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func reactComment(
        id: String,
        reaction: String,
        active: Bool,
        completion: ((NativeReactionSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        facade?.reactComment(id: id, reaction: reaction, active: active) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeReactionSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func reactReview(
        id: String,
        reaction: String,
        active: Bool,
        completion: ((NativeReactionSnapshot?, String?) -> Void)? = nil,
    ) {
        start()
        facade?.reactReview(id: id, reaction: reaction, active: active) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeReactionSnapshot.self) }
                completion?(snapshot, error)
            }
        }
    }

    func beginBangumiLogin(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.beginBangumiLogin { _, error in
            Task { @MainActor in completion?(error) }
        }
    }

    func loginWithAnime(username: String, password: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.loginWithAnime(username: username, password: password) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let rawSnapshot {
                    self.session = Self.decode(rawSnapshot, as: NativeSessionSnapshot.self) ?? self.session
                }
                completion?(error)
            }
        }
    }

    func registerAnime(username: String, password: String, displayName: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.registerAnime(username: username, password: password, displayName: displayName) { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let rawSnapshot {
                    self.session = Self.decode(rawSnapshot, as: NativeSessionSnapshot.self) ?? self.session
                }
                completion?(error)
            }
        }
    }

    func updateProfile(displayName: String, completion: ((String?) -> Void)? = nil) {
        start()
        facade?.updateProfile(displayName: displayName) { error in
            Task { @MainActor in completion?(error) }
        }
    }

    func logout(completion: ((String?) -> Void)? = nil) {
        start()
        facade?.logout { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if error == nil { self.profile = nil }
                completion?(error)
            }
        }
    }

    func loadDiagnostics(completion: (([NativeDiagnosticSnapshot]?, String?) -> Void)? = nil) {
        start()
        facade?.diagnostics { [weak self] rawSnapshot, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: [NativeDiagnosticSnapshot].self) }
                self.diagnostics = snapshot ?? []
                completion?(snapshot, error)
            }
        }
    }

    fileprivate static func decode<T: Decodable>(_ raw: String, as type: T.Type) -> T? {
        guard let data = raw.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }
}

struct NativeDiscoverView: View {
    @ObservedObject var model: NativeAppModel

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 28) {
                    discoverHeader
                    if model.isLoading && model.discovery == nil {
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 240)
                    } else if let discovery = model.discovery {
                        if let hero = discovery.sections.flatMap(\.subjects).first {
                            NavigationLink(value: hero) {
                                NativeHeroCard(subject: hero)
                            }
                            .buttonStyle(.plain)
                        }

                        ForEach(discovery.sections) { section in
                            NativeDiscoverySectionView(section: section)
                        }
                    } else {
                        NativeEmptyState(
                            title: "暂时没有内容",
                            message: model.errorMessage ?? "下拉重新加载发现内容。",
                            actionTitle: "重试",
                            action: { model.refresh(force: true) },
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .refreshable {
                model.refresh(force: true)
            }
            .navigationTitle("发现")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        model.refresh(force: true)
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .accessibilityLabel("刷新")
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
        }
        .task {
            model.start()
            if model.discovery == nil {
                model.refresh()
            }
        }
    }

    private var discoverHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Anime")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tint)
            Text("为你整理的新作、口碑与正在追")
                .font(.title3.weight(.semibold))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NativeDiscoverySectionView: View {
    let section: NativeDiscoverySection

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(section.title)
                    .font(.title2.weight(.bold))
                Text(description)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 12) {
                    ForEach(section.subjects) { subject in
                        NavigationLink(value: subject) {
                            NativeSubjectCard(subject: subject)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
        }
    }

    private var description: String {
        switch section.id {
        case "continue": return "从上次停下的地方继续"
        case "airing": return "这一刻，大家正在追"
        case "top-rated": return "来自 Bangumi 的高口碑作品"
        case "upcoming": return "值得提前留意的新作"
        default: return "为你整理的作品"
        }
    }
}

private struct NativeHeroCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: subject.posterURL) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    Color.secondary.opacity(0.2)
                }
            }
            .frame(maxWidth: .infinity)
            .overlay {
                LinearGradient(
                    colors: [.clear, .black.opacity(0.82)],
                    startPoint: .center,
                    endPoint: .bottom,
                )
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("本季口碑新作")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.82))
                Text(subject.title)
                    .font(.title.weight(.bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                if let rating = subject.rating {
                    Label(String(format: "%.1f  Bangumi", rating), systemImage: "star.fill")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white)
                }
            }
            .padding(20)
        }
        .frame(height: 250)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

private struct NativeSubjectCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            AsyncImage(url: subject.posterURL) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    ZStack {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color.secondary.opacity(0.14))
                        Image(systemName: "photo")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .frame(width: 156, height: 218)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: 5) {
                if let rating = subject.rating {
                    Image(systemName: "star.fill")
                        .foregroundStyle(.orange)
                    Text(String(format: "%.1f", rating))
                } else {
                    Text("暂无评分")
                }
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
        .frame(width: 156, alignment: .leading)
    }
}

struct NativeSubjectDetailView: View {
    let summary: NativeSubjectSummary
    @ObservedObject var model: NativeAppModel
    @State private var detail: NativeSubjectDetailSnapshot?
    @State private var community: NativeSubjectCommunitySnapshot?
    @State private var errorMessage: String?
    @State private var communityError: String?
    @State private var isLoading = true

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 22) {
                NativeDetailHero(summary: detail?.summary ?? summary)

                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                }

                if let detail {
                    detailBody(detail)
                } else if let errorMessage {
                    NativeEmptyState(
                        title: "详情加载失败",
                        message: errorMessage,
                        actionTitle: "重试",
                        action: { load(force: true) },
                    )
                }
                NativeSubjectCommunityView(
                    subjectId: summary.id,
                    community: community,
                    errorMessage: communityError,
                    model: model,
                    onReload: loadCommunity,
                )
            }
            .padding(16)
            .padding(.bottom, 32)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle(summary.title)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            load()
            loadCommunity()
        }
    }

    @ViewBuilder
    private func detailBody(_ detail: NativeSubjectDetailSnapshot) -> some View {
        if let summaryText = detail.summaryText, !summaryText.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                Text("简介")
                    .font(.title3.weight(.bold))
                Text(summaryText)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }

        if !detail.tags.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 8) {
                    ForEach(detail.tags, id: \.self) { tag in
                        Text(tag)
                            .font(.caption.weight(.medium))
                            .padding(.horizontal, 11)
                            .padding(.vertical, 7)
                            .background(.thinMaterial, in: Capsule())
                    }
                }
            }
        }

        if let url = URL(string: detail.sourceUrl) {
            Link(destination: url) {
                Label("查看来源", systemImage: "arrow.up.right.square")
                    .font(.subheadline.weight(.semibold))
            }
        }
    }

    private func load(force: Bool = false) {
        isLoading = true
        errorMessage = nil
        model.loadSubject(id: summary.id, force: force) { value, error in
            detail = value
            errorMessage = error
            isLoading = false
        }
    }

    private func loadCommunity() {
        communityError = nil
        if let cached = model.subjectCommunity[summary.id] {
            community = cached
        }
        model.loadSubjectCommunity(subjectId: summary.id) { value, error in
            community = value
            communityError = error
        }
    }
}

private struct NativeDetailHero: View {
    let summary: NativeSubjectSummary

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            AsyncImage(url: summary.posterURL) { phase in
                switch phase {
                case .success(let image): image.resizable().scaledToFill()
                default: Color.secondary.opacity(0.15)
                }
            }
            .frame(width: 132, height: 188)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

            VStack(alignment: .leading, spacing: 10) {
                Text(summary.title)
                    .font(.title2.weight(.bold))
                if let originalTitle = summary.originalTitle, !originalTitle.isEmpty {
                    Text(originalTitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                if let rating = summary.rating {
                    Label(String(format: "%.1f", rating), systemImage: "star.fill")
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(.orange)
                }
                Text(metadata)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var metadata: String {
        [summary.year.map(String.init), summary.type.displayName, summary.airingStatus.displayName]
            .compactMap { $0 }
            .joined(separator: " · ")
    }
}

private struct NativeEmptyState: View {
    let title: String
    let message: String
    let actionTitle: String
    let action: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "sparkles")
        } description: {
            Text(message)
        } actions: {
            Button(actionTitle, action: action)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, minHeight: 220)
    }
}

private extension NativeSubjectSummary {
    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

private extension String {
    var displayName: String {
        switch self {
        case "Tv": return "TV"
        case "Web": return "Web"
        case "Ova": return "OVA"
        case "Movie": return "剧场版"
        case "Airing": return "连载中"
        case "Finished": return "已完结"
        case "Announced": return "未开播"
        default: return self
        }
    }
}

struct NativeSessionSnapshot: Codable {
    let status: String
    let userId: String?
    let displayName: String?
    let avatarUrl: String?
    let expiresAtEpochSeconds: Int64?
    let message: String?

    init(
        status: String,
        userId: String? = nil,
        displayName: String? = nil,
        avatarUrl: String? = nil,
        expiresAtEpochSeconds: Int64? = nil,
        message: String? = nil,
    ) {
        self.status = status
        self.userId = userId
        self.displayName = displayName
        self.avatarUrl = avatarUrl
        self.expiresAtEpochSeconds = expiresAtEpochSeconds
        self.message = message
    }
}

struct NativeDiscoverySnapshot: Codable {
    let sections: [NativeDiscoverySection]
    let generatedAtEpochSeconds: Int64
}

struct NativeDiscoverySection: Codable, Identifiable {
    let id: String
    let title: String
    let subjects: [NativeSubjectSummary]
}

struct NativeSubjectSummary: Codable, Identifiable, Hashable {
    let id: Int64
    let title: String
    let originalTitle: String?
    let posterUrl: String?
    let year: Int?
    let type: String
    let airingStatus: String
    let rating: Double?
    let ratingVotes: Int
}

struct NativeSubjectDetailSnapshot: Codable {
    let summary: NativeSubjectSummary
    let summaryText: String?
    let airDate: String?
    let endDate: String?
    let totalEpisodes: Int?
    let tags: [String]
    let backdropUrl: String?
    let sourceUrl: String
    let dataUpdatedAtEpochSeconds: Int64
}
