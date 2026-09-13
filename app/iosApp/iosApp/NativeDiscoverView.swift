import Foundation
import SwiftUI
import AnimeShared

@MainActor
final class NativeAppModel: ObservableObject {
    @Published private(set) var discovery: NativeDiscoverySnapshot?
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var session = NativeSessionSnapshot(status: "restoring")

    private let facade: IosNativeAppFacade

    init() {
        facade = IosBridge.shared.nativeAppFacade(
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
        facade.startSessionObservation { [weak self] rawSnapshot in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.session = Self.decode(rawSnapshot, as: NativeSessionSnapshot.self) ?? NativeSessionSnapshot(status: "failed")
            }
        }
        facade.start()
    }

    deinit {
        facade.stopSessionObservation()
    }

    func refresh(force: Bool = false) {
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
        facade.loadSubject(subjectId: id, force: force) { rawSnapshot, error in
            Task { @MainActor in
                let snapshot = rawSnapshot.flatMap { Self.decode($0, as: NativeSubjectDetailSnapshot.self) }
                completion(snapshot, error)
            }
        }
    }

    private static func decode<T: Decodable>(_ raw: String, as type: T.Type) -> T? {
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
    @State private var errorMessage: String?
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
            }
            .padding(16)
            .padding(.bottom, 32)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle(summary.title)
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
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
