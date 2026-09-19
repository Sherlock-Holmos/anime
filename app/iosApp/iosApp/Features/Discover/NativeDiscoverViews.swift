import Foundation
import SwiftUI
import AnimeShared

struct NativeDiscoverView: View {
    @ObservedObject var model: NativeAppModel
    @State private var discoverTitleOpacity = 1.0

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 28) {
                    if model.isLoading && model.discovery == nil {
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 240)
                    } else if let discovery = model.discovery {
                        let heroSubjects = carouselSubjects(from: discovery)
                        if !heroSubjects.isEmpty {
                            NativeDiscoveryCarousel(subjects: heroSubjects)
                        }

                        ForEach(discovery.sections) { section in
                            NativeDiscoverySectionView(section: section, model: model)
                        }
                    } else {
                        NativeEmptyState(
                            title: AnimeL10n.string(.discoverEmptyTitle),
                            message: model.errorMessage ?? AnimeL10n.string(.discoverEmptyMessage),
                            actionTitle: AnimeL10n.string(.retry),
                            action: { model.refresh(force: true) },
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .refreshable {
                model.refresh(force: true)
            }
            .onScrollGeometryChange(for: CGFloat.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top
            } action: { _, offset in
                let fadeDistance: CGFloat = 56
                let nextOpacity = 1 - min(max(offset / fadeDistance, 0), 1)
                if abs(nextOpacity - discoverTitleOpacity) > 0.01 {
                    discoverTitleOpacity = nextOpacity
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text(AnimeL10n.key(.discover))
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                        .layoutPriority(1)
                        .opacity(discoverTitleOpacity)
                        .accessibilityAddTraits(.isHeader)
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeCalendarView(model: model)
                    } label: {
                        Image(systemName: "calendar")
                    }
                    .accessibilityLabel(AnimeL10n.string(.calendar))
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

    private func carouselSubjects(from discovery: NativeDiscoverySnapshot) -> [NativeSubjectSummary] {
        var seen = Set<Int64>()
        return Array(
            discovery.sections
                .flatMap(\.subjects)
                .filter { seen.insert($0.id).inserted }
                .prefix(5)
        )
    }
}

private struct NativeDiscoveryCarousel: View {
    let subjects: [NativeSubjectSummary]

    var body: some View {
        TabView {
            ForEach(subjects) { subject in
                NavigationLink(value: subject) {
                    NativeHeroCard(subject: subject)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(height: 250)
        .tabViewStyle(.page(indexDisplayMode: .automatic))
        .indexViewStyle(.page(backgroundDisplayMode: .interactive))
    }
}

private struct NativeDiscoverySectionView: View {
    let section: NativeDiscoverySection
    @ObservedObject var model: NativeAppModel

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(section.title)
                        .font(.title2.weight(.bold))
                    Text(description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                NavigationLink(AnimeL10n.key(.seeAll)) {
                    NativeDiscoverySectionListView(section: section, model: model)
                }
                .font(.subheadline.weight(.semibold))
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
        case "continue": return AnimeL10n.string(.discoverContinue)
        case "airing": return AnimeL10n.string(.discoverAiring)
        case "top-rated": return AnimeL10n.string(.discoverTopRated)
        case "upcoming": return AnimeL10n.string(.discoverUpcoming)
        default: return AnimeL10n.string(.discoverCurated)
        }
    }
}

private struct NativeDiscoverySectionListView: View {
    let section: NativeDiscoverySection
    @ObservedObject var model: NativeAppModel

    var body: some View {
        ScrollView {
            LazyVGrid(columns: posterGridColumns, spacing: 20) {
                ForEach(section.subjects) { subject in
                    NavigationLink(value: subject) {
                        NativeSubjectCard(subject: subject, fixedWidth: nil)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(section.title)
        .navigationBarTitleDisplayMode(.large)
        .navigationDestination(for: NativeSubjectSummary.self) { subject in
            NativeSubjectDetailView(summary: subject, model: model)
        }
    }

    private let posterGridColumns = [GridItem(.flexible()), GridItem(.flexible())]
}

private struct NativeHeroCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            NativeRemoteImage(url: subject.posterURL, contentMode: .fill)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .clipped()
            LinearGradient(
                colors: [.clear, .black.opacity(0.82)],
                startPoint: .center,
                endPoint: .bottom,
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            VStack(alignment: .leading, spacing: 8) {
                Text(AnimeL10n.key(.heroEyebrow))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.82))
                Text(subject.title)
                    .font(.title.weight(.bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                if let rating = subject.rating {
                    Label(AnimeL10n.string(.subjectRating, String(format: "%.1f", rating)), systemImage: "star.fill")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white)
                }
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 250)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

private struct NativeSubjectCard: View {
    let subject: NativeSubjectSummary
    let fixedWidth: CGFloat?

    init(subject: NativeSubjectSummary, fixedWidth: CGFloat? = 156) {
        self.subject = subject
        self.fixedWidth = fixedWidth
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let fixedWidth {
                NativePosterImage(
                    url: subject.posterURL,
                    width: fixedWidth,
                    height: fixedWidth * 4 / 3,
                    cornerRadius: 16,
                )
            } else {
                NativePosterImage(
                    url: subject.posterURL,
                    aspectRatio: 3 / 4,
                    cornerRadius: 16,
                )
            }

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
                    Text(AnimeL10n.key(.noRating))
                }
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
        .frame(width: fixedWidth, alignment: .leading)
        .frame(maxWidth: fixedWidth == nil ? .infinity : nil, alignment: .leading)
    }
}

struct NativeSubjectDetailView: View {
    let summary: NativeSubjectSummary
    @ObservedObject var model: NativeAppModel
    let focusCommentId: String?
    @State private var detail: NativeSubjectDetailSnapshot?
    @State private var community: NativeSubjectCommunitySnapshot?
    @State private var errorMessage: String?
    @State private var communityError: String?
    @State private var isLoading = true

    init(
        summary: NativeSubjectSummary,
        model: NativeAppModel,
        focusCommentId: String? = nil,
    ) {
        self.summary = summary
        self.model = model
        self.focusCommentId = focusCommentId
    }

    var body: some View {
        ScrollViewReader { proxy in
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
                            title: AnimeL10n.string(.statusLoadingFailed),
                            message: errorMessage,
                            actionTitle: AnimeL10n.string(.retry),
                            action: { load(force: true) },
                        )
                    }
                    NativeSubjectCommunityView(
                        subjectId: summary.id,
                        community: community,
                        errorMessage: communityError,
                        totalEpisodes: detail?.totalEpisodes,
                        model: model,
                        onReload: loadCommunity,
                    )
                }
                .padding(16)
                .padding(.bottom, 32)
            }
            .onChange(of: community?.comments.map(\.id) ?? []) { _, _ in
                scrollToFocusedComment(using: proxy)
            }
            .task {
                load()
                loadCommunity()
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .navigationTitle(summary.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        NativeSubjectSectionsView(subjectId: summary.id, model: model)
                    } label: {
                        Image(systemName: "list.bullet.rectangle")
                    }
                    .accessibilityLabel(AnimeL10n.string(.subjectDetails))
                }
            }
        }
    }

    private func scrollToFocusedComment(using proxy: ScrollViewProxy) {
        guard let focusCommentId else { return }
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(120))
            withAnimation(.easeInOut(duration: 0.35)) {
                proxy.scrollTo("comment-\(focusCommentId)", anchor: .center)
            }
        }
    }

    @ViewBuilder
    private func detailBody(_ detail: NativeSubjectDetailSnapshot) -> some View {
        if let summaryText = detail.summaryText, !summaryText.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                Text(AnimeL10n.key(.subjectSummary))
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
                Label(AnimeL10n.key(.subjectSource), systemImage: "arrow.up.right.square")
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
            NativePosterImage(url: summary.posterURL, width: 132, height: 176, cornerRadius: 18)

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

