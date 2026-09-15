import SwiftUI
import Foundation
import PhotosUI
import UIKit

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

struct NativeProfileRatingPageSnapshot: Codable {
    let items: [NativeProfileRatingSnapshot]
    let nextCursor: String?
}

struct NativeProfileRatingSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64
    let title: String
    let posterUrl: String?
    let score: Int
    let tags: [String]
    let visibility: String
    let updatedAt: String

    var posterURL: URL? { posterUrl.flatMap(URL.init(string:)) }
}

struct NativeAdminOverviewSnapshot: Codable {
    let role: String
    let usersTotal: Int64
    let usersActive: Int64
    let reviewsPublished: Int64
    let commentsPublished: Int64
    let openCommentReports: Int64
}

struct NativeAdminCommentSnapshot: Codable, Identifiable {
    let id: String
    let subjectId: Int64?
    let subjectTitle: String?
    let authorId: String
    let authorName: String
    let body: String
    let spoiler: Bool
    let moderationStatus: String
    let createdAt: String
    let editedAt: String?
}

struct NativeAdminReportSnapshot: Codable, Identifiable {
    let id: String
    let commentId: String
    let reporterId: String
    let reporterName: String
    let authorId: String
    let authorName: String
    let subjectId: Int64?
    let subjectTitle: String?
    let body: String
    let spoiler: Bool
    let moderationStatus: String
    let reasonCode: String
    let details: String?
    let status: String
    let assignedTo: String?
    let createdAt: String
    let resolvedAt: String?
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
    let commentId: String?
    let summary: String
    let occurredAt: String
    let owned: Bool

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
    let role: String?

    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

struct NativeNetworkContextSnapshot: Codable {
    let countryCode: String?
    let region: String?
    let city: String?
    let displayLocation: String?
    let isDomestic: Bool
    let recommendedRoute: String

    enum CodingKeys: String, CodingKey {
        case countryCode = "country_code"
        case region
        case city
        case displayLocation = "display_location"
        case isDomestic = "is_domestic"
        case recommendedRoute = "recommended_route"
    }
}

struct NativeDiagnosticSnapshot: Codable, Identifiable {
    let endpoint: String
    let statusCode: Int?
    let healthy: Bool
    let body: String
    let latencyMs: Int64?
    let errorMessage: String?

    var id: String { endpoint }
}

struct NativeSubjectCommunitySnapshot: Codable {
    let rating: NativeRatingSnapshot
    let reviews: [NativeReviewSnapshot]
    let comments: [NativeCommentSnapshot]
    let personal: NativeSubjectPersonalStateSnapshot?

    init(
        rating: NativeRatingSnapshot,
        reviews: [NativeReviewSnapshot],
        comments: [NativeCommentSnapshot],
        personal: NativeSubjectPersonalStateSnapshot? = nil,
    ) {
        self.rating = rating
        self.reviews = reviews
        self.comments = comments
        self.personal = personal
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        rating = try container.decode(NativeRatingSnapshot.self, forKey: .rating)
        reviews = try container.decode([NativeReviewSnapshot].self, forKey: .reviews)
        comments = try container.decode([NativeCommentSnapshot].self, forKey: .comments)
        personal = try container.decodeIfPresent(NativeSubjectPersonalStateSnapshot.self, forKey: .personal)
    }
}

struct NativeSubjectPersonalStateSnapshot: Codable {
    let userRating: Int?
    let collectionStatus: String?
    let collectionEpisodeProgress: Int?
    let isCollected: Bool
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
    @State private var isSearchPresented = false
    @State private var hasSubmittedSearch = false
    @State private var isLoadingResults = false
    @State private var errorMessage: String?
    @State private var selectedType = "all"
    @State private var selectedAiring = "all"
    @State private var selectedSort = "relevance"
    @State private var yearStartText = ""
    @State private var yearEndText = ""
    @State private var showingFilters = false
    @State private var recentSearches: [String] = []
    @State private var requestGeneration = 0
    @State private var requestedCursors = Set<String>()
    @State private var exploreGeneration = 0
    @State private var rankingItems: [NativeSubjectSummary] = []
    @State private var seasonalItems: [NativeSubjectSummary] = []
    @State private var isLoadingExploreRails = false
    @State private var exploreErrorMessage: String?
    @State private var currentBrowseMode: NativeLibraryBrowseMode?
    @State private var currentSectionID: String?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private static let recentSearchesKey = "anime.ios.library.recentSearches"

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    if hasSubmittedSearch {
                        resultContent
                    } else if isSearchPresented {
                        searchLandingContent
                    } else {
                        exploreHomeContent
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 112)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .searchable(
                text: $query,
                isPresented: $isSearchPresented,
                placement: .navigationBarDrawer(displayMode: .automatic),
                prompt: AnimeL10n.key(.searchPrompt),
            )
            .onSubmit(of: .search, submitSearch)
            .refreshable { await refreshExploreContent() }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        if hasSubmittedSearch && !isSearchPresented {
                            showingFilters = true
                        } else if isSearchPresented {
                            showingFilters = true
                        } else {
                            isSearchPresented = true
                        }
                    } label: {
                        Image(systemName: isSearchPresented || hasSubmittedSearch ? "line.3.horizontal.decrease.circle" : "magnifyingglass")
                    }
                    .accessibilityLabel(AnimeL10n.key(isSearchPresented || hasSubmittedSearch ? .searchFilters : .search))
                }
            }
            .navigationDestination(for: NativeSubjectSummary.self) { subject in
                NativeSubjectDetailView(summary: subject, model: model)
            }
        }
        .task {
            model.start()
            if model.searchDiscovery == nil { model.loadSearchDiscovery() }
            if model.discovery == nil { model.refresh() }
            loadRecentSearches()
            loadExploreRails()
        }
        .onChange(of: isSearchPresented) { _, presented in
            if presented {
                hasSubmittedSearch = false
            } else if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                hasSubmittedSearch = false
            }
        }
        .sheet(isPresented: $showingFilters) {
            NativeSearchFiltersSheet(
                selectedType: $selectedType,
                selectedAiring: $selectedAiring,
                selectedSort: $selectedSort,
                yearStartText: $yearStartText,
                yearEndText: $yearEndText,
            ) {
                if canRunQuery {
                    submitSearch()
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    @ViewBuilder
    private var exploreHomeContent: some View {
        VStack(alignment: .leading, spacing: 28) {
            Text(AnimeL10n.key(.library))
                .font(.largeTitle.weight(.bold))
                .accessibilityAddTraits(.isHeader)

            browseEntrySection

            if !themeSections.isEmpty {
                librarySectionHeader(.libraryThemes)
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 14) {
                        ForEach(themeSections) { section in
                            Button {
                                showSection(section)
                            } label: {
                                NativeLibraryThemeCard(section: section, reduceTransparency: reduceTransparency)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(section.title)
                        }
                    }
                }
            }

            if !rankingItems.isEmpty || isLoadingExploreRails {
                libraryRail(
                    title: .libraryRanking,
                    subjects: rankingItems,
                    isLoading: isLoadingExploreRails,
                    mode: .rating,
                )
            }

            if !seasonalItems.isEmpty || isLoadingExploreRails {
                libraryRail(
                    title: .librarySeason,
                    subjects: seasonalItems,
                    isLoading: isLoadingExploreRails,
                    mode: .seasonal,
                )
            }

            if let topRated = section(withID: "top-rated"), !topRated.subjects.isEmpty {
                libraryRail(title: .libraryHighRated, subjects: topRated.subjects, isLoading: false, mode: .section(topRated.id))
            }

            if let discovery = model.searchDiscovery, !discovery.recommendations.isEmpty {
                libraryRail(title: .libraryCurated, subjects: discovery.recommendations, isLoading: false, mode: .curated)
            }

            if let exploreErrorMessage {
                NativeLibraryInlineError(message: exploreErrorMessage) { loadExploreRails() }
            }
        }
    }

    private var browseEntrySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(AnimeL10n.key(.libraryBrowse))
                .font(.title2.weight(.bold))
                .accessibilityAddTraits(.isHeader)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                browseEntry(.type, systemImage: "square.grid.2x2") {
                    showingFilters = true
                }
                if themeSections.isEmpty {
                    browseEntry(.topics, systemImage: "rectangle.stack") { }
                        .disabled(true)
                } else {
                    NavigationLink {
                        NativeLibraryTopicListView(sections: themeSections, model: model)
                    } label: {
                        NativeLibraryBrowseEntryLabel(kind: .topics, systemImage: "rectangle.stack")
                    }
                    .buttonStyle(.plain)
                }
                browseEntry(.rankings, systemImage: "chart.bar.fill") { browseRating() }
                browseEntry(.seasonal, systemImage: "calendar") { browseSeasonal() }
            }
        }
    }

    private func browseEntry(
        _ kind: NativeLibraryBrowseEntryKind,
        systemImage: String,
        action: @escaping () -> Void,
    ) -> some View {
        Button(action: action) {
            NativeLibraryBrowseEntryLabel(kind: kind, systemImage: systemImage)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func librarySectionHeader(_ key: AnimeL10n.Key) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(AnimeL10n.key(key))
                .font(.title2.weight(.bold))
                .accessibilityAddTraits(.isHeader)
            Spacer()
            if key == .libraryThemes {
                NavigationLink {
                    NativeLibraryTopicListView(sections: themeSections, model: model)
                } label: {
                    Text(AnimeL10n.key(.seeAll))
                        .font(.subheadline.weight(.semibold))
                }
                .accessibilityLabel(AnimeL10n.key(.seeAll))
            }
        }
    }

    private func libraryRail(
        title: AnimeL10n.Key,
        subjects: [NativeSubjectSummary],
        isLoading: Bool,
        mode: NativeLibraryBrowseMode,
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(AnimeL10n.key(title))
                    .font(.title2.weight(.bold))
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                Button(AnimeL10n.key(.seeAll)) { browse(mode) }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.tint)
            }
            if isLoading && subjects.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: 112)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: 14) {
                        ForEach(subjects) { subject in
                            NavigationLink(value: subject) {
                                NativeLibrarySubjectCard(subject: subject)
                                    .frame(width: 132)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private var searchLandingContent: some View {
        VStack(alignment: .leading, spacing: 24) {
            if !recentSearches.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text(AnimeL10n.key(.libraryRecentSearches))
                            .font(.title3.weight(.bold))
                            .accessibilityAddTraits(.isHeader)
                        Spacer()
                        Button(AnimeL10n.key(.libraryClearRecent)) { clearRecentSearches() }
                            .font(.footnote.weight(.semibold))
                    }
                    FlowLayout(items: recentSearches) { term in
                        Button(term) {
                            query = term
                            submitSearch()
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }

            if let discovery = model.searchDiscovery, !discovery.trending.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(AnimeL10n.key(.searchTrending))
                        .font(.title3.weight(.bold))
                        .accessibilityAddTraits(.isHeader)
                    FlowLayout(items: discovery.trending) { term in
                        Button(term) {
                            query = term
                            submitSearch()
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }

            VStack(alignment: .leading, spacing: 12) {
                Text(AnimeL10n.key(.libraryQuickExplore))
                    .font(.title3.weight(.bold))
                    .accessibilityAddTraits(.isHeader)
                HStack(spacing: 10) {
                    browseEntry(.type, systemImage: "square.grid.2x2") { showingFilters = true }
                    browseEntry(.rankings, systemImage: "chart.bar.fill") { browseRating() }
                    browseEntry(.seasonal, systemImage: "calendar") { browseSeasonal() }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 8)
    }

    private var resultContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(resultHeading)
                    .font(.title2.weight(.bold))
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                if hasActiveFilters {
                    Text(activeFilterSummary)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            }
            if isLoadingResults && results.isEmpty {
                ProgressView(AnimeL10n.key(.searchLoading))
                    .frame(maxWidth: .infinity, minHeight: 180)
            } else if results.isEmpty {
                ContentUnavailableView(
                    AnimeL10n.key(.librarySearchEmpty),
                    systemImage: "magnifyingglass",
                    description: Text(AnimeL10n.key(.librarySearchEmptyDescription)),
                )
                .frame(maxWidth: .infinity, minHeight: 220)
            } else {
                LazyVGrid(columns: posterGridColumns, spacing: 20) {
                    ForEach(results) { subject in
                        NavigationLink(value: subject) {
                            NativeLibrarySubjectCard(subject: subject)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            if isLoadingResults && !results.isEmpty {
                ProgressView(AnimeL10n.key(.libraryLoadingMore))
                    .frame(maxWidth: .infinity)
            } else if let nextCursor, !isLoadingResults {
                Button(AnimeL10n.key(.actionLoadMore)) { loadMore(cursor: nextCursor) }
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.bordered)
            }
            if let errorMessage {
                NativeLibraryInlineError(message: errorMessage) {
                    if let nextCursor { loadMore(cursor: nextCursor) } else { submitSearch() }
                }
            }
        }
    }

    private var resultHeading: String {
        if !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return AnimeL10n.string(.searchResults)
        }
        return browseHeading
    }

    private var browseHeading: String {
        if let sectionID = currentSectionID, let section = section(withID: sectionID) { return section.title }
        if currentBrowseMode == .rating { return AnimeL10n.string(.libraryRanking) }
        if currentBrowseMode == .seasonal { return AnimeL10n.string(.librarySeason) }
        if currentBrowseMode == .curated { return AnimeL10n.string(.libraryCurated) }
        return AnimeL10n.string(.searchResults)
    }

    private func submitSearch() {
        guard canRunQuery else { return }
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        currentBrowseMode = nil
        currentSectionID = nil
        hasSubmittedSearch = true
        isSearchPresented = true
        runQuery(query: normalized) { snapshot, error in
            if error == nil, !normalized.isEmpty {
                saveRecentSearch(normalized)
                model.saveSearchHistory(query: normalized)
            }
        }
    }

    private func runQuery(
        query: String,
        onFinish: @escaping (NativeSearchResultsSnapshot?, String?) -> Void = { _, _ in },
    ) {
        requestGeneration += 1
        let generation = requestGeneration
        requestedCursors.removeAll()
        results = []
        nextCursor = nil
        errorMessage = nil
        isLoadingResults = true
        model.search(
            query: query,
            typesCsv: typeQueryValue,
            yearStart: Int(yearStartText),
            yearEnd: Int(yearEndText),
            airingCsv: airingQueryValue,
            sort: selectedSort,
        ) { snapshot, error in
            guard generation == requestGeneration else { return }
            if let snapshot {
                results = deduplicated(results + snapshot.items)
                nextCursor = snapshot.nextCursor
            }
            errorMessage = error
            isLoadingResults = false
            onFinish(snapshot, error)
        }
    }

    private func loadMore(cursor: String) {
        guard !isLoadingResults, cursor == nextCursor, !requestedCursors.contains(cursor) else { return }
        requestedCursors.insert(cursor)
        let generation = requestGeneration
        isLoadingResults = true
        errorMessage = nil
        model.search(
            query: query.trimmingCharacters(in: .whitespacesAndNewlines),
            cursor: cursor,
            typesCsv: typeQueryValue,
            yearStart: Int(yearStartText),
            yearEnd: Int(yearEndText),
            airingCsv: airingQueryValue,
            sort: selectedSort,
        ) { snapshot, error in
            guard generation == requestGeneration else { return }
            if let snapshot {
                results = deduplicated(results + snapshot.items)
                nextCursor = snapshot.nextCursor
            }
            errorMessage = error
            isLoadingResults = false
        }
    }

    private func browse(_ mode: NativeLibraryBrowseMode) {
        switch mode {
        case .rating: browseRating()
        case .seasonal: browseSeasonal()
        case .curated:
            if let recommendations = model.searchDiscovery?.recommendations {
                results = recommendations
                nextCursor = nil
                currentBrowseMode = .curated
                currentSectionID = nil
                hasSubmittedSearch = true
                isSearchPresented = false
            }
        case let .section(id):
            if let section = section(withID: id) { showSection(section) }
        }
    }

    private func browseRating() {
        query = ""
        selectedType = "all"
        selectedAiring = "all"
        selectedSort = "rating"
        yearStartText = ""
        yearEndText = ""
        currentBrowseMode = .rating
        currentSectionID = nil
        hasSubmittedSearch = true
        isSearchPresented = false
        runQuery(query: "")
    }

    private func browseSeasonal() {
        query = ""
        selectedType = "all"
        selectedAiring = "airing"
        selectedSort = "updated"
        let year = Calendar.current.component(.year, from: Date())
        yearStartText = String(year)
        yearEndText = String(year)
        currentBrowseMode = .seasonal
        currentSectionID = nil
        hasSubmittedSearch = true
        isSearchPresented = false
        runQuery(query: "")
    }

    private func showSection(_ section: NativeDiscoverySection) {
        query = ""
        results = section.subjects
        nextCursor = nil
        errorMessage = nil
        currentBrowseMode = .section(section.id)
        currentSectionID = section.id
        hasSubmittedSearch = true
        isSearchPresented = false
    }

    private func refreshExploreContent() async {
        model.refresh(force: true)
        await withCheckedContinuation { continuation in
            model.loadSearchDiscovery { _ in continuation.resume() }
        }
        loadExploreRails()
    }

    private func loadExploreRails() {
        exploreGeneration += 1
        let generation = exploreGeneration
        isLoadingExploreRails = true
        exploreErrorMessage = nil
        let year = Calendar.current.component(.year, from: Date())
        var completed = 0
        var firstError: String?
        let finish: (String?) -> Void = { error in
            guard generation == exploreGeneration else { return }
            completed += 1
            if firstError == nil { firstError = error }
            if completed == 2 {
                isLoadingExploreRails = false
                exploreErrorMessage = firstError
            }
        }
        model.search(query: "", sort: "rating") { snapshot, error in
            guard generation == exploreGeneration else { return }
            if let snapshot { rankingItems = deduplicated(snapshot.items) }
            finish(error)
        }
        model.search(query: "", yearStart: year, yearEnd: year, airingCsv: "airing", sort: "updated") { snapshot, error in
            guard generation == exploreGeneration else { return }
            if let snapshot { seasonalItems = deduplicated(snapshot.items) }
            finish(error)
        }
    }

    private func loadRecentSearches() {
        recentSearches = UserDefaults.standard.stringArray(forKey: Self.recentSearchesKey) ?? []
    }

    private func saveRecentSearch(_ value: String) {
        recentSearches = ([value] + recentSearches.filter { $0.caseInsensitiveCompare(value) != .orderedSame }).prefix(10).map { $0 }
        UserDefaults.standard.set(recentSearches, forKey: Self.recentSearchesKey)
    }

    private func clearRecentSearches() {
        recentSearches = []
        UserDefaults.standard.removeObject(forKey: Self.recentSearchesKey)
    }

    private func deduplicated(_ values: [NativeSubjectSummary]) -> [NativeSubjectSummary] {
        var seen = Set<Int64>()
        return values.filter { seen.insert($0.id).inserted }
    }

    private var themeSections: [NativeDiscoverySection] { model.discovery?.sections ?? [] }
    private var canRunQuery: Bool {
        !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || hasActiveFilters
    }
    private var posterGridColumns: [GridItem] {
        dynamicTypeSize.isAccessibilitySize ? [GridItem(.flexible())] : [GridItem(.flexible()), GridItem(.flexible())]
    }
    private func section(withID id: String) -> NativeDiscoverySection? { themeSections.first { $0.id == id } }
    private var typeQueryValue: String? { selectedType == "all" ? nil : selectedType }
    private var airingQueryValue: String? { selectedAiring == "all" ? nil : selectedAiring }
    private var hasActiveFilters: Bool {
        selectedType != "all" || selectedAiring != "all" || selectedSort != "relevance" || !yearStartText.isEmpty || !yearEndText.isEmpty
    }
    private var activeFilterSummary: String {
        var values: [String] = []
        if selectedType != "all" { values.append(selectedType.searchTypeName) }
        if selectedAiring != "all" { values.append(selectedAiring.searchAiringName) }
        if selectedSort != "relevance" { values.append(selectedSort.searchSortName) }
        if !yearStartText.isEmpty || !yearEndText.isEmpty { values.append(AnimeL10n.string(.filterYear)) }
        return values.joined(separator: " · ")
    }
}

private enum NativeLibraryBrowseEntryKind {
    case type, topics, rankings, seasonal

    var title: AnimeL10n.Key {
        switch self {
        case .type: return .libraryBrowseType
        case .topics: return .libraryBrowseTopics
        case .rankings: return .libraryBrowseRankings
        case .seasonal: return .libraryBrowseSeasonal
        }
    }
}

private enum NativeLibraryBrowseMode: Equatable {
    case rating, seasonal, curated, section(String)
}

private struct NativeLibraryBrowseEntryLabel: View {
    let kind: NativeLibraryBrowseEntryKind
    let systemImage: String

    var body: some View {
        Label(AnimeL10n.key(kind.title), systemImage: systemImage)
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 54, alignment: .leading)
            .padding(.horizontal, 14)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(.quaternary, lineWidth: 0.7)
            }
    }
}

private struct NativeLibraryThemeCard: View {
    let section: NativeDiscoverySection
    let reduceTransparency: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 4) {
                ForEach(section.subjects.prefix(4)) { subject in
                    NativePosterImage(url: subject.posterURL, width: 56, height: 74, cornerRadius: 8)
                }
            }
            Text(section.title)
                .font(.headline)
                .lineLimit(2)
            Text(AnimeL10n.string(.librarySectionCount, section.subjects.count))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .frame(width: 280, alignment: .leading)
        .background(
            reduceTransparency ? AnyShapeStyle(Color(uiColor: .secondarySystemGroupedBackground)) : AnyShapeStyle(.regularMaterial),
            in: RoundedRectangle(cornerRadius: 20, style: .continuous),
        )
    }
}

private struct NativeLibrarySubjectCard: View {
    let subject: NativeSubjectSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativePosterImage(url: subject.posterURL, aspectRatio: 3 / 4, cornerRadius: 14)
            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            Text([subject.year.map(String.init), subject.type.displayName].compactMap { $0 }.joined(separator: " · "))
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            if let rating = subject.rating {
                Label(AnimeL10n.string(.subjectRating, String(format: "%.1f", rating)), systemImage: "star.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.orange)
            } else {
                Text(AnimeL10n.key(.subjectNoRating))
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(subject.title)
    }
}

private struct NativeLibraryInlineError: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Text(message).font(.footnote).foregroundStyle(.secondary)
            Spacer()
            Button(AnimeL10n.key(.actionRetry)) { retry() }
                .font(.footnote.weight(.semibold))
        }
        .padding(12)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct FlowLayout<Data: RandomAccessCollection, Content: View>: View where Data.Element: Hashable {
    let items: Data
    let content: (Data.Element) -> Content

    init(items: Data, @ViewBuilder content: @escaping (Data.Element) -> Content) {
        self.items = items
        self.content = content
    }

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 96), alignment: .leading)],
            alignment: .leading,
            spacing: 8,
        ) {
            ForEach(items, id: \.self) { item in content(item) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NativeLibraryTopicListView: View {
    let sections: [NativeDiscoverySection]
    @ObservedObject var model: NativeAppModel

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                ForEach(sections) { section in
                    NavigationLink {
                        NativeLibrarySectionView(section: section, model: model)
                    } label: {
                        NativeLibraryThemeCard(section: section, reduceTransparency: false)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.libraryBrowseTopics))
        .navigationBarTitleDisplayMode(.large)
    }
}

private struct NativeLibrarySectionView: View {
    let section: NativeDiscoverySection
    @ObservedObject var model: NativeAppModel

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 20) {
                ForEach(section.subjects) { subject in
                    NavigationLink {
                        NativeSubjectDetailView(summary: subject, model: model)
                    } label: {
                        NativeLibrarySubjectCard(subject: subject)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(section.title)
        .navigationBarTitleDisplayMode(.large)
    }
}

private struct NativeSearchFiltersSheet: View {
    @Binding var selectedType: String
    @Binding var selectedAiring: String
    @Binding var selectedSort: String
    @Binding var yearStartText: String
    @Binding var yearEndText: String
    let onApply: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var validationMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section(AnimeL10n.key(.searchFilters)) {
                    Picker(AnimeL10n.key(.filterType), selection: $selectedType) {
                        Text(AnimeL10n.key(.filterAllTypes)).tag("all")
                        Text(AnimeL10n.key(.subjectTypeTv)).tag("tv")
                        Text(AnimeL10n.key(.subjectTypeWeb)).tag("web")
                        Text(AnimeL10n.key(.subjectTypeOva)).tag("ova")
                        Text(AnimeL10n.key(.filterMovie)).tag("movie")
                        Text(AnimeL10n.key(.filterOther)).tag("other")
                    }
                    Picker(AnimeL10n.key(.filterAiring), selection: $selectedAiring) {
                        Text(AnimeL10n.key(.filterAllStatuses)).tag("all")
                        Text(AnimeL10n.key(.filterAnnounced)).tag("announced")
                        Text(AnimeL10n.key(.filterCurrentlyAiring)).tag("airing")
                        Text(AnimeL10n.key(.filterFinished)).tag("finished")
                    }
                    Picker(AnimeL10n.key(.filterSort), selection: $selectedSort) {
                        Text(AnimeL10n.key(.sortRelevance)).tag("relevance")
                        Text(AnimeL10n.key(.sortRating)).tag("rating")
                        Text(AnimeL10n.key(.sortUpdated)).tag("updated")
                    }
                }

                Section(AnimeL10n.key(.filterYearRange)) {
                    TextField(AnimeL10n.key(.filterYearStart), text: $yearStartText)
                        .keyboardType(.numberPad)
                    TextField(AnimeL10n.key(.filterYearEnd), text: $yearEndText)
                        .keyboardType(.numberPad)
                    if let validationMessage {
                        Text(validationMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button(AnimeL10n.key(.clearFilters)) {
                        selectedType = "all"
                        selectedAiring = "all"
                        selectedSort = "relevance"
                        yearStartText = ""
                        yearEndText = ""
                        validationMessage = nil
                    }
                }
            }
            .navigationTitle(AnimeL10n.key(.searchFilters))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AnimeL10n.key(.actionCancel)) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(AnimeL10n.key(.applyFilters)) { apply() }
                }
            }
        }
    }

    private func apply() {
        let start = yearStartText.trimmingCharacters(in: .whitespacesAndNewlines)
        let end = yearEndText.trimmingCharacters(in: .whitespacesAndNewlines)
        let startValue = start.isEmpty ? nil : Int(start)
        let endValue = end.isEmpty ? nil : Int(end)
        guard (start.isEmpty || startValue != nil), (end.isEmpty || endValue != nil) else {
            validationMessage = AnimeL10n.string(.validationYearNumber)
            return
        }
        guard (startValue == nil || startValue! >= 1900), (endValue == nil || endValue! >= 1900) else {
            validationMessage = AnimeL10n.string(.validationYearMinimum)
            return
        }
        guard startValue == nil || endValue == nil || startValue! <= endValue! else {
            validationMessage = AnimeL10n.string(.validationYearOrder)
            return
        }
        onApply()
        dismiss()
    }
}

struct NativeCollectionView: View {
    @ObservedObject var model: NativeAppModel
    @State private var selectedStatus = "Watching"
    @State private var errorMessage: String?

    private let filters = ["Watching", "Wish", "Completed", "OnHold", "Dropped"]

    var body: some View {
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
                        LazyVGrid(columns: posterGridColumns, spacing: 18) {
                            ForEach(page.items) { item in
                                NavigationLink {
                                    NativeSubjectDetailView(summary: item.subjectSummary, model: model)
                                } label: {
                                    NativeCollectionCard(item: item)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .id(selectedStatus)
                        if let cursor = page.nextCursor {
                            Button(AnimeL10n.key(.actionLoadMore)) {
                                model.loadCollection(status: selectedStatus, cursor: cursor, append: true) { _, error in
                                    errorMessage = error
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .buttonStyle(.bordered)
                        }
                    } else if !model.isSessionReady || model.isLoadingCollection {
                        ProgressView(AnimeL10n.key(.libraryRestoring))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    } else {
                        ContentUnavailableView(AnimeL10n.key(.libraryEmpty), systemImage: "books.vertical", description: Text(AnimeL10n.key(.libraryEmptyDescription)))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 36)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .scrollIndicators(.hidden)
            .scrollEdgeEffectStyle(.soft, for: .top)
            .navigationTitle(AnimeL10n.key(.libraryTitle))
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
                if model.isSessionReady, model.session.status == "authenticated", model.collectionPage == nil {
                    loadCollection(selectedStatus)
                }
            }
            .onChange(of: model.isSessionReady) { _, ready in
                if ready, model.session.status == "authenticated", model.collectionPage == nil {
                    loadCollection(selectedStatus)
                }
            }
            .onChange(of: model.session.status) { _, status in
                if status == "authenticated", model.isSessionReady, model.collectionPage == nil {
                    loadCollection(selectedStatus)
                }
        }
    }

    private func loadCollection(_ status: String) {
        errorMessage = nil
        model.loadCollection(status: status) { _, error in
            errorMessage = error
        }
    }

    private let posterGridColumns = [GridItem(.flexible()), GridItem(.flexible())]
}

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

struct NativeAdminView: View {
    @ObservedObject var model: NativeAppModel
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    if let overview = model.adminOverview {
                        Text(AnimeL10n.key(.adminConsole)).font(.title2.weight(.bold))
                        Text(AnimeL10n.string(.adminRole, overview.role)).font(.subheadline).foregroundStyle(.secondary)
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                            NativeMetric(value: Int(overview.usersActive), title: AnimeL10n.string(.adminActiveUsers))
                            NativeMetric(value: Int(overview.usersTotal), title: AnimeL10n.string(.adminTotalUsers))
                            NativeMetric(value: Int(overview.reviewsPublished), title: AnimeL10n.string(.adminPublishedReviews))
                            NativeMetric(value: Int(overview.commentsPublished), title: AnimeL10n.string(.adminPublishedComments))
                            NativeMetric(value: Int(overview.openCommentReports), title: AnimeL10n.string(.adminOpenReports))
                        }
                        Text(AnimeL10n.key(.adminReportQueue))
                            .font(.headline)
                            .padding(.top, 8)
                        if model.adminReports.isEmpty {
                            Text(AnimeL10n.key(.adminNoReports))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        } else {
                            ForEach(model.adminReports) { report in
                                VStack(alignment: .leading, spacing: 9) {
                                    HStack {
                                        Text(report.reasonCode.reportDisplayName).font(.headline)
                                        Spacer()
                                        Text(report.status.adminReportDisplayName)
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(.secondary)
                                    }
                                    Text(AnimeL10n.string(.adminReportPeople, report.reporterName, report.authorName))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                    if let subjectTitle = report.subjectTitle {
                                        Text(subjectTitle).font(.caption).foregroundStyle(.tint)
                                    }
                                    Text(report.body).lineLimit(4)
                                    HStack(spacing: 8) {
                                        Button(AnimeL10n.key(.adminClaim)) { reportAction(report, action: "claim") }
                                            .buttonStyle(.bordered)
                                            .disabled(report.status != "open")
                                        Button(AnimeL10n.key(.adminKeep)) { reportAction(report, action: "resolve", contentAction: "published") }
                                            .buttonStyle(.bordered)
                                        Button(AnimeL10n.key(.adminHide), role: .destructive) { reportAction(report, action: "resolve", contentAction: "hidden") }
                                            .buttonStyle(.bordered)
                                        Button(AnimeL10n.key(.adminDismiss)) { reportAction(report, action: "dismiss") }
                                            .buttonStyle(.bordered)
                                    }
                                }
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                        }
                        Text(AnimeL10n.key(.adminCommentModeration))
                            .font(.headline)
                            .padding(.top, 8)
                        if model.adminComments.isEmpty {
                            Text(AnimeL10n.key(.adminNoComments))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        } else {
                            ForEach(model.adminComments) { comment in
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack {
                                        Text(comment.authorName).font(.headline)
                                        Spacer()
                                        Text(comment.moderationStatus.adminDisplayName)
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(.secondary)
                                    }
                                    if let subjectTitle = comment.subjectTitle {
                                        Text(subjectTitle)
                                            .font(.caption)
                                            .foregroundStyle(.tint)
                                    }
                                    Text(comment.body)
                                        .lineLimit(4)
                                    HStack(spacing: 8) {
                                        Button(AnimeL10n.key(.actionPublish)) { moderate(comment.id, action: "published") }
                                            .buttonStyle(.bordered)
                                            .disabled(comment.moderationStatus == "published")
                                        Button(AnimeL10n.key(.adminHide)) { moderate(comment.id, action: "hidden") }
                                            .buttonStyle(.bordered)
                                            .disabled(comment.moderationStatus == "hidden")
                                        Button(AnimeL10n.key(.actionDelete), role: .destructive) { moderate(comment.id, action: "deleted") }
                                            .buttonStyle(.bordered)
                                    }
                                }
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                        }
                        NativeActionRow(title: AnimeL10n.string(.adminAccount), subtitle: AnimeL10n.string(.adminAccountDescription), systemImage: "person.badge.key")
                    } else if let errorMessage {
                        NativeInlineError(message: errorMessage) { load() }
                    } else {
                        ProgressView(AnimeL10n.key(.adminLoading))
                            .frame(maxWidth: .infinity, minHeight: 220)
                    }
                }
                .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
            .navigationTitle(AnimeL10n.key(.adminConsole))
            .navigationBarTitleDisplayMode(.large)
            .task { load() }
            .refreshable { await refresh() }
    }

    private func load() {
        errorMessage = nil
        model.loadAdminOverview { _, error in errorMessage = error }
        model.loadAdminComments { _, error in if errorMessage == nil { errorMessage = error } }
        model.loadAdminReports { _, error in if errorMessage == nil { errorMessage = error } }
    }

    private func moderate(_ id: String, action: String) {
        model.moderateComment(id: id, action: action) { error in
            if let error { errorMessage = error }
        }
    }

    private func reportAction(_ report: NativeAdminReportSnapshot, action: String, contentAction: String? = nil) {
        model.adminReportAction(id: report.id, action: action, contentAction: contentAction) { error in
            if let error { errorMessage = AnimeL10n.string(.activityOperationFailed, error) }
        }
    }

    private func refresh() async {
        await withCheckedContinuation { continuation in
            model.loadAdminOverview { _, error in
                errorMessage = error
                model.loadAdminComments { _, commentsError in
                    if errorMessage == nil { errorMessage = commentsError }
                    model.loadAdminReports { _, reportsError in
                        if errorMessage == nil { errorMessage = reportsError }
                        continuation.resume()
                    }
                }
            }
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
                DatePicker(AnimeL10n.key(.calendarDate), selection: $selectedDate, displayedComponents: .date)
                    .datePickerStyle(.compact)
                    .padding(16)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load() }
                } else if let calendar = model.calendar, calendar.date == dateString {
                    if calendar.items.isEmpty {
                        ContentUnavailableView(AnimeL10n.key(.calendarEmpty), systemImage: "calendar.badge.clock", description: Text(AnimeL10n.key(.calendarEmptyDescription)))
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
                    ProgressView(AnimeL10n.key(.calendarLoading))
                        .frame(maxWidth: .infinity, minHeight: 180)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.calendar))
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
            NativePosterImage(url: subject.posterURL, width: 60, height: 80, cornerRadius: 12)
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
                Picker(AnimeL10n.key(.subjectMaterial), selection: $selectedSection) {
                    Text(AnimeL10n.key(.subjectEpisodes)).tag("episodes")
                    Text(AnimeL10n.key(.subjectCharacters)).tag("characters")
                    Text(AnimeL10n.key(.subjectRelations)).tag("relations")
                }
                .pickerStyle(.segmented)

                if let errorMessage {
                    NativeInlineError(message: errorMessage) { load(force: true) }
                } else if let sections = model.subjectSections[subjectId] {
                    sectionContent(sections)
                } else {
                    ProgressView(AnimeL10n.key(.subjectLoadingMaterial))
                        .frame(maxWidth: .infinity, minHeight: 220)
                }
            }
            .padding(16)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollIndicators(.hidden)
        .scrollEdgeEffectStyle(.soft, for: .top)
        .navigationTitle(AnimeL10n.key(.subjectDetails))
        .navigationBarTitleDisplayMode(.inline)
        .task { load() }
        .refreshable { await refresh() }
    }

    @ViewBuilder
    private func sectionContent(_ sections: NativeSubjectSectionsSnapshot) -> some View {
        switch selectedSection {
        case "characters":
            if sections.characters.isEmpty {
                ContentUnavailableView(AnimeL10n.key(.subjectNoCharacters), systemImage: "person.2")
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
                                Text(AnimeL10n.string(.subjectVoiceActor, character.actors.map(\.name).joined(separator: "、")))
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
                ContentUnavailableView(AnimeL10n.key(.subjectNoRelations), systemImage: "link")
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
                ContentUnavailableView(AnimeL10n.key(.subjectNoEpisodes), systemImage: "list.number")
            } else {
                ForEach(sections.episodes) { episode in
                    HStack(spacing: 12) {
                        Text(episode.number.map { String(format: "%g", $0) } ?? "—")
                            .font(.headline.monospacedDigit())
                            .frame(width: 42)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(episode.title ?? AnimeL10n.string(.subjectUnnamedEpisode))
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

struct NativeSettingsView: View {
    @ObservedObject var model: NativeAppModel
    @State private var showingAccount = false

    var body: some View {
        Form {
            Section(AnimeL10n.key(.accountSection)) {
                if model.session.status == "authenticated" {
                    LabeledContent(AnimeL10n.key(.username), value: model.session.displayName ?? AnimeL10n.string(.userFallback))
                    NavigationLink {
                        NativeAccountManagementView(model: model)
                    } label: {
                        Label(AnimeL10n.key(.accountData), systemImage: "person.crop.circle.badge.checkmark")
                    }
                    Button(AnimeL10n.key(.accountSwitch)) { showingAccount = true }
                } else {
                    Button(AnimeL10n.key(.actionLoginOrRegister)) { showingAccount = true }
                }
            }

            Section(AnimeL10n.key(.serviceSection)) {
                NavigationLink {
                    NativeDiagnosticsView(model: model)
                } label: {
                    Label(AnimeL10n.key(.diagnostics), systemImage: "waveform.path.ecg")
                }
            }

            Section(AnimeL10n.key(.appearanceSection)) {
                Picker(AnimeL10n.key(.profileTheme), selection: Binding(
                    get: { model.appearanceTheme },
                    set: { model.setAppearanceTheme($0) },
                )) {
                    Text(AnimeL10n.key(.profileSystem)).tag("system")
                    Text(AnimeL10n.key(.profileLight)).tag("light")
                    Text(AnimeL10n.key(.profileDark)).tag("dark")
                }
                Toggle(AnimeL10n.key(.profileGlass), isOn: Binding(
                    get: { model.glassEnabled },
                    set: { model.setGlassEnabled($0) },
                ))
                Toggle(AnimeL10n.key(.profileReduceMotion), isOn: Binding(
                    get: { model.reduceMotionEnabled },
                    set: { model.setReduceMotionEnabled($0) },
                ))
            }

            Section(AnimeL10n.key(.languageSection)) {
                Picker(AnimeL10n.key(.profileAppLanguage), selection: Binding(
                    get: { model.languagePreference.rawValue },
                    set: { model.setLanguage($0) },
                )) {
                    ForEach(NativeLanguagePreference.allCases) { language in
                        Text(LocalizedStringKey(language.displayName)).tag(language.rawValue)
                    }
                }
            }

            Section(AnimeL10n.key(.aboutSection)) {
                LabeledContent(AnimeL10n.key(.version), value: appVersion)
                LabeledContent(AnimeL10n.key(.interfaceValue), value: AnimeL10n.string(.interfaceValue))
            }
        }
        .navigationTitle(AnimeL10n.key(.settings))
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showingAccount) {
            NativeAccountSheet(model: model)
                .presentationDetents([.medium, .large])
        }
    }

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? AnimeL10n.string(.genericUnknown)
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
    @State private var selectedAvatar: PhotosPickerItem?
    @State private var isUploadingAvatar = false

    var body: some View {
        Form {
                Section(AnimeL10n.key(.profileTitle)) {
                HStack(spacing: 12) {
                    NativeAvatar(url: model.profile?.avatarURL ?? model.session.avatarURL, name: model.profile?.displayName ?? model.session.displayName)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(AnimeL10n.key(.profilePicture)).font(.headline)
                        Text(AnimeL10n.key(.profilePictureFormats))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    PhotosPicker(selection: $selectedAvatar, matching: .images) {
                        Image(systemName: "camera.fill")
                    }
                    .buttonStyle(.bordered)
                    .disabled(isUploadingAvatar)
                }
                TextField(AnimeL10n.key(.displayName), text: $displayName)
                Button(AnimeL10n.key(.profileSaveDisplayName)) {
                    model.updateProfile(displayName: displayName) { error in
                        message = error.map { AnimeL10n.string(.profileSaveFailed, $0) } ?? AnimeL10n.string(.profileSaved)
                        if error == nil { model.loadProfile() }
                    }
                }
                .disabled(displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section(AnimeL10n.key(.passwordChange)) {
                SecureField(AnimeL10n.key(.passwordCurrent), text: $currentPassword)
                SecureField(AnimeL10n.key(.passwordNew), text: $newPassword)
                Button(AnimeL10n.key(.passwordChange)) {
                    model.changePassword(currentPassword: currentPassword, newPassword: newPassword) { error in
                        message = error.map { AnimeL10n.string(.passwordChangeFailed, $0) } ?? AnimeL10n.string(.passwordChanged)
                        if error == nil { currentPassword = ""; newPassword = "" }
                    }
                }
                .disabled(currentPassword.isEmpty || newPassword.count < 8)
            }

            Section(AnimeL10n.key(.accountData)) {
                NavigationLink {
                    NativeSyncView(model: model)
                } label: {
                    Label(AnimeL10n.key(.bangumiSync), systemImage: "arrow.triangle.2.circlepath")
                }
                Button(AnimeL10n.key(.exportData)) {
                    model.exportMyData { data, error in
                        if let data { exportText = AnimeL10n.string(.exportSuccess, String(data.utf8.count)) }
                        else { exportText = AnimeL10n.string(.exportFailed, error ?? AnimeL10n.string(.genericUnknown)) }
                    }
                }
            }

            Section {
                Button(AnimeL10n.key(.actionDeleteAccount), role: .destructive) { showingDeleteConfirmation = true }
            } footer: {
                Text(AnimeL10n.key(.accountDeleteWarning))
            }

            if let message { Section { Text(message).foregroundStyle(message.contains(AnimeL10n.string(.errorFailureMarker)) ? .red : .secondary) } }
            if let exportText { Section(AnimeL10n.key(.exportResult)) { Text(exportText).font(.footnote) } }
        }
        .navigationTitle(AnimeL10n.key(.accountData))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            displayName = model.profile?.displayName ?? model.session.displayName ?? ""
            if model.profile == nil { model.loadProfile() }
        }
        .onChange(of: selectedAvatar) { _, item in
            guard let item else { return }
            Task {
                do {
                    guard let data = try await item.loadTransferable(type: Data.self) else {
                        message = AnimeL10n.string(.avatarReadFailed)
                        return
                    }
                    isUploadingAvatar = true
                    let jpegData = UIImage(data: data)?.jpegData(compressionQuality: 0.86) ?? data
                    model.uploadAvatar(base64: jpegData.base64EncodedString(), contentType: "image/jpeg") { error in
                        isUploadingAvatar = false
                        message = error.map { AnimeL10n.string(.avatarUploadFailed, $0) } ?? AnimeL10n.string(.avatarUpdated)
                        if error == nil { selectedAvatar = nil }
                    }
                } catch {
                    message = AnimeL10n.string(.avatarReadFailed, error.localizedDescription)
                }
            }
        }
        .confirmationDialog(AnimeL10n.key(.accountDeleteConfirmation), isPresented: $showingDeleteConfirmation, titleVisibility: .visible) {
            Button(AnimeL10n.key(.accountDeleteForever), role: .destructive) {
                model.deleteAccount { error in message = error.map { AnimeL10n.string(.accountDeleteFailed, $0) } ?? AnimeL10n.string(.accountDeleted) }
            }
        }
    }
}

struct NativeSyncView: View {
    @ObservedObject var model: NativeAppModel
    @State private var message: String?

    var body: some View {
        List {
            Section(AnimeL10n.key(.syncSection)) {
                if let status = model.syncStatus {
                    LabeledContent(AnimeL10n.key(.syncPending), value: String(status.pendingCount))
                    LabeledContent(AnimeL10n.key(.syncFailed), value: String(status.failedCount))
                    LabeledContent(AnimeL10n.key(.syncConflicts), value: String(status.conflictCount))
                    LabeledContent("Bangumi", value: status.bangumiLinked ? AnimeL10n.string(.syncConnected) : AnimeL10n.string(.syncDisconnected))
                    if let last = status.lastSuccessfulAt {
                        LabeledContent(AnimeL10n.key(.syncLastSuccess), value: last.prefix(16).description)
                    }
                } else {
                    ProgressView(AnimeL10n.key(.syncLoading))
                }
                Button(AnimeL10n.key(.syncNow)) {
                    model.startSync { error in
                        message = error.map { AnimeL10n.string(.syncRequestFailed, $0) } ?? AnimeL10n.string(.syncRequestSubmitted)
                        model.loadSyncStatus()
                        model.loadSyncConflicts()
                    }
                }
                if let message { Text(message).font(.footnote).foregroundStyle(.secondary) }
            }

            Section(AnimeL10n.key(.syncConflictSection)) {
                if model.syncConflicts.isEmpty {
                    Text(AnimeL10n.key(.syncNoConflicts))
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(model.syncConflicts) { conflict in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(AnimeL10n.string(.syncConflictTitle, String(conflict.subjectId), conflict.fieldName)).font(.headline)
                            Text(AnimeL10n.string(.syncLocal, conflict.localValue)).font(.caption).foregroundStyle(.secondary)
                            Text(AnimeL10n.string(.syncRemote, conflict.remoteValue)).font(.caption).foregroundStyle(.secondary)
                            HStack {
                                Button(AnimeL10n.key(.syncKeepLocal)) { resolve(conflict, choice: "keep_local") }
                                Button(AnimeL10n.key(.syncUseRemote)) { resolve(conflict, choice: "use_remote") }
                                Button(AnimeL10n.key(.syncLater)) { resolve(conflict, choice: "later") }
                            }
                            .font(.caption.weight(.semibold))
                        }
                        .padding(.vertical, 5)
                    }
                }
            }
        }
        .scrollIndicators(.hidden)
        .navigationTitle(AnimeL10n.key(.bangumiSync))
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
            message = error.map { AnimeL10n.string(.syncResolveFailed, $0) } ?? AnimeL10n.string(.syncResolved)
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
                Button {
                    model.loadDiagnostics()
                } label: {
                    if model.isLoadingDiagnostics {
                        HStack {
                            ProgressView()
                            Text(AnimeL10n.key(.diagnosticsTesting))
                        }
                    } else {
                        Text(AnimeL10n.key(.diagnosticsRetest))
                    }
                }
                .disabled(model.isLoadingDiagnostics)
            }
            Section(AnimeL10n.key(.serviceSection)) {
                ForEach(model.diagnostics) { item in
                    HStack {
                        Image(systemName: item.healthy ? "checkmark.circle.fill" : "xmark.octagon.fill")
                            .foregroundStyle(item.healthy ? .green : .red)
                        VStack(alignment: .leading) {
                            Text(item.endpoint).font(.headline)
                            Text(diagnosticStatusText(item))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .scrollIndicators(.hidden)
        .navigationTitle(AnimeL10n.key(.diagnostics))
        .task { if model.diagnostics.isEmpty { model.loadDiagnostics() } }
    }

    private func diagnosticStatusText(_ item: NativeDiagnosticSnapshot) -> String {
        let latency = item.latencyMs.map { "\($0) ms" } ?? AnimeL10n.string(.diagnosticsNoResponse)
        if item.healthy {
            return AnimeL10n.string(.diagnosticsHTTP, latency, item.statusCode.map(String.init) ?? "—")
        }
        if let error = item.errorMessage, !error.isEmpty {
            return "\(latency) · \(error)"
        }
        return AnimeL10n.string(.diagnosticsHTTP, latency, item.statusCode.map(String.init) ?? "—")
    }
}

private struct NativeReviewComposer: View {
    let subjectId: Int64
    @ObservedObject var model: NativeAppModel
    let onComplete: (NativeReviewSnapshot?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var reviewBody = ""
    @State private var spoiler = false
    @State private var visibility = "public"
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section(AnimeL10n.key(.formFeeling)) {
                    TextEditor(text: $reviewBody)
                        .frame(minHeight: 160)
                    Toggle(AnimeL10n.key(.formSpoiler), isOn: $spoiler)
                    Picker(AnimeL10n.key(.visibility), selection: $visibility) {
                        Text(AnimeL10n.key(.visibilityPublic)).tag("public")
                        Text(AnimeL10n.key(.visibilityPrivate)).tag("private")
                    }
                }
                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                    }
                }
                Section {
                    Button(isSubmitting ? AnimeL10n.key(.formPosting) : AnimeL10n.key(.actionPublish)) {
                        submit()
                    }
                        .disabled(isSubmitting || reviewBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .navigationTitle(AnimeL10n.key(.writeReview))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AnimeL10n.key(.actionCancel)) { dismiss() }
                }
            }
        }
    }

    private func submit() {
        let normalizedBody = reviewBody.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedBody.isEmpty else { return }
        guard !isSubmitting else { return }
        isSubmitting = true
        errorMessage = nil
        model.createReviewAdvanced(
            subjectId: subjectId,
            kind: "short",
            title: nil,
            body: normalizedBody,
            spoiler: spoiler,
            visibility: visibility,
        ) { review, error in
            isSubmitting = false
            if let review {
                onComplete(review, nil)
                dismiss()
            } else {
                errorMessage = error ?? AnimeL10n.string(.reviewPublishFailed)
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
                Section(AnimeL10n.key(.bangumiAccount)) {
                    TextField(AnimeL10n.key(.accountUsername), text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField(AnimeL10n.key(.accountPassword), text: $password)
                    if isRegistering { TextField(AnimeL10n.key(.accountDisplayName), text: $displayName) }
                }
                Section {
                    Button(isLoading ? AnimeL10n.key(.accountSubmitting) : (isRegistering ? AnimeL10n.key(.accountRegisterLogin) : AnimeL10n.key(.actionLogin))) {
                        submit()
                    }
                    .disabled(isLoading || username.isEmpty || password.isEmpty || (isRegistering && displayName.isEmpty))
                    Button(isRegistering ? AnimeL10n.key(.accountExistingLogin) : AnimeL10n.key(.accountNoAccountRegister)) {
                        isRegistering.toggle()
                    }
                }
                Section {
                    Button(AnimeL10n.key(.accountBangumiLogin)) {
                        model.beginBangumiLogin { errorMessage = $0 }
                    }
                }
                if let errorMessage {
                    Section { Text(errorMessage).foregroundStyle(.red) }
                }
            }
            .navigationTitle(AnimeL10n.key(.account))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(AnimeL10n.key(.accountClose)) { dismiss() } }
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
            NativePosterImage(url: subject.posterURL, aspectRatio: 3 / 4, cornerRadius: 16)
            Text(subject.title)
                .font(.headline)
                .lineLimit(2)
            HStack(spacing: 5) {
                Image(systemName: "star.fill")
                    .foregroundStyle(.orange)
            Text(subject.rating.map { String(format: "%.1f", $0) } ?? AnimeL10n.string(.subjectNoRating))
            }
            .font(.caption.weight(.medium))
            .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NativeCollectionCard: View {
    let item: NativeCollectionItemSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            NativePosterImage(url: item.posterURL, aspectRatio: 3 / 4, cornerRadius: 16)
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
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NativeRatingRow: View {
    let rating: NativeProfileRatingSnapshot

    var body: some View {
        HStack(spacing: 12) {
            NativePosterImage(url: rating.posterURL, width: 56, height: 75, cornerRadius: 10)
            VStack(alignment: .leading, spacing: 6) {
                Text(rating.title).font(.headline).lineLimit(2)
                Label("\(rating.score) / 10", systemImage: "star.fill")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.orange)
                if !rating.tags.isEmpty {
                    Text(rating.tags.joined(separator: " · "))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
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

private struct NativeActivityCard: View {
    let item: NativeActivityItemSnapshot
    let destination: AnyView?
    let onDelete: (() -> Void)?

    var body: some View {
        Group {
            if let destination {
                NavigationLink { destination } label: { content(subject: item.subjectSummary) }
            } else {
                content(subject: item.subjectSummary)
            }
        }
        .buttonStyle(.plain)
        .contextMenu {
            if let onDelete {
                Button(AnimeL10n.key(.activityWithdraw), role: .destructive, action: onDelete)
            }
        }
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
            Label(AnimeL10n.key(.statusLoadingFailed), systemImage: "exclamationmark.triangle")
                .font(.headline)
            Text(message).font(.subheadline).foregroundStyle(.secondary)
            Button(AnimeL10n.key(.actionRetry), action: retry).buttonStyle(.borderedProminent)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

struct NativePosterImage: View {
    let url: URL?
    let width: CGFloat?
    let height: CGFloat?
    let aspectRatio: CGFloat?
    let cornerRadius: CGFloat

    init(
        url: URL?,
        width: CGFloat? = nil,
        height: CGFloat? = nil,
        aspectRatio: CGFloat? = nil,
        cornerRadius: CGFloat = 14,
    ) {
        self.url = url
        self.width = width
        self.height = height
        self.aspectRatio = aspectRatio
        self.cornerRadius = cornerRadius
    }

    var body: some View {
        posterImage
            .frame(width: width, height: height)
            .aspectRatio(width == nil && height == nil ? (aspectRatio ?? 3 / 4) : nil, contentMode: .fill)
            .frame(maxWidth: width == nil && height == nil ? .infinity : nil)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .contentShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    private var posterImage: some View {
        NativePosterLoader(url: url)
    }
}

private struct NativePosterLoader: View {
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
        .clipped()
    }
}

struct NativeRemoteImage: View {
    let url: URL?
    var contentMode: ContentMode = .fit

    var body: some View {
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image): image.resizable().aspectRatio(contentMode: contentMode)
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
        let actor = actorName ?? AnimeL10n.string(.someone)
        switch kind {
        case "review_liked": return AnimeL10n.string(.notificationReviewLiked, actor)
        case "comment_liked": return AnimeL10n.string(.notificationCommentLiked, actor)
        case "comment_replied": return AnimeL10n.string(.notificationCommentReplied, actor)
        case "followed": return AnimeL10n.string(.notificationFollowed, actor)
        default: return AnimeL10n.string(.notificationDefault, actor)
        }
    }
}

private extension NativeSessionSnapshot {
    var avatarURL: URL? { avatarUrl.flatMap(URL.init(string:)) }
}

private extension String {
    var collectionDisplayName: String {
        switch self {
        case "Wish", "wish": return AnimeL10n.string(.statusWish)
        case "Watching", "watching": return AnimeL10n.string(.statusWatching)
        case "Completed", "completed": return AnimeL10n.string(.statusCompleted)
        case "OnHold", "on_hold", "onhold": return AnimeL10n.string(.statusOnHold)
        case "Dropped", "dropped": return AnimeL10n.string(.statusDropped)
        default: return self
        }
    }

    var feedDisplayName: String {
        switch self {
        case "following": return AnimeL10n.string(.feedFollowing)
        case "popular": return AnimeL10n.string(.feedPopular)
        case "public": return AnimeL10n.string(.feedPublic)
        default: return self
        }
    }

    var adminDisplayName: String {
        switch self {
        case "published": return AnimeL10n.string(.moderationPublished)
        case "hidden": return AnimeL10n.string(.moderationHidden)
        case "deleted": return AnimeL10n.string(.moderationDeleted)
        default: return self
        }
    }

    var reportDisplayName: String {
        switch self {
        case "spam": return AnimeL10n.string(.reportSpam)
        case "harassment": return AnimeL10n.string(.reportHarassment)
        case "spoiler": return AnimeL10n.string(.reportSpoiler)
        case "illegal": return AnimeL10n.string(.reportIllegal)
        case "other": return AnimeL10n.string(.reportOther)
        default: return self
        }
    }

    var adminReportDisplayName: String {
        switch self {
        case "open": return AnimeL10n.string(.reportOpen)
        case "claimed": return AnimeL10n.string(.reportClaimed)
        case "resolved": return AnimeL10n.string(.reportResolved)
        case "dismissed": return AnimeL10n.string(.reportDismissed)
        default: return self
        }
    }

    var searchTypeName: String {
        switch self {
        case "tv": return "TV"
        case "web": return "Web"
        case "ova": return "OVA"
        case "movie": return AnimeL10n.string(.subjectTypeMovie)
        case "other": return AnimeL10n.string(.subjectTypeOther)
        default: return self
        }
    }

    var searchAiringName: String {
        switch self {
        case "announced": return AnimeL10n.string(.subjectAiringAnnounced)
        case "airing": return AnimeL10n.string(.subjectAiring)
        case "finished": return AnimeL10n.string(.subjectFinished)
        default: return self
        }
    }

    var searchSortName: String {
        switch self {
        case "rating": return AnimeL10n.string(.searchSortRating)
        case "updated": return AnimeL10n.string(.searchSortUpdated)
        default: return AnimeL10n.string(.searchSortRelevance)
        }
    }
}
